package ewm.event.controller;

import ewm.event.dto.EventInfoDto;
import ewm.event.dto.EventPreviewDto;
import ewm.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InternalEventController {

    private final EventService eventService;

    @GetMapping("/events/{eventId}/info")
    public EventInfoDto getEventInfo(@PathVariable Long eventId) {
        return eventService.getEventInfo(eventId);
    }

    @GetMapping("/events/previews")
    public List<EventPreviewDto> getEventPreviews(@RequestParam List<Long> ids) {
        return eventService.getEventPreviews(ids);
    }
}