package ewm.event.dto;

import ewm.event.model.EventState;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;


public record AdminEventSearchFilter(
        List<Long> users,
        List<EventState> states,
        List<Long> categories,

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime rangeStart,

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime rangeEnd,

        @Min(value = 0) Integer from,
        @Min(value = 1) Integer size
) {
    public AdminEventSearchFilter {
        if (from == null) from = 0;
        if (size == null) size = 10;
    }
}