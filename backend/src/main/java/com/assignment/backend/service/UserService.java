package com.assignment.backend.service;

import com.assignment.backend.entity.User;
import com.assignment.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(String username) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            throw new RuntimeException("Username already exists: " + username);
        }

        User user = new User();
        user.setUsername(username);
        return userRepository.save(user);
    }

    public User updateUser(Long id, String username, Boolean isPremium) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found with id: " + id);
        }

        User user = userOpt.get();
        if (username != null) {
            user.setUsername(username);
        }
        if (isPremium != null) {
            user.setIsPremium(isPremium);
        }

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
