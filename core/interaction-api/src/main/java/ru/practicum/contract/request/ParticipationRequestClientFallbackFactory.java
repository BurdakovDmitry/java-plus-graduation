package ru.practicum.contract.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.dto.request.ConfirmedRequestCount;
import ru.practicum.dto.request.ParticipationStatus;

import java.util.List;

@Slf4j
@Component
public class ParticipationRequestClientFallbackFactory implements FallbackFactory<ParticipationRequestClient> {
    @Override
    public ParticipationRequestClient create(Throwable cause) {
        log.error("Сбой при вызове сервиса request-service. Причина: {}", cause.getMessage());

        return new ParticipationRequestClient() {
            @Override
            public List<ConfirmedRequestCount> getConfirmedRequestCount(List<Long> eventIds) {
                log.error("Fallback для getConfirmedRequestCount: сервис временно недоступен");
                return List.of();
            }

            @Override
            public Long getRequestCount(Long eventId, ParticipationStatus status) {
                log.error("Fallback для getRequestCount: сервис временно недоступен");
                return 0L;
            }

            @Override
            public boolean checkUserAttendance(Long userId, Long eventId) {
                log.error("Fallback для checkUserAttendance: сервис временно недоступен");
                return false;
            }
        };
    }
}
