package com.assignment.backend.repository;

import com.assignment.backend.entity.Bot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BotRepository extends JpaRepository<Bot, Long> {
    Optional<Bot> findByName(String name);
}
