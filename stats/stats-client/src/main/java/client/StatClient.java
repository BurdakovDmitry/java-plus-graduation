package client;

import ewm.HitDto;
import ewm.ParamDto;
import ewm.StatsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class StatClient {
    final RestClient restClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatClient(@Value("${stats.server.url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public void hit(HitDto hitDto) {
        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri("/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hitDto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.info("Failed to connect to stat service");
        }
    }

    public List<StatsDto> get(ParamDto paramDto) {
        List<StatsDto> stats;

        Optional<List<String>> uris = (paramDto.uris() == null || paramDto.uris().isEmpty())
                ? Optional.empty()
                : Optional.of(paramDto.uris());

        try {
            stats = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stats")
                            .queryParam("start", paramDto.start().format(formatter))
                            .queryParam("end", paramDto.end().format(formatter))
                            .queryParamIfPresent("uris", uris)
                            .queryParamIfPresent("unique", Optional.ofNullable(paramDto.unique()))
                            .build())
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (Exception e) {
            log.info("Failed to connect to stat service");
            stats = List.of(new StatsDto("ewm-main-service", "/fake-uri", 0L));
        }
        return stats;
    }
}
