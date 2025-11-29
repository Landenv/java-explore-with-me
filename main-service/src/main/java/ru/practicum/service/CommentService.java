package ru.practicum.service;

import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UpdateCommentRequest;

import java.util.List;

public interface CommentService {
    List<CommentDto> getEventComments(Long eventId, int from, int size);

    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    List<CommentDto> getUserComments(Long userId, int from, int size);

    CommentDto getUserComment(Long userId, Long commentId);

    CommentDto updateUserComment(Long userId, Long commentId, UpdateCommentRequest updateRequest);

    void deleteUserComment(Long userId, Long commentId);

    List<CommentDto> getAdminComments(List<Long> eventIds, List<String> statuses, int from, int size);

    CommentDto updateAdminComment(Long commentId, String status);

    void deleteAdminComment(Long commentId);
}