package ewm.exception;

import org.springframework.http.HttpStatus;

public record ErrorResponse(HttpStatus status, String description, String error, String stackTrace) {
}
