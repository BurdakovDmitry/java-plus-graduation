package ru.practicum.mapper;

import ru.practicum.dto.CommentDto;
import ru.practicum.dto.PostCommentParam;
import ru.practicum.dto.event.EventPreviewDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;

@Mapper()
public interface CommentMapper {

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "author", source = "userDto")
    @Mapping(target = "event", source = "eventDto")
    CommentDto toCommentDto(Comment comment, UserShortDto userDto, EventPreviewDto eventDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "editedOn", ignore = true)
    @Mapping(target = "authorId", source = "author")
    @Mapping(target = "eventId", source = "event")
    Comment postToComment(PostCommentParam postCommentParam);

    default List<CommentDto> toFullDtoList(List<Comment> comments,
                                           Map<Long, UserShortDto> usersMap,
                                           Map<Long, EventPreviewDto> eventsMap) {
        if (comments == null) return List.of();

        return comments.stream()
                .map(comment -> {
                    // Ищем автора комментария в нашей мапе, полученной по сети пачкой
                    UserShortDto userDto = usersMap.get(comment.getAuthorId());
                    // Ищем событие с этим комментарием в нашей мапе, полученной по сети пачкой
                    EventPreviewDto eventDto = eventsMap.get(comment.getEventId());
                    // Маппим одиночный комментарий, склеивая его с автором и событием
                    return toCommentDto(comment, userDto, eventDto);
                })
                .toList();
    }
}