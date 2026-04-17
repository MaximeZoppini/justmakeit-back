package com.justmakeit.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioAnalysisResponse {

    private String status;
    private String message;
    private Double bpm;
    private String fileName;
    private String serverMessage;
}
