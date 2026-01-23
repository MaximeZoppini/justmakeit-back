package com.justmakeit.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollaborationMessage {
    private String sender;
    private String type; // Ex: "JOIN", "NOTE_TOGGLED", "BPM_CHANGED"
    private String projectId;
    private String deviceId;
    private Map<String, Object> payload; // Contient les données spécifiques (ex: { note: 'C3', step: 4 })
}