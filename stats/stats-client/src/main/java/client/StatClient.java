package client;

import ewm.HitDto;
import ewm.ParamDto;
import ewm.StatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StatClient {
    private final FeignStatsClient feignClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void hit(HitDto hitDto) {
        feignClient.hit(hitDto);
    }

    public List<StatsDto> get(ParamDto paramDto) {
        String startStr = paramDto.start().format(formatter);
        String endStr = paramDto.end().format(formatter);

        List<String> uris = (paramDto.uris() == null) ? List.of() : paramDto.uris();
        Boolean unique = (paramDto.unique() == null) ? false : paramDto.unique();

        return feignClient.get(startStr, endStr, uris, unique);
    }
}
