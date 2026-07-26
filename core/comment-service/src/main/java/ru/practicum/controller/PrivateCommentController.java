package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.PostCommentDto;
import ru.practicum.dto.PostCommentParam;
import ru.practicum.dto.UpdateCommentParam;
import ru.practicum.service.CommentService;
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

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
public class PrivateCommentController {
    private final CommentService commentService;

    @GetMapping("/comments")
    public List<CommentDto> findAllByUser(@PathVariable @Positive Long userId) {
        log.info("GET/users/{}/comments", userId);
        return commentService.findAllByAuthor(userId);
    }

    @GetMapping("/comments/{commentId}")
    public CommentDto findByIdAndAuthor(@PathVariable @Positive Long userId,
                                        @PathVariable @Positive Long commentId) {
        log.info("GET/users/{}/comments/{}", userId, commentId);
        return commentService.findByIdAndAuthor(userId, commentId);
    }

    @GetMapping("/events/{eventId}/comments")
    public List<CommentDto> findAllByEventAndAuthor(@PathVariable @Positive Long userId,
                                                    @PathVariable @Positive Long eventId) {
        log.info("GET/users/{}/events/{}/comments", userId, eventId);
        return commentService.findAllByEventAndAuthor(userId, eventId);
    }

    @PostMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto create(@PathVariable @Positive Long userId,
                             @PathVariable @Positive Long eventId,
                             @Valid @RequestBody PostCommentDto postCommentDto) {
        log.info("POST/users/{}/events/{}/comments: postCommentDto={}", userId, eventId, postCommentDto);
        PostCommentParam postCommentParam = new PostCommentParam(userId, eventId, postCommentDto.comment());
        return commentService.create(postCommentParam);
    }

    @PatchMapping("/comments/{commentId}")
    public CommentDto update(@PathVariable @Positive Long userId,
                             @PathVariable @Positive Long commentId,
                             @Valid @RequestBody PostCommentDto postCommentDto) {
        log.info("PATCH/users/{}/comments/{}: postCommentDto={}", userId, commentId, postCommentDto);
        UpdateCommentParam updCommentParam = new UpdateCommentParam(userId, commentId, postCommentDto.comment());
        return commentService.update(updCommentParam);

    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long userId,
                       @PathVariable @Positive Long commentId) {
        log.info("DELETE/users/{}/comments/{}", userId, commentId);
        commentService.delete(userId, commentId);
    }
}
