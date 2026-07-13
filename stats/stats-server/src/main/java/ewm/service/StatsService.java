package ewm.service;

import ewm.HitDto;
import ewm.ParamDto;
import ewm.StatsDto;

import java.util.List;

public interface StatsService {
    void createHit(HitDto hitDto);

    List<StatsDto> getStats(ParamDto paramDto);
}
