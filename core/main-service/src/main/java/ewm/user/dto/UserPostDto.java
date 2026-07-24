package ewm.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPostDto(
        @NotBlank
        @Email
        @Size(min = 6, max = 254, message = "Длина email не должна превышать 254 символа")
        String email,

        @NotBlank
        @Size(min = 2, max = 250, message = "Имя пользователя не должно превышать 250 символов")
        String name) {
}
