package ru.practicum.client;

import org.apache.kafka.clients.consumer.Consumer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface AnalyzerClient {
    Consumer<String, UserActionAvro> getConsumerUser();

    Consumer<String, EventSimilarityAvro> getConsumerEvent();

    void stop();
}
