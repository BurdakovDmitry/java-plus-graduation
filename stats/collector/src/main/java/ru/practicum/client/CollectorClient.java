package ru.practicum.client;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;

public interface CollectorClient {
    Producer<String, SpecificRecordBase> getProducer();

    void stop();
}
