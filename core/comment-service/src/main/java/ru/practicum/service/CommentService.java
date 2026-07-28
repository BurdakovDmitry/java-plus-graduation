package ru.practicum.service;

import ru.practicum.dto.AdminCommentSearchFilter;
import ru.practicum.dto.CommentDto;

import ru.practicum.dto.CommentSearchParams;
import ru.practicum.dto.PostCommentParam;
import ru.practicum.dto.UpdateCommentParam;
import ru.practicum.dto.UpdateCommentStatusRequest;

import java.util.List;

public interface CommentService {
    CommentDto create(PostCommentParam postCommentParam);

    CommentDto update(UpdateCommentParam updCommentParam);

    void delete(Long userId, Long commentId);

    List<CommentDto> findAllByAuthor(Long userId);

    CommentDto findByIdAndAuthor(Long userId, Long commentId);

    List<CommentDto> findAllByEventAndAuthor(Long userId, Long eventId);

    List<CommentDto> getPublishedComments(CommentSearchParams params);

    CommentDto getPublishedComment(Long commentId);

    List<CommentDto> searchComments(AdminCommentSearchFilter filter);

    CommentDto findCommentById(Long commentId);

    CommentDto updateStatusComment(Long commentId, UpdateCommentStatusRequest status);

    void deleteComment(Long commentId);
}
