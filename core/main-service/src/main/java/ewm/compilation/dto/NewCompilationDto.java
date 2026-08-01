package ewm.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NewCompilationDto(
        List<Long> events,

        @NotBlank
        @Size(min = 1, max = 50, message = "Длина заголовка не должна превышать 50 символов")
        String title,

        Boolean pinned
) {
}
