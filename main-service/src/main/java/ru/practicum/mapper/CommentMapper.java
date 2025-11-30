package ru.practicum.mapper;

import ru.practicum.dto.comment.CommentDto;
import ru.practicum.model.comment.CommentStatus;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.model.comment.Comment;

public class CommentMapper {

    public static Comment toEntity(NewCommentDto dto) {
        return Comment.builder()
                .text(dto.getText())
                .status(CommentStatus.PENDING)
                .build();
    }

    public static CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(UserMapper.toShortDto(comment.getAuthor()))
                .eventId(comment.getEvent().getId())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .status(comment.getStatus())
                .build();
    }
}