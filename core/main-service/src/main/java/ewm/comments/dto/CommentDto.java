package ewm.comments.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ewm.comments.model.CommentStatus;
import ewm.event.dto.EventPreviewDto;
import ewm.user.dto.UserShortDto;

import java.time.LocalDateTime;

public record CommentDto(
        Long id,

        String comment,

        CommentStatus status,

        EventPreviewDto event,

        UserShortDto author,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdOn,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime editedOn
) {
}
