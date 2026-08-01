package ewm.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // ← null поля не будут отображаться
public class ApiError {
    private List<String> errors;    // может быть null или пустым
    private String message;
    private String reason;
    private String status;
    private String timestamp;
}