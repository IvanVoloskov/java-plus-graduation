package ewm.comments.mapper;

import ewm.comments.dto.CommentDto;
import ewm.comments.dto.PostCommentParam;
import ewm.comments.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "event", ignore = true)
    @Mapping(target = "author", ignore = true)
    CommentDto toCommentDto(Comment comment);

    @Mapping(target = "eventId", source = "event")
    @Mapping(target = "authorId", source = "author")
    Comment postToComment(PostCommentParam postCommentParam);

    List<CommentDto> toFullDtoList(List<Comment> comments);
}