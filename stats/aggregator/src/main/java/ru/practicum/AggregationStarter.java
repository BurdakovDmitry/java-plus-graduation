package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.client.AggregatorClient;
import ru.practicum.dto.Topics;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {
    private final AggregatorClient client;

    // Хранилище весов действий пользователей
    Map<Integer, Map<Integer, Double>> eventUserWeights = new ConcurrentHashMap<>();

    // Сумма весов для каждого мероприятия
    Map<Integer, Double> eventWeightsSum = new ConcurrentHashMap<>();

    // Сумма минимальных весов для каждой пары мероприятий
    Map<Integer, Map<Integer, Double>> minWeightsSums = new ConcurrentHashMap<>();

    public void start() {
        Consumer<String, UserActionAvro> consumer = client.getConsumer();
        Producer<String, SpecificRecordBase> producer = client.getProducer();

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            log.info("Aggregator: подписываемся на топик: {}", Topics.STATS_USER_ACTIONS_V1);
            consumer.subscribe(List.of(Topics.STATS_USER_ACTIONS_V1));

            // Цикл чтения данных из Kafka и отправки результата сходства мероприятий в Kafka
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    UserActionAvro userAvro = record.value();

                    log.info("Aggregator: получено событие из Kafka: User={}, Event={}, Type={}",
                            userAvro.getUserId(), userAvro.getEventId(), userAvro.getActionType());

                    // Получение списка пересчитанных сходств мероприятий
                    List<EventSimilarityAvro> similarities = calculateEventSimilarity(userAvro);

                    //Цикл для отправки пересчитанных сходств в Kafka
                    for (EventSimilarityAvro similarity : similarities) {
                        ProducerRecord<String, SpecificRecordBase> producerRecord = new ProducerRecord<>(
                                Topics.STATS_EVENTS_SIMILARITY_V1,
                                similarity.getEventA() + "_" + similarity.getEventB(), // Ключ для сходства пары мероприятий
                                similarity
                        );

                        producer.send(producerRecord);
                        log.info("Aggregator: Отправлено сходство: {}-{}, score={}",
                                similarity.getEventA(), similarity.getEventB(), similarity.getScore());
                    }
                }

                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                log.info("Сброса данных в буфере у продюсера");
                producer.flush();
                log.info("Фиксация смещений консьюмера");
                consumer.commitSync();
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }

    /**
     * Расчет косинусного сходства мероприятий с использованием весов действий
     * Использует 'ConcurrentHashMap<>()' для обеспечения высокой производительности и потокобезопасноти
     *
     * @param userAvro с данными действия пользователя, полученные из Kafka
     * @return список EventSimilarityAvro с результатом расчета сходства мероприятий для отправки в Kafka
     */
    private List<EventSimilarityAvro> calculateEventSimilarity(UserActionAvro userAvro) {
        int userId = userAvro.getUserId(); // Идентификатор пользователя
        int eventA = userAvro.getEventId(); // Идентификатор события

        // Инициализация объекта для заполнения данных результатами расчета
        List<EventSimilarityAvro> results = new ArrayList<>();

        // Веса действий
        double actionWeight = switch (userAvro.getActionType()) {
            case VIEW -> 0.4; // Просмотр мероприятия
            case REGISTER -> 0.8; // Регистрация на мероприятие
            case LIKE -> 1.0; // Лайк мероприятию
        };

        // Извлекаем или создаем новую мапу пользователей для текущего мероприятия
        eventUserWeights.putIfAbsent(eventA, new ConcurrentHashMap<>());
        Map<Integer, Double> userWeightsForEventA = eventUserWeights.get(eventA);

        // Извлекаем старое значение максимального веса
        double oldWeight = userWeightsForEventA.getOrDefault(userId, 0.0);

        // Если максимальный вес не изменился, пересчитывать сходство не требуется
        if (actionWeight <= oldWeight) {
            return results;
        }

        // Обновляем максимальный вес для этого пользователя и мероприятия
        userWeightsForEventA.put(userId, actionWeight);

        // Пересчитываем общую сумму весов мероприятия на дельту изменения веса
        double deltaWeight = actionWeight - oldWeight;
        eventWeightsSum.merge(eventA, deltaWeight, Double::sum);

        // Пересчитываем сходство мероприятия A с остальными мероприятиями, где был этот пользователь
        for (Map.Entry<Integer, Map<Integer, Double>> entry : eventUserWeights.entrySet()) {
            int eventB = entry.getKey();

            if (eventA == eventB) {
                continue; // Не считаем сходство мероприятия с самим собой
            }

            Map<Integer, Double> userWeightsForEventB = entry.getValue();

            // Если пользователь взаимодействовал и с мероприятием B
            if (userWeightsForEventB.containsKey(userId)) {
                double weightInB = userWeightsForEventB.get(userId);

                // Сравним старый вклад пользователя в общую сумму и новый для пары (A, B)
                double oldMinContribution = Math.min(oldWeight, weightInB);
                double newMinContribution = Math.min(actionWeight, weightInB);
                double deltaSMin = newMinContribution - oldMinContribution;

                // Упорядочивание пар (eventA < eventB)
                int first = Math.min(eventA, eventB);
                int second = Math.max(eventA, eventB);

                // Обновляем сумму минимальных весов (A, B)
                minWeightsSums.putIfAbsent(first, new ConcurrentHashMap<>());
                minWeightsSums.get(first).merge(second, deltaSMin, Double::sum);

                // Считаем новое косинусное сходство мероприятий
                double sMin = minWeightsSums.get(first).get(second);
                double sumA = eventWeightsSum.getOrDefault(eventA, 0.0);
                double sumB = eventWeightsSum.getOrDefault(eventB, 0.0);

                double score = 0.0;
                if (sumA > 0 && sumB > 0) {
                    score = sMin / (Math.sqrt(sumA) * Math.sqrt(sumB)); // Косинусное сходство мероприятий
                }

                results.add(EventSimilarityAvro.newBuilder()
                        .setEventA(first)
                        .setEventB(second)
                        .setScore(score)
                        .setTimestamp(userAvro.getTimestamp())
                        .build());
            }
        }

        return results;
    }
}
