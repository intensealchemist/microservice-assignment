package com.assignment.backend.service;

import com.assignment.backend.entity.Post;
import com.assignment.backend.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }

    public List<Post> getPostsByAuthor(Long authorId, String authorType) {
        return postRepository.findByAuthorIdAndAuthorType(authorId, authorType);
    }

    public Post createPost(Long authorId, String authorType, String content) {
        Post post = Post.builder()
                .authorId(authorId)
                .authorType(authorType)
                .content(content)
                .build();
        return postRepository.save(post);
    }

    public Post updatePost(Long id, String content) {
        Optional<Post> postOpt = postRepository.findById(id);
        if (postOpt.isEmpty()) {
            throw new RuntimeException("Post not found with id: " + id);
        }

        Post post = postOpt.get();
        if (content != null) {
            post.setContent(content);
        }

        return postRepository.save(post);
    }

    public void deletePost(Long id) {
        if (!postRepository.existsById(id)) {
            throw new RuntimeException("Post not found with id: " + id);
        }
        postRepository.deleteById(id);
    }
}
