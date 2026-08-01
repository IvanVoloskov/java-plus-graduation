package ewm.user.controller;

import ewm.comments.dto.CommentDto;
import ewm.comments.dto.PostCommentDto;
import ewm.comments.dto.PostCommentParam;
import ewm.comments.dto.UpdateCommentParam;
import ewm.comments.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
public class PrivateCommentController {
    private final CommentService commentService;

    @GetMapping("/comments")
    public List<CommentDto> findAllByUser(@PathVariable Long userId) {
        return commentService.findAllByAuthor(userId);
    }

    @GetMapping("/comments/{commentId}")
    public CommentDto findByIdAndAuthor(@PathVariable Long userId, @PathVariable Long commentId) {
        return commentService.findByIdAndAuthor(userId, commentId);
    }

    @GetMapping("/events/{eventId}/comments")
    public List<CommentDto> findAllByEventAndAuthor(@PathVariable Long userId, @PathVariable Long eventId) {
        return commentService.findAllByEventAndAuthor(userId, eventId);
    }

    @PostMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto create(@PathVariable Long userId,
                             @PathVariable Long eventId,
                             @Valid @RequestBody PostCommentDto postCommentDto) {
        PostCommentParam postCommentParam = new PostCommentParam(userId, eventId, postCommentDto.comment());
        return commentService.create(postCommentParam);
    }

    @PatchMapping("/comments/{commentId}")
    public CommentDto update(@PathVariable Long userId,
                             @PathVariable Long commentId,
                             @Valid @RequestBody PostCommentDto postCommentDto) {
        UpdateCommentParam updCommentParam = new UpdateCommentParam(userId, commentId, postCommentDto.comment());
        return commentService.update(updCommentParam);

    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId, @PathVariable Long commentId) {
        commentService.delete(userId, commentId);
    }
}
