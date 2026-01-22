package com.justmakeit.api.controller;

import com.justmakeit.api.dto.CollaborationMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class CollaborationController {

    @MessageMapping("/sync") // Le front envoie à /app/sync
    @SendTo("/topic/project") // Tout le monde reçoit sur /topic/project
    public CollaborationMessage broadcastUpdate(CollaborationMessage message) {
        // On pourrait ici sauvegarder l'état en DB avant de renvoyer
        return message;
    }
}