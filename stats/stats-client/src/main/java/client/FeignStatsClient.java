package client;

import ewm.HitDto;
import ewm.StatsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "stats-server", fallbackFactory = StatClientFallbackFactory.class)
interface FeignStatsClient {
    @PostMapping("/hit")
    void hit(@RequestBody HitDto hitDto);

    @GetMapping("/stats")
    List<StatsDto> get(@RequestParam("start") String start,
                       @RequestParam("end") String end,
                       @RequestParam(value = "uris", required = false) List<String> uris,
                       @RequestParam(value = "unique", defaultValue = "false") Boolean unique);
}
