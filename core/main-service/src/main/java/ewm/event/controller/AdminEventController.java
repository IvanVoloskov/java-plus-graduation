package ewm.event.controller;

import ewm.event.dto.AdminEventSearchFilter;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.UpdateEventAdminRequest;
import ewm.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/events")
public class AdminEventController {
    private final EventService adminEventService;

    @GetMapping
    public List<EventFullDto> getEvents(@Valid AdminEventSearchFilter filter) {
        log.info("GET /admin/events: filter={}", filter);
        return adminEventService.searchEventsAdmin(filter);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable Long eventId,
                                    @Valid @RequestBody UpdateEventAdminRequest updateRequest) {
        log.info("PATCH /admin/events/{}: {}", eventId, updateRequest);
        return adminEventService.updateEventAdmin(eventId, updateRequest);
    }
}