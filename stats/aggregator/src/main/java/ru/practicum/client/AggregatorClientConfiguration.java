package ru.practicum.client;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.deserialization.UserActionDeserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.serializer.AggregatorAvroSerializer;

import java.util.Properties;
import java.util.UUID;

@Slf4j
@Configuration
public class AggregatorClientConfiguration {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public AggregatorClient getClient() {
        return new AggregatorClient() {
            private Producer<String, SpecificRecordBase> producer;
            private Consumer<String, UserActionAvro> consumer;

            @Override
            public Producer<String, SpecificRecordBase> getProducer() {
                if (producer == null) {
                    initProducer();
                }
                return producer;
            }

            private void initProducer() {
                log.info("Инициализация Kafka Producer");

                Properties config = new Properties();
                config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AggregatorAvroSerializer.class);

                producer = new KafkaProducer<>(config);

                log.info("Инициализация Kafka Producer успешно завершена");
            }

            @Override
            public Consumer<String, UserActionAvro> getConsumer() {
                if (consumer == null) {
                    initConsumer();
                }
                return consumer;
            }

            private void initConsumer() {
                log.info("Инициализация Kafka Consumer");

                Properties config = new Properties();
                config.put(ConsumerConfig.GROUP_ID_CONFIG, "aggregator-group-" + UUID.randomUUID());
                config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class);

                consumer = new KafkaConsumer<>(config);

                log.info("Инициализация Kafka Consumer успешно завершена");
            }

            @Override
            @PreDestroy
            public void stop() {
                if (producer != null) {
                    log.info("Закрытие Kafka Producer соединения");
                    producer.close();
                }

                if (consumer != null) {
                    log.info("Закрытие Kafka Consumer соединения");
                    consumer.close();
                }
            }
        };
    }
}
