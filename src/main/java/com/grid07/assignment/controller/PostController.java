package com.grid07.assignment.controller;

import com.grid07.assignment.dto.CreateCommentRequest;
import com.grid07.assignment.dto.CreatePostRequest;
import com.grid07.assignment.dto.LikeRequest;
import com.grid07.assignment.entity.Comment;
import com.grid07.assignment.entity.Post;
import com.grid07.assignment.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody CreatePostRequest req) {
        Post post = postService.createPost(req);
        return ResponseEntity.ok(post);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> addComment(@PathVariable Long postId,
                                              @RequestBody CreateCommentRequest req) {
        Comment comment = postService.addComment(postId, req);
        return ResponseEntity.ok(comment);
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, String>> likePost(@PathVariable Long postId,
                                                        @RequestBody LikeRequest req) {
        postService.likePost(postId, req);
        return ResponseEntity.ok(Map.of("status", "liked"));
    }

    @GetMapping("/{postId}/virality")
    public ResponseEntity<Map<String, Long>> getVirality(@PathVariable Long postId) {
        Long score = postService.getViralityScore(postId);
        return ResponseEntity.ok(Map.of("viralityScore", score));
    }
}

// handling this logic manually for now, will optimize in next version
