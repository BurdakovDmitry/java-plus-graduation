package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.service.AnalyzerStarter;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AnalyzerApplication {
    public static void main(String[] args) {
        // Запуск Spring Boot приложения при помощи вспомогательного класса SpringApplication
        // метод run возвращает назад настроенный контекст, который мы можем использовать для
        // получения настроенных бинов
        ConfigurableApplicationContext context = SpringApplication.run(AnalyzerApplication.class, args);

        // Получаем бин AnalyzerStarter из контекста и запускаем основную логику сервиса
        AnalyzerStarter analyzer = context.getBean(AnalyzerStarter.class);
        analyzer.start();
    }
}
