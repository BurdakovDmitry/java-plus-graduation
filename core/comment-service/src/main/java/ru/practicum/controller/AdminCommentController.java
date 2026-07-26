package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import ru.practicum.dto.AdminCommentSearchFilter;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.UpdateCommentStatusRequest;
import ru.practicum.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/comments")
public class AdminCommentController {
    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> searchCommentFilter(@Valid AdminCommentSearchFilter filter) {
        log.info("GET/admin/comments: filter={}", filter);
        return commentService.searchComments(filter);
    }

    @GetMapping("/{commentId}")
    public CommentDto findCommentById(@PathVariable @Positive Long commentId) {
        log.info("GET/admin/comments/{}", commentId);
        return commentService.findCommentById(commentId);
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateStatusComment(@PathVariable @Positive Long commentId,
                                          @Valid @RequestBody UpdateCommentStatusRequest updateStatus) {
        log.info("PATCH/admin/comments/{}", commentId);
        return commentService.updateStatusComment(commentId, updateStatus);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentById(@PathVariable @Positive Long commentId) {
        log.info("DELETE/admin/comments/{}", commentId);
        commentService.deleteComment(commentId);
    }
}
