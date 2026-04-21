package com.grid07.assignment.service;

import com.grid07.assignment.dto.CreateCommentRequest;
import com.grid07.assignment.dto.CreatePostRequest;
import com.grid07.assignment.dto.LikeRequest;
import com.grid07.assignment.entity.Comment;
import com.grid07.assignment.entity.Post;
import com.grid07.assignment.entity.PostLike;
import com.grid07.assignment.repository.BotRepository;
import com.grid07.assignment.repository.CommentRepository;
import com.grid07.assignment.repository.PostLikeRepository;
import com.grid07.assignment.repository.PostRepository;
import com.grid07.assignment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BotRepository botRepository;

    @Autowired
    private ViralityService viralityService;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public Post createPost(CreatePostRequest req) {
        Post post = new Post();
        post.setAuthorId(req.getAuthorId());
        post.setAuthorType(req.getAuthorType());
        post.setContent(req.getContent());
        return postRepository.save(post);
    }

    @Transactional
    public Comment addComment(Long postId, CreateCommentRequest req) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        if (req.getDepthLevel() > 20) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Thread too deep");
        }

        boolean isBot = "BOT".equalsIgnoreCase(req.getAuthorType());

        if (isBot) {
            boolean allowed = viralityService.checkAndIncrementBotCount(postId);
            if (!allowed) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bot reply cap reached for this post");
            }

            if (req.getHumanUserId() != null) {
                if (viralityService.isCooldownActive(req.getAuthorId(), req.getHumanUserId())) {
                    viralityService.checkAndIncrementBotCount(postId);
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bot cooldown active");
                }
                viralityService.setCooldown(req.getAuthorId(), req.getHumanUserId());

                String botName = botRepository.findById(req.getAuthorId())
                        .map(b -> b.getName())
                        .orElse("Bot " + req.getAuthorId());

                notificationService.handleBotNotification(req.getHumanUserId(), botName, postId);
            }

            viralityService.addBotReply(postId);
        } else {
            viralityService.addHumanComment(postId);
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(req.getAuthorId());
        comment.setAuthorType(req.getAuthorType());
        comment.setContent(req.getContent());
        comment.setDepthLevel(req.getDepthLevel());

        return commentRepository.save(comment);
    }

    @Transactional
    public void likePost(Long postId, LikeRequest req) {
        postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(req.getUserId());
        postLikeRepository.save(like);

        viralityService.addHumanLike(postId);
    }

    public Long getViralityScore(Long postId) {
        return viralityService.getViralityScore(postId);
    }
}
