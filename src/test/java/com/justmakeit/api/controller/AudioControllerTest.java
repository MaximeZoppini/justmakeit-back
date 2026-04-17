package com.justmakeit.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.justmakeit.api.service.BpmAnalyzerService;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AudioController.class)
class AudioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BpmAnalyzerService bpmAnalyzerService;

    @Test
    void testAnalyzeBpmSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.wav",
            "audio/wav",
            "test content".getBytes()
        );

        when(bpmAnalyzerService.analyze(any(File.class))).thenReturn(120.0);

        mockMvc.perform(multipart("/api/audio/analyze-bpm").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Success"))
            .andExpect(jsonPath("$.bpm").value(120.0))
            .andExpect(jsonPath("$.fileName").value("test.wav"));
    }

    @Test
    void testAnalyzeBpmInvalidType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "test content".getBytes()
        );

        mockMvc.perform(multipart("/api/audio/analyze-bpm").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("Error"))
            .andExpect(jsonPath("$.message").value("Type de fichier non supporté. Formats acceptés : WAV, MP3, AIFF."));
    }

    @Test
    void testAnalyzeBpmInvalidFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test/../danger.wav",
            "audio/wav",
            "test content".getBytes()
        );

        mockMvc.perform(multipart("/api/audio/analyze-bpm").file(file))
            .andExpect(status().isOk());
    }
}
