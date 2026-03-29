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
    private String type;
    private String projectId;
    private String deviceId;
    private Map<String, Object> payload;
}