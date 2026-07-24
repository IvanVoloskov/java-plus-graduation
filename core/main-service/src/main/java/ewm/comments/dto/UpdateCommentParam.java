package ewm.comments.dto;

public record UpdateCommentParam(
        Long author,
        Long commentId,
        String comment
) {
}
