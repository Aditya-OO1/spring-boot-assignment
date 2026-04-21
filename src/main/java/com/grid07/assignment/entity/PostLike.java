package com.grid07.assignment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_likes")
@Data
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "liked_at")
    private LocalDateTime likedAt;

    @PrePersist
    public void prePersist() {
        likedAt = LocalDateTime.now();
    }
}
