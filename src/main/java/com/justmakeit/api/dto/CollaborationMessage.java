package com.justmakeit.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollaborationMessage {
    private String sender;
    private String type;
    private String projectId;
    private String deviceId;
    private JsonNode payload;
}