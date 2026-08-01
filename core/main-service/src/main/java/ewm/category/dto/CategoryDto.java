package ewm.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryDto(
        Long id,

        @NotBlank(message = "Название категории не может быть пустым")
        @Size(min = 1, max = 50, message = "Имя категории не должно превышать 50 символом")
        String name
) {}