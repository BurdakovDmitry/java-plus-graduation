package ru.practicum.deserialization;

import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

public class UserActionDeserializer extends BaseAvroDeserializer<UserActionAvro> implements Deserializer<UserActionAvro> {
    public UserActionDeserializer() {
        super(UserActionAvro.getClassSchema());
    }
}
