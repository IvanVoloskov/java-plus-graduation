package ewm;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

public record ParamDto(
        @NotNull(message = "Дата и время начала диапазона за который нужно выгрузить статистику обязательны")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime start,

        @NotNull(message = "Дата и время конца диапазона за который нужно выгрузить статистику обязательны")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime end,

        List<String> uris,

        Boolean unique) {
}
