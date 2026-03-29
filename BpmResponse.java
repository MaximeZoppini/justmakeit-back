package com.justmakeit.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BpmResponse {
    private String fileName;
    private double bpm;
    private String detectedKey;
}