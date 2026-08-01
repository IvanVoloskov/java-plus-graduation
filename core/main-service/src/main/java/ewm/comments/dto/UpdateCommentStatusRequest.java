package ewm.comments.dto;

import ewm.comments.model.CommentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCommentStatusRequest(
        @NotNull
        CommentStatus status
) {
}