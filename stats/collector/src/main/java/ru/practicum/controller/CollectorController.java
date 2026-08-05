package ru.practicum.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import ru.practicum.client.CollectorClient;
import ru.practicum.dto.Topics;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.message.collector.controller.UserActionControllerGrpc;
import ru.practicum.grpc.message.collector.user.UserActionProto;
import ru.practicum.mapper.CollectorMapper;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class CollectorController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final CollectorClient client;
    private final CollectorMapper mapper;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.info("gRPC: Получено действие пользователя ID={} для события ID={}", request.getUserId(), request.getEventId());

        try {
            UserActionAvro userAvro = mapper.mapToAvroFromProto(request);

            ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                    Topics.STATS_USER_ACTIONS_V1,
                    userAvro
            );

            client.getProducer().send(record);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: Критический сбой при обработке лога: {}", e.getMessage());
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)));
        }
    }
}
