package client;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.dto.collector.ActionType;
import ru.practicum.grpc.message.analyzer.controller.RecommendationsControllerGrpc;
import ru.practicum.grpc.message.analyzer.event.InteractionsCountRequestProto;
import ru.practicum.grpc.message.analyzer.event.RecommendedEventProto;
import ru.practicum.grpc.message.analyzer.event.SimilarEventsRequestProto;
import ru.practicum.grpc.message.analyzer.event.UserPredictionsRequestProto;
import ru.practicum.grpc.message.collector.controller.UserActionControllerGrpc;
import ru.practicum.grpc.message.collector.user.ActionTypeProto;
import ru.practicum.grpc.message.collector.user.UserActionProto;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorClient;

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerClient;

    /**
     * Отправляет действие пользователя по gRPC в сервис Collector.
     */
    public void sendUserActionToCollector(long userId, long eventId, ActionType actionType) {
        log.info("gRPC-Client: Отправка действия пользователя ID={} для события ID={}", userId, eventId);

        try {
            // Конвертируем строковый тип действия в Protobuf ENUM
            ActionTypeProto protoEnum = switch (actionType.toString()) {
                case "VIEW" -> ActionTypeProto.ACTION_VIEW;
                case "REGISTER" -> ActionTypeProto.ACTION_REGISTER;
                case "LIKE" -> ActionTypeProto.ACTION_LIKE;
                default -> throw new IllegalArgumentException("Неизвестный тип действия: " + actionType);
            };

            // Сборка Timestamp из текущего Instant
            Instant now = Instant.now();
            Timestamp timestamp = Timestamp.newBuilder()
                    .setSeconds(now.getEpochSecond())
                    .setNanos(now.getNano())
                    .build();

            // Сборка Protobuf сообщения
            UserActionProto request = UserActionProto.newBuilder()
                    .setUserId(Math.toIntExact(userId)) //  Выбросит исключение ArithmeticException, если long > Integer.MAX_VALUE
                    .setEventId(Math.toIntExact(eventId))
                    .setActionType(protoEnum)
                    .setTimestamp(timestamp)
                    .build();

            // Вызов удаленного gRPC метода
            Empty response = collectorClient.collectUserAction(request);
            log.info("gRPC-Client: Действие успешно передано в Collector");

        } catch (StatusRuntimeException e) {
            log.error("gRPC-Client: Ошибка отправки лога в Collector: {} (Код: {})",
                    e.getStatus().getDescription(), e.getStatus().getCode());
        } catch (Exception e) {
            log.error("gRPC-Client: Сбой при подготовке сообщения для Collector: {}", e.getMessage());
        }
    }

    /**
     * Возвращает поток персональных рекомендаций для указанного пользователя.
     *
     * @param userId Идентификатор пользователя, для которого вычисляются рекомендации
     * @param maxResults Ограничение количества мероприятий в результате выполнения запроса
     * @return поток сообщений RecommendedEventProto с рекомендациями
     */
    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        log.info("gRPC-Client: Запрос персональных рекомендаций для userId={}, max={}", userId, maxResults);

        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(Math.toIntExact(userId))
                .setMaxResults(maxResults)
                .build();

        Iterator<RecommendedEventProto> iterator = analyzerClient.getRecommendationsForUser(request);
        return asStream(iterator);
    }

    /**
     * Возвращает поток мероприятий, с которыми не взаимодействовал этот пользователь,
     * но которые максимально похожи на указанное мероприятие.
     *
     * @param eventId идентификатор мероприятия, для которого нужно найти похожие мероприятия.
     * @param userId идентификатор пользователя, чтобы исключить мероприятия, с которыми он уже взаимодействовал
     * @param maxResults ограничение количества мероприятий в результате выполнения запроса
     * @return поток сообщений RecommendedEventProto с похожими мероприятиями
     */
    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        log.info("gRPC-Client: Запрос похожих событий для eventId={}, userId={}, max={}", eventId, userId, maxResults);

        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(Math.toIntExact(eventId))
                .setUserId(Math.toIntExact(userId))
                .setMaxResults(maxResults)
                .build();

        Iterator<RecommendedEventProto> iterator = analyzerClient.getSimilarEvents(request);
        return asStream(iterator);
    }

    /**
     * Возвращает поток рейтинга мероприятий для переданного списка ID.
     *
     * @param eventIds список идентификаторов мероприятий, для которых нужно вернуть рейтинг
     * @return поток сообщений RecommendedEventProto с рейтингом мероприятий
     */
    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        log.info("gRPC-Client: Запрос рейтинга для списка из {} мероприятий", eventIds.size());

        List<Integer> protoEventIds = eventIds.stream()
                .map(Math::toIntExact)
                .toList();

        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(protoEventIds)
                .build();

        Iterator<RecommendedEventProto> iterator = analyzerClient.getInteractionsCount(request);
        return asStream(iterator);
    }

    /**
     * Метод для преобразования Iterator в Stream
     */
    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}
