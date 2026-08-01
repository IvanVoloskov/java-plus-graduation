package ewm.request.controller;

import ewm.request.model.ConfirmedRequestCount;
import ewm.request.service.ParticipationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class InternalRequestController {

    private final ParticipationRequestService requestService;

    @GetMapping("/requests/confirmed")
    public List<ConfirmedRequestCount> getConfirmedCounts(@RequestParam("eventIds") List<Long> eventIds) {
        return requestService.getConfirmedCounts(eventIds);
    }
}