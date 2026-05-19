package com.hotelbooking.comment.repository;

import com.hotelbooking.comment.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByHotelId(UUID hotelId);
    Page<Comment> findByHotelId(UUID hotelId, Pageable pageable);
    List<Comment> findByUserId(UUID userId);
}
