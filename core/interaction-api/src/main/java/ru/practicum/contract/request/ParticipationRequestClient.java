package ru.practicum.contract.request;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.request.ConfirmedRequestCount;
import ru.practicum.dto.request.ParticipationStatus;

import java.util.List;

@FeignClient(name = "request-service", fallbackFactory = ParticipationRequestClientFallbackFactory.class)
public interface ParticipationRequestClient {

    @GetMapping("/contract/request/confirmed")
    List<ConfirmedRequestCount> getConfirmedRequestCount(@RequestParam("eventIds") List<Long> eventIds);

    @GetMapping("/contract/request/count")
    Long getRequestCount(@RequestParam("eventId") Long eventId,
                         @RequestParam("status") ParticipationStatus status);

    @GetMapping("/contract/request/attendance")
    boolean checkUserAttendance(@RequestParam("userId") Long userId,
                                @RequestParam("eventId") Long eventId);
}
