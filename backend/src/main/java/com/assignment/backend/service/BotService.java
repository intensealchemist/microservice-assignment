package com.assignment.backend.service;

import com.assignment.backend.entity.Bot;
import com.assignment.backend.repository.BotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BotService {

    @Autowired
    private BotRepository botRepository;

    public List<Bot> getAllBots() {
        return botRepository.findAll();
    }

    public Optional<Bot> getBotById(Long id) {
        return botRepository.findById(id);
    }

    public Bot createBot(String name, String personaDescription) {
        Optional<Bot> existingBot = botRepository.findByName(name);
        if (existingBot.isPresent()) {
            throw new RuntimeException("Bot name already exists: " + name);
        }

        Bot bot = new Bot();
        bot.setName(name);
        if (personaDescription != null) {
            bot.setPersonaDescription(personaDescription);
        }
        return botRepository.save(bot);
    }

    public Bot updateBot(Long id, String name, String personaDescription) {
        Optional<Bot> botOpt = botRepository.findById(id);
        if (botOpt.isEmpty()) {
            throw new RuntimeException("Bot not found with id: " + id);
        }

        Bot bot = botOpt.get();
        if (name != null) {
            bot.setName(name);
        }
        if (personaDescription != null) {
            bot.setPersonaDescription(personaDescription);
        }

        return botRepository.save(bot);
    }

    public void deleteBot(Long id) {
        if (!botRepository.existsById(id)) {
            throw new RuntimeException("Bot not found with id: " + id);
        }
        botRepository.deleteById(id);
    }
}
