package com.justmakeit.api.controller;

import com.justmakeit.api.dto.CollaborationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Controller
public class CollaborationController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Stockage temporaire des participants pour assigner "Maker X"
    private final Map<String, Integer> projectParticipantsCount = new ConcurrentHashMap<>();

    @MessageMapping("/sync/{projectId}")
    public void broadcastUpdate(@DestinationVariable String projectId, CollaborationMessage message) {
        if ("JOIN".equals(message.getType())) {
            // Logique pour assigner un nom "Maker X"
            int count = projectParticipantsCount.getOrDefault(projectId, 0) + 1;
            projectParticipantsCount.put(projectId, count);
            message.setSender("Maker " + count);
        }

        // Sauvegarde automatique ici (Appel à un ProjectService)
        
        // Envoi ciblé uniquement aux gens dans ce salon
        messagingTemplate.convertAndSend("/topic/project/" + projectId, message);
    }
}