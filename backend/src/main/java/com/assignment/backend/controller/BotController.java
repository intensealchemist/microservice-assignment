package com.assignment.backend.controller;

import com.assignment.backend.entity.Bot;
import com.assignment.backend.service.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/bots")
public class BotController {

    @Autowired
    private BotService botService;

    @GetMapping
    public List<Bot> getAllBots() {
        return botService.getAllBots();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBotById(@PathVariable Long id) {
        Optional<Bot> bot = botService.getBotById(id);
        if (bot.isPresent()) {
            return ResponseEntity.ok(bot.get());
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Bot not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PostMapping
    public ResponseEntity<?> createBot(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            if (name == null || name.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Bot name is required");
                return ResponseEntity.badRequest().body(error);
            }

            String personDescription = request.get("personDescription") != null
                    ? (String) request.get("personDescription")
                    : null;

            Bot bot = botService.createBot(name, personDescription);
            return ResponseEntity.status(HttpStatus.CREATED).body(bot);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBot(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String personDescription = request.get("personDescription") != null
                    ? (String) request.get("personDescription")
                    : null;

            Bot bot = botService.updateBot(id, name, personDescription);
            return ResponseEntity.ok(bot);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBot(@PathVariable Long id) {
        try {
            botService.deleteBot(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Bot deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
