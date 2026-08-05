package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.message.collector.user.UserActionProto;

import java.time.Instant;

@Component
public class CollectorMapper {
    public UserActionAvro mapToAvroFromProto(UserActionProto request) {
        ActionTypeAvro avroEnum = switch (request.getActionType()) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Неизвестный тип действия: " + request.getActionType());
        };

        Instant instant = Instant.ofEpochSecond(
                request.getTimestamp().getSeconds(),
                request.getTimestamp().getNanos()
        );

        return UserActionAvro.newBuilder()
                .setUserId(request.getUserId())
                .setEventId(request.getEventId())
                .setActionType(avroEnum)
                .setTimestamp(instant)
                .build();
    }
}
