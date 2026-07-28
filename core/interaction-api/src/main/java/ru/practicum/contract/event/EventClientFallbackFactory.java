package ru.practicum.contract.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventContractDto;
import ru.practicum.dto.event.EventPreviewDto;
import ru.practicum.exception.ServiceUnavailableException;

import java.util.List;

@Slf4j
@Component
public class EventClientFallbackFactory implements FallbackFactory<EventClient> {
    @Override
    public EventClient create(Throwable cause) {
        log.error("Сбой при вызове сервиса event-service. Причина: {}", cause.getMessage());

        return new EventClient() {
            @Override
            public boolean isCategory(Long categoryId) {
                log.error("Fallback для isCategory: сервис временно недоступен");
                return false;
            }

            @Override
            public EventContractDto getEventByIdContract(Long eventId) {
                log.error("Fallback для getEventByIdContract: сервис временно недоступен");
                throw new ServiceUnavailableException("Сервер управления событиями временно недоступен.");
            }

            @Override
            public EventPreviewDto findByIdPreview(Long eventId) {
                log.error("Fallback для findByIdPreview: сервис временно недоступен");
                throw new ServiceUnavailableException("Сервер управления событиями временно недоступен.");
            }

            @Override
            public List<EventPreviewDto> getEventPreviewByIds(List<Long> ids) {
                log.error("Fallback для getEventPreviewByIds: сервис временно недоступен");
                return List.of();
            }
        };
    }
}
