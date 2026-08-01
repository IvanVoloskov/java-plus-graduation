package ewm.compilation.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateCompilationDto(
        List<Long> events,

        @Size(min = 1, max = 50, message = "Длина заголовка не должна превышать 50 символов")
        String title,

        Boolean pinned
) {
}
