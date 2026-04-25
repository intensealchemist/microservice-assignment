package com.assignment.backend.repository;

import com.assignment.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostId(Long postId);
    long countByPostIdAndAuthorType(Long postId, String authorType);
}
