package ewm.controller;

import ewm.HitDto;
import ewm.ParamDto;
import ewm.StatsDto;
import ewm.service.StatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void createHit(@Valid @RequestBody HitDto hitDto) {
        log.info("POST /hit: hitDto={}", hitDto);
        statsService.createHit(hitDto);
    }

    @GetMapping("/stats")
    public List<StatsDto> getStats(@Valid @ModelAttribute ParamDto paramDto) {
        log.info("GET /stats: paramDto={}", paramDto);
        return statsService.getStats(paramDto);
    }
}
