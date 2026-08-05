package ru.practicum.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.dto.collector.ActionType;
import ru.practicum.grpc.message.analyzer.controller.RecommendationsControllerGrpc;
import ru.practicum.grpc.message.analyzer.event.InteractionsCountRequestProto;
import ru.practicum.grpc.message.analyzer.event.RecommendedEventProto;
import ru.practicum.grpc.message.analyzer.event.SimilarEventsRequestProto;
import ru.practicum.grpc.message.analyzer.event.UserPredictionsRequestProto;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.UserAction;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserActionRepository;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AnalyzerController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final UserActionRepository actionRepository;
    private final EventSimilarityRepository similarityRepository;

    /**
     * Возвращает поток рекомендованных мероприятий для указанного пользователя
     *
     * @param request с данными для рекомендаций:
     *                'user_id' - Идентификатор пользователя, для которого вычисляются рекомендации
     *                'max_results' - Ограничение количества мероприятий в результате выполнения запроса
     * @param responseObserver поток сообщений RecommendedEventProto с предсказанной оценкой
     */
    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        int userId = request.getUserId();
        int maxResults = request.getMaxResults();
        log.info("gRPC: Запрос рекомендаций для пользователя ID={}, max={}", userId, maxResults);

        try {
            // Получаем всю историю для фильтрации
            List<UserAction> allUserActions = actionRepository.findAllByUserIdOrderByUpdatedAtDesc(userId);

            // Если истории нет — возвращаем пустой список
            if (allUserActions.isEmpty()) {
                responseObserver.onCompleted();
                return;
            }

            // Создаем список ID всех просмотренных мероприятий
            List<Integer> fullInteractedIds = allUserActions.stream()
                    .map(UserAction::getEventId)
                    .toList();

            // Выделяем лимитированный список ID недавно просмотренных мероприятий
            List<Integer> recentInteractedEvents = allUserActions.stream()
                    .map(UserAction::getEventId)
                    .limit(maxResults)
                    .toList();

            // Получаем все отсортированные пары подобия для всей пачки мероприятий
            List<EventSimilarity> similarEvents = similarityRepository
                    .findByEventAInOrEventBInOrderByScoreDesc(recentInteractedEvents, recentInteractedEvents);

            // Формируем список кандидатов для дальнейшего вычисления оценок
            List<Integer> candidateEventIds = new ArrayList<>();

            for (EventSimilarity event : similarEvents) {
                // Определяем, какой из ID в паре является похожим новым мероприятием
                int similarId = recentInteractedEvents.contains(event.getEventA()) ? event.getEventB() : event.getEventA();

                // Проверяем, что пользователь раньше не взаимодействовал с этим мероприятием
                if (!fullInteractedIds.contains(similarId)) {
                    candidateEventIds.add(similarId);
                }
            }

            // Получаем все сходства для всех кандидатов
            List<EventSimilarity> allSimsForCandidates = similarityRepository
                    .findByEventAInOrEventBInOrderByScoreDesc(candidateEventIds, candidateEventIds);

            // Распределяем полученные сходства по кандидатам в Map<CandidateId, List<EventSimilarity>>
            Map<Integer, List<EventSimilarity>> simsByCandidateMap = new ConcurrentHashMap<>();

            for (Integer candidateId : candidateEventIds) {
                simsByCandidateMap.put(candidateId, new ArrayList<>());
            }

            for (EventSimilarity sim : allSimsForCandidates) {
                if (candidateEventIds.contains(sim.getEventA())) {
                    simsByCandidateMap.get(sim.getEventA()).add(sim);
                }
                if (candidateEventIds.contains(sim.getEventB())) {
                    simsByCandidateMap.get(sim.getEventB()).add(sim);
                }
            }

            // Преобразуем всю историю пользователя в Map
            Map<Integer, UserAction> userActionsMap = allUserActions.stream()
                    .collect(Collectors.toMap(UserAction::getEventId, action -> action));

            // Переходим к вычислению прогнозируемой оценки для каждого нового мероприятия-кандидата
            List<RecommendedEventProto> recommendationsList = new ArrayList<>();

            // Перебираем каждое мероприятие-кандидат для расчета его прогнозируемой оценки
            for (Integer newEventId : candidateEventIds) {
                // Получаем отсортированные пары подобия, где участвует текущий newEventId.
                List<EventSimilarity> simsWithHistory = simsByCandidateMap.getOrDefault(newEventId, List.of());

                // Оставляем только те пары, с которыми пользователь уже взаимодействовал
                List<Map.Entry<UserAction, Double>> neighbors = new ArrayList<>();

                for (EventSimilarity sim : simsWithHistory) {
                    int historyEventId = sim.getEventA().equals(newEventId) ? sim.getEventB() : sim.getEventA();

                    // Проверяем, есть ли historyEventId в истории действий пользователя.
                    UserAction userAction = userActionsMap.get(historyEventId);
                    if (userAction != null) {
                        // Если нашли — сохраняем пару
                        neighbors.add(new AbstractMap.SimpleEntry<>(userAction, sim.getScore()));
                    }
                }

                // Отбираем максимальное количество похожих соседей
                List<Map.Entry<UserAction, Double>> limitNeighbors = neighbors.stream()
                        .limit(maxResults)
                        .toList();

                if (limitNeighbors.isEmpty()) continue;

                double sumWeightedScores = 0.0; // Сумма взвешенных оценок (оценка * коэффициент подобия)
                double sumSimilarities = 0.0;   // Сумма коэффициентов подобия

                //Вычисляем компоненты для формулы взвешенного среднего
                for (Map.Entry<UserAction, Double> neighbor : limitNeighbors) {
                    // Переводим тип действия пользователя в числовой вес
                    double weight = getActionWeight(neighbor.getKey().getActionType());
                    double scoreSim = neighbor.getValue(); // Коэффициент подобия пары

                    sumWeightedScores += (weight * scoreSim);
                    sumSimilarities += scoreSim;
                }

                // Вычисляем итоговую прогнозируемую оценку с защитой деления на ноль
                double finalPredictedScore = (sumSimilarities > 0.0) ? (sumWeightedScores / sumSimilarities) : 0.0;

                recommendationsList.add(RecommendedEventProto.newBuilder()
                        .setEventId(newEventId)
                        .setScore(finalPredictedScore)
                        .build());
            }

            // Сортируем по убыванию score и отдаем maxResults клиенту
            recommendationsList.stream()
                    .sorted((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()))
                    .limit(maxResults)
                    .forEach(responseObserver::onNext); // Передача элементов потоком данных (stream)

            // Уведомляем gRPC-клиента об успешном завершении передачи всего потока данных
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка при передаче данных в getRecommendationsForUser: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    /**
     * Возвращает поток мероприятий, с которыми не взаимодействовал этот пользователь,
     * но которые максимально похожи на указанное мероприятие
     *
     * @param request с параметрами:
     *                'eventId' - идентификатор мероприятия, для которого нужно найти похожие мероприятия.
     *                'userId' - идентификатор пользователя, чтобы исключить мероприятия, с которыми он уже взаимодействовал
     *                'max_results' - ограничение количества мероприятий в результате выполнения запроса
     * @param responseObserver поток сообщений RecommendedEventProto с коэффициентом подобия
     */
    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        int eventId = request.getEventId();
        int userId = request.getUserId();
        int maxResults = request.getMaxResults();
        log.info("gRPC: Запрос похожих событий для eventId={}, userId={}, max={}", eventId, userId, maxResults);

        try {
            // Получаем отсортированные пары подобия, в которых хотя бы одно мероприятие соответствует указанному eventId
            List<EventSimilarity> similarities = similarityRepository.findAllSimilarToEventOrderByScoreDesc(eventId);

            // Получаем список ID всех мероприятий, с которыми данный пользователь уже взаимодействовал
            List<Integer> interactedEventIds = actionRepository.findInteractedEventIdsByUserId(userId);

            // Преобразуем список пар подобия в итоговые gRPC-сообщения, фильтруя и ограничивая их.
            similarities.stream()
                    // Извлекаем противоположный ID из пары (похожее новое мероприятие)
                    .map(sim -> {
                        int similarEventId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();

                        return RecommendedEventProto.newBuilder()
                                .setEventId(similarEventId)
                                .setScore(sim.getScore())
                                .build();
                    })
                    // Убираем мероприятия, с которыми пользователь уже взаимодействовал
                    .filter(proto -> !interactedEventIds.contains(proto.getEventId()))
                    .limit(maxResults)
                    .forEach(responseObserver::onNext);

            // Уведомляем gRPC-клиента об успешном завершении передачи всего потока данных
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка при передаче данных в getSimilarEvents: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    /**
     * Возвращает их поток с суммой максимальных весов действий каждого пользователя с этими мероприятиями
     *
     * @param request список идентификаторов мероприятий, для которых нужно вернуть сумму всех взаимодействий
     * @param responseObserver поток сообщений RecommendedEventProto с количеством взаимодействий
     */
    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<Integer> eventIds = request.getEventIdList();
            List<Object[]> results = actionRepository.sumActionWeightsByEventIds(eventIds);

            // Превращаем результат в Map<EventId, TotalWeight>
            Map<Integer, Double> weightsMap = results.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Double) row[1]
                    ));

            for (Integer eventId : eventIds) {
                double scoreValue = weightsMap.getOrDefault(eventId, 0.0);

                RecommendedEventProto recommendation = RecommendedEventProto.newBuilder()
                        .setEventId(eventId)
                        .setScore(scoreValue)
                        .build();

                responseObserver.onNext(recommendation);
            }

            // Уведомляем gRPC-клиента об успешном завершении передачи всего потока данных
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка при передаче данных в getInteractionsCount: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    /**
     * Метод для перевода ActionType в числовые веса
     */
    private double getActionWeight(ActionType type) {
        return switch (type) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}
