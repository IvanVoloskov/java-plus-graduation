package ewm.event.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

public record PublicEventParamDto(
        String text,
        List<Long> category,
        Boolean paid,

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime rangeStart,

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime rangeEnd,

        Boolean onlyAvailable,
        String sort,

        @PositiveOrZero
        Integer from,

        @Positive
        Integer size) {

    public PublicEventParamDto {
        if (onlyAvailable == null) onlyAvailable = false;
        if (from == null) from = 0;
        if (size == null) size = 10;
    }
}
