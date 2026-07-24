package ewm.comments.mapper;

import ewm.comments.dto.CommentDto;
import ewm.comments.dto.PostCommentParam;
import ewm.comments.model.Comment;
import ewm.event.mapper.EventMapper;
import ewm.event.service.EventService;
import ewm.user.mapper.UserMapper;
import ewm.user.service.UserService;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {EventService.class, UserService.class, EventMapper.class, UserMapper.class})
public interface CommentMapper {

    CommentDto toCommentDto(Comment comment);

    Comment postToComment(PostCommentParam postCommentParam);

    List<CommentDto> toFullDtoList(List<Comment> comments);
}