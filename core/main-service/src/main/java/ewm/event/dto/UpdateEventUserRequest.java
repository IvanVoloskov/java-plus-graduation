package ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ewm.event.model.Location;
import ewm.event.model.StateAction;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateEventUserRequest(
        @Size(min = 20, max = 2000)
        String annotation,

        Long category,

        @Size(min = 20, max = 7000)
        String description,

        @Future
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime eventDate,

        Location location,
        Boolean paid,

        @PositiveOrZero
        Integer participantLimit,

        Boolean requestModeration,

        @Size(min = 3, max = 120)
        String title,

        StateAction stateAction
) {}