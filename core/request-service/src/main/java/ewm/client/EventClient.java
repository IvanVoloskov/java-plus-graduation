package ewm.client;

import ewm.client.dto.EventInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/events/{eventId}/info")
    EventInfoDto getEventInfo(@PathVariable Long eventId);
}