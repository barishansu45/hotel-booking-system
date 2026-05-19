package com.hotelbooking.comment.service;

import com.hotelbooking.comment.model.Comment;
import com.hotelbooking.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;

    public Comment createComment(Comment comment) {
        comment.setCreatedAt(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    /** Non-paginated (kept for graph/distribution endpoint). */
    public List<Comment> getCommentsByHotelId(UUID hotelId) {
        return commentRepository.findByHotelId(hotelId);
    }

    /** Paginated list for REST consumers. */
    public Page<Comment> getCommentsByHotelId(UUID hotelId, Pageable pageable) {
        return commentRepository.findByHotelId(hotelId, pageable);
    }
}
