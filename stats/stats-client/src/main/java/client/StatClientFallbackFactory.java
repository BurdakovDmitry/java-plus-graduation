package client;

import ewm.HitDto;
import ewm.StatsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class StatClientFallbackFactory implements FallbackFactory<FeignStatsClient> {

    @Override
    public FeignStatsClient create(Throwable cause) {
        log.error("Сбой при вызове сервиса stats. Причина: {}", cause.getMessage());

        return new FeignStatsClient() {
            @Override
            public void hit(HitDto hitDto) {
                log.error("Fallback для hit: сервис временно недоступен");
            }

            @Override
            public List<StatsDto> get(String start, String end, List<String> uris, Boolean unique) {
                log.error("Fallback для get: сервис временно недоступен");
                return List.of();
            }
        };
    }
}
