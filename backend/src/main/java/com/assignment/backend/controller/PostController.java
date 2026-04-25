package com.assignment.backend.controller;

import com.assignment.backend.entity.Comment;
import com.assignment.backend.entity.Post;
import com.assignment.backend.repository.CommentRepository;
import com.assignment.backend.service.GuardrailService;
import com.assignment.backend.service.NotificationService;
import com.assignment.backend.service.PostService;
import com.assignment.backend.service.ViralityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ViralityService viralityService;

    @Autowired
    private GuardrailService guardrailService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public List<Post> getAllPosts() {
        return postService.getAllPosts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPostById(@PathVariable Long id) {
        Optional<Post> post = postService.getPostById(id);
        if (post.isPresent()) {
            return ResponseEntity.ok(post.get());
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Post not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/author/{authorId}")
    public List<Post> getPostsByAuthor(@PathVariable Long authorId, @RequestParam String authorType) {
        return postService.getPostsByAuthor(authorId, authorType);
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Map<String, Object> request) {
        try {
            Long authorId = Long.valueOf(request.get("authorId").toString());
            String authorType = (String) request.get("authorType");
            String content = (String) request.get("content");

            if (content == null || content.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Content is required");
                return ResponseEntity.badRequest().body(error);
            }

            Post post = postService.createPost(authorId, authorType, content);
            return ResponseEntity.status(HttpStatus.CREATED).body(post);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");

            Post post = postService.updatePost(id, content);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        try {
            postService.deletePost(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Post deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long postId, @RequestBody Map<String, Object> request) {
        try {
            Long authorId = Long.valueOf(request.get("authorId").toString());
            String authorType = (String) request.get("authorType");
            String content = (String) request.get("content");
            Long parentCommentId = request.get("parentCommentId") != null
                    ? Long.valueOf(request.get("parentCommentId").toString())
                    : null;

            Optional<Post> postOpt = postService.getPostById(postId);
            if (postOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Post not found"));
            }
            Post post = postOpt.get();

            int depthLevel = 0;
            if (parentCommentId != null) {
                Optional<Comment> parentOpt = commentRepository.findById(parentCommentId);
                if (parentOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Parent comment not found"));
                }
                depthLevel = parentOpt.get().getDepthLevel() + 1;
            }

            if (!guardrailService.checkDepthLevel(depthLevel)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "Comment thread exceeds maximum depth of 20"));
            }

            if ("BOT".equals(authorType) && "USER".equals(post.getAuthorType())) {
                if (!guardrailService.checkCooldown(authorId, post.getAuthorId())) {
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                            .body(Map.of("error", "Bot cooldown active for this human user"));
                }
            }

            if ("BOT".equals(authorType)) {
                if (!guardrailService.checkAndIncrementBotCount(postId)) {
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                            .body(Map.of("error", "Post has reached maximum bot comment limit (100)"));
                }
            }

            Comment comment = Comment.builder()
                    .postId(postId)
                    .parentCommentId(parentCommentId)
                    .authorId(authorId)
                    .authorType(authorType)
                    .content(content)
                    .depthLevel(depthLevel)
                    .build();

            Comment saved = commentRepository.save(comment);

            if ("USER".equals(authorType)) {
                viralityService.incrementScore(postId, "HUMAN_COMMENT");
            } else {
                viralityService.incrementScore(postId, "BOT_REPLY");
            }

            if ("BOT".equals(authorType)) {
                notificationService.handleBotInteraction(post.getAuthorId(),
                        "Bot " + authorId + " commented on your post");
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<?> likePost(@PathVariable Long postId, @RequestParam Long userId) {
        try {
            Optional<Post> postOpt = postService.getPostById(postId);
            if (postOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Post not found"));
            }

            viralityService.incrementScore(postId, "HUMAN_LIKE");

            return ResponseEntity.ok(Map.of("message", "Post liked successfully"));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
