package ru.practicum.deserialization;

import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public class EventSimilarityDeserializer extends BaseAvroDeserializer<EventSimilarityAvro> implements Deserializer<EventSimilarityAvro> {
    public EventSimilarityDeserializer() { super(EventSimilarityAvro.getClassSchema()); }
}
