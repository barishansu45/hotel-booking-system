package com.hotelbooking.comment.controller;

import com.hotelbooking.comment.model.Comment;
import com.hotelbooking.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<Comment> createComment(@RequestBody Comment comment) {
        return ResponseEntity.ok(commentService.createComment(comment));
    }

    /**
     * Paginated comment list.
     * GET /comments/hotel/{hotelId}?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<Page<Comment>> getComments(
            @PathVariable UUID hotelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(commentService.getCommentsByHotelId(hotelId, pageable));
    }

    /**
     * Full (non-paginated) list kept for the frontend graph/distribution feature.
     * GET /comments/hotel/{hotelId}/all
     */
    @GetMapping("/hotel/{hotelId}/all")
    public ResponseEntity<List<Comment>> getAllComments(@PathVariable UUID hotelId) {
        return ResponseEntity.ok(commentService.getCommentsByHotelId(hotelId));
    }
}
