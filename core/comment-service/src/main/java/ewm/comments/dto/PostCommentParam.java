package ewm.comments.dto;

public record PostCommentParam(
        Long author,
        Long event,
        String comment
) {
}
