package ru.practicum.client;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.deserialization.EventSimilarityDeserializer;
import ru.practicum.deserialization.UserActionDeserializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Properties;
import java.util.UUID;

@Slf4j
@Configuration
public class AnalyzerClientConfiguration {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public AnalyzerClient getClient() {
        return new AnalyzerClient() {
            private Consumer<String, UserActionAvro> consumerUser;
            private Consumer<String, EventSimilarityAvro> consumerEvent;

            @Override
            public Consumer<String, UserActionAvro> getConsumerUser() {
                if (consumerUser == null) {
                    initConsumerUser();
                }
                return consumerUser;
            }

            private void initConsumerUser() {
                log.info("Инициализация Kafka Consumer для UserAction");

                Properties config = new Properties();
                config.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-group-" + UUID.randomUUID());
                config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class);

                consumerUser = new KafkaConsumer<>(config);

                log.info("Инициализация Kafka Consumer для UserAction успешно завершена");
            }

            @Override
            public Consumer<String, EventSimilarityAvro> getConsumerEvent() {
                if (consumerEvent == null) {
                    initConsumerEvent();
                }
                return consumerEvent;
            }

            private void initConsumerEvent() {
                log.info("Инициализация Kafka Consumer для EventSimilarity");

                Properties config = new Properties();
                config.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-group-" + UUID.randomUUID());
                config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventSimilarityDeserializer.class);

                consumerEvent = new KafkaConsumer<>(config);

                log.info("Инициализация Kafka Consumer для EventSimilarity успешно завершена");
            }

            @Override
            @PreDestroy
            public void stop() {
                if (consumerUser != null) {
                    log.info("Закрытие Kafka Consumer соединения для UserAction");
                    consumerUser.close();
                }

                if (consumerEvent != null) {
                    log.info("Закрытие Kafka Consumer соединения для EventSimilarity");
                    consumerEvent.close();
                }
            }
        };
    }
}
