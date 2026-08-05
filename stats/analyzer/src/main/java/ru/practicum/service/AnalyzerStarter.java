package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.client.AnalyzerClient;
import ru.practicum.dto.Topics;
import ru.practicum.dto.collector.ActionType;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.UserAction;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserActionRepository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerStarter {
    private final AnalyzerClient client;
    private final UserActionRepository actionRepository;
    private final EventSimilarityRepository similarityRepository;

    // Выделяем пул из 2 потоков
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    /**
     * Метод запускает параллельные фоновые потоки консьюмеров Kafka
     */
    public void start() {
        log.info("Analyzer: Запуск фонового пула консьюмеров Kafka.");
        executorService.submit(this::runUserActionsConsumer);
        executorService.submit(this::runEventSimilarityConsumer);
    }

    /**
     * ПОТОК 1: Читает историю взаимодействий
     */
    private void runUserActionsConsumer() {
        Consumer<String, UserActionAvro> consumer = client.getConsumerUser();

        // Регистрируем ShutdownHook для безопасной остановки блокирующего метода poll
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            log.info("Analyzer: подписываемся на топик: {}", Topics.STATS_USER_ACTIONS_V1);
            consumer.subscribe(List.of(Topics.STATS_USER_ACTIONS_V1));

            // Цикл чтения данных из Kafka для обновления или сохранения действий пользователя в БД
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    UserActionAvro userAvro = record.value();

                    log.info("Analyzer: получено событие из Kafka: User={}, Event={}, Type={}",
                            userAvro.getUserId(), userAvro.getEventId(), userAvro.getActionType());

                    // Переводим Avro-Enum в ActionType-Enum
                    ActionType actionType = ActionType.valueOf(userAvro.getActionType().name());

                    // Определяем веса действий
                    double incomingWeight = switch (actionType) {
                        case VIEW -> 0.4;
                        case REGISTER -> 0.8;
                        case LIKE -> 1.0;
                    };

                    // Проверяем, совершал ли пользователь действия с этим мероприятием ранее
                    Optional<UserAction> existingUserActionOpt = actionRepository
                            .findByUserIdAndEventId(userAvro.getUserId(), userAvro.getEventId());

                    if (existingUserActionOpt.isPresent()) {
                        UserAction userAction = existingUserActionOpt.get();
                        double existingWeight = switch (userAction.getActionType()) {
                            case VIEW -> 0.4;
                            case REGISTER -> 0.8;
                            case LIKE -> 1.0;
                        };

                        // Если новый вес больше старого — обновляем тип действия и время
                        if (incomingWeight > existingWeight) {
                            userAction.setActionType(actionType);
                            userAction.setUpdatedAt(userAvro.getTimestamp());

                            actionRepository.save(userAction);
                            log.info("Analyzer: действие пользователя {} обновлено на новый тип взаимодействия: {}",
                                    userAvro.getUserId(), actionType);
                        }
                    } else {
                        // Если это первое взаимодействие пользователя с мероприятием — создаем новую запись
                        UserAction newUserAction = UserAction.builder()
                                .userId(userAvro.getUserId())
                                .eventId(userAvro.getEventId())
                                .actionType(actionType)
                                .updatedAt(userAvro.getTimestamp())
                                .build();

                        actionRepository.save(newUserAction);
                        log.info("Analyzer: сохранено новое действие пользователя {} с событием {}",
                                userAvro.getUserId(), userAvro.getEventId());
                    }
                }

                // Фиксируем прочитанные смещения в Kafka после успешной обработки пачки сообщений
                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                log.info("Фиксация смещений консьюмера");
                consumer.commitSync();
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
            }
        }
    }

    /**
     * ПОТОК 2: Обновляет таблицу коэффициентов подобия
     */
    private void runEventSimilarityConsumer() {
        Consumer<String, EventSimilarityAvro> consumer = client.getConsumerEvent();

        // Регистрируем ShutdownHook для безопасной остановки блокирующего метода poll
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            log.info("Analyzer: подписываемся на топик: {}", Topics.STATS_EVENTS_SIMILARITY_V1);
            consumer.subscribe(List.of(Topics.STATS_EVENTS_SIMILARITY_V1));

            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, EventSimilarityAvro> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                    EventSimilarityAvro similarityAvro = record.value();

                    log.info("Analyzer: получена матрица подобия ({}-{}), score={}",
                            similarityAvro.getEventA(), similarityAvro.getEventB(), similarityAvro.getScore());

                    // Формируем JPA-сущность на основе полученной Avro-модели
                    EventSimilarity similarity = EventSimilarity.builder()
                            .eventA(similarityAvro.getEventA())
                            .eventB(similarityAvro.getEventB())
                            .score(similarityAvro.getScore())
                            .updatedAt(similarityAvro.getTimestamp())
                            .build();

                    // Благодаря составному ключу, обновление в БД автоматическое, без дублирования данных
                    EventSimilarity saveSimilarity = similarityRepository.save(similarity);
                    log.info("Analyzer: матрица подобия ({}-{}) со значением score={} обновлена в БД",
                            saveSimilarity.getEventA(), saveSimilarity.getEventB(), saveSimilarity.getScore());
                }

                // Фиксируем прочитанные смещения в Kafka после успешной обработки пачки сообщений
                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                log.info("Фиксация смещений консьюмера");
                consumer.commitSync();
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
            }
        }
    }
}
