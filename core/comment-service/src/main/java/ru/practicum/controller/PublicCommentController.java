package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.CommentSearchParams;
import ru.practicum.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class PublicCommentController {
    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getComments(@Valid CommentSearchParams params) {
        log.info("GET/comments: params={}", params);
        return commentService.getPublishedComments(params);
    }

    @GetMapping("/{commentId}")
    public CommentDto getComment(@PathVariable @Positive Long commentId) {
        log.info("GET/comments/{}", commentId);
        return commentService.getPublishedComment(commentId);
    }
}