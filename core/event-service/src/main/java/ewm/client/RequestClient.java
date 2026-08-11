package ewm.client;

import ewm.client.dto.ConfirmedRequestCount;
import ewm.client.dto.ParticipationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "request-service",
        fallback = RequestClientFallback.class,
        configuration = RequestClientConfig.class)
public interface RequestClient {

    @GetMapping("/requests/confirmed")
    List<ConfirmedRequestCount> getConfirmedCounts(@RequestParam("eventIds") List<Long> eventIds);

    @GetMapping("/users/{userId}/requests")
    List<ParticipationRequestDto> getUserRequests(@PathVariable("userId") Long userId);
}