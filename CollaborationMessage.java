package com.justmakeit.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollaborationMessage {
    private String user;
    private String action; // e.g., "TOGGLE_STEP", "CHANGE_BPM"
    private Object payload;
}