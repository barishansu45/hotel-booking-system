package com.hotelbooking.comment.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Document(collection = "comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    private String id;
    private UUID hotelId;
    private UUID userId;
    private String userName;
    private Double rating;
    private String comment;
    private Map<String, Integer> serviceRatings;
    private LocalDateTime createdAt;
    private Boolean isVerifiedStay;
    private UUID bookingId;
}
