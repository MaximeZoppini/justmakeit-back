package com.justmakeit.api.controller;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.justmakeit.api.dto.AudioAnalysisResponse;
import javax.sound.sampled.*;
import com.justmakeit.api.service.BpmAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Set;

@RestController
@RequestMapping("/api/audio")
@CrossOrigin(origins = {
        "http://localhost:3000",
        "https://justmakeit.maximezoppini.fr"
})
public class AudioController {

    private static final Logger log = LoggerFactory.getLogger(AudioController.class);

    private final BpmAnalyzerService bpmAnalyzerService;

    @Autowired
    public AudioController(BpmAnalyzerService bpmAnalyzerService) {
        this.bpmAnalyzerService = bpmAnalyzerService;
    }

    // Whitelist des types MIME audio acceptés
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "audio/wav", "audio/x-wav",
            "audio/mpeg", "audio/mp3",
            "audio/aiff", "audio/x-aiff"
    );

    @PostMapping("/analyze-bpm")
    public ResponseEntity<AudioAnalysisResponse> analyzeBpm(@RequestParam("file") MultipartFile file) {

        String originalFilename = org.springframework.util.StringUtils.cleanPath(
            file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"
        );

        ResponseEntity<AudioAnalysisResponse> validationError = validateFile(file, originalFilename);
        if (validationError != null) return validationError;

        log.info("Fichier reçu pour analyse : {}", originalFilename);
        File tempFile = null;
        try {
            // 3. Create neutralized temporary file
            tempFile = File.createTempFile("bpm-analysis-", ".audio");
            file.transferTo(tempFile);

            double detectedBpm = bpmAnalyzerService.analyze(tempFile);

            return ResponseEntity.ok(AudioAnalysisResponse.builder()
                    .bpm(Math.round(detectedBpm * 10.0) / 10.0)
                    .fileName(originalFilename)
                    .status("Success")
                    .serverMessage("Analyse terminée avec succès !")
                    .build());

        } catch (Exception e) {
            log.error("Erreur lors de l'analyse BPM pour le fichier '{}'", originalFilename, e);
            return buildErrorResponse(500, "Une erreur est survenue lors de l'analyse. Vérifiez que le fichier est valide.");
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private ResponseEntity<AudioAnalysisResponse> validateFile(MultipartFile file, String originalFilename) {
        if (file.getContentType() == null || !ALLOWED_MIME_TYPES.contains(file.getContentType())) {
            return buildErrorResponse(400, "Type de fichier non supporté. Formats acceptés : WAV, MP3, AIFF.");
        }
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            return buildErrorResponse(400, "Nom de fichier invalide.");
        }
        return null;
    }

    private ResponseEntity<AudioAnalysisResponse> buildErrorResponse(int status, String message) {
        return ResponseEntity.status(status).body(
            AudioAnalysisResponse.builder().status("Error").message(message).build()
        );
    }
}