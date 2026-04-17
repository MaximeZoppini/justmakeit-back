package com.justmakeit.api.controller;

import com.justmakeit.api.dto.CollaborationMessage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class CollaborationController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Transient memory allocation tracking identity indexing
    private final Map<String, Integer> projectParticipantsCount = new ConcurrentHashMap<>();

    @MessageMapping("/sync/{projectId}")
    public void broadcastUpdate(
        @DestinationVariable String projectId,
        CollaborationMessage message
    ) {
        if ("JOIN".equals(message.getType())) {
            // Identity indexing assignment
            int count = projectParticipantsCount.getOrDefault(projectId, 0) + 1;
            projectParticipantsCount.put(projectId, count);
            message.setSender("Maker " + count);
        }

        // Restrict broadcast domain strictly to respective local namespace
        messagingTemplate.convertAndSend("/topic/project/" + projectId, message);
    }
}
