package ewm.client;

import ewm.client.dto.EventPreviewDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/events/previews")
    List<EventPreviewDto> getEventPreviews(@RequestParam("ids") List<Long> ids);
}