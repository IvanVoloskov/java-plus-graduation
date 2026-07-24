package ewm.controller;

import ewm.HitDto;
import ewm.ParamDto;
import ewm.StatsDto;
import ewm.service.StatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void createHit(@Valid @RequestBody HitDto hitDto) {
        statsService.createHit(hitDto);
    }

    @GetMapping("/stats")
    public List<StatsDto> getStats(@Valid @ModelAttribute ParamDto paramDto) {
        return statsService.getStats(paramDto);
    }
}
