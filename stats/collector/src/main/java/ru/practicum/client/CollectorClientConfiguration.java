package ru.practicum.client;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.serializer.CollectorAvroSerializer;

import java.util.Properties;

@Slf4j
@Configuration
public class CollectorClientConfiguration {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public CollectorClient getClient() {
        return new CollectorClient() {
            private Producer<String, SpecificRecordBase> producer;

            @Override
            public Producer<String, SpecificRecordBase> getProducer() {
                if (producer == null) {
                    initProducer();
                }

                return producer;
            }

            @Override
            @PreDestroy
            public void stop() {
                if (producer != null) {
                    log.info("Закрытие Kafka Producer соединения");
                    producer.close();
                }
            }

            private void initProducer() {
                log.info("Инициализация Kafka Producer");

                Properties config = new Properties();
                config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CollectorAvroSerializer.class);

                producer = new KafkaProducer<>(config);

                log.info("Инициализация Kafka Producer успешно завершена");
            }
        };
    }
}
