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
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/audio")
@CrossOrigin(origins = {
        "http://localhost:3000",
        "https://justmakeit.maximezoppini.fr"
})
public class AudioController {

    private static final Logger log = LoggerFactory.getLogger(AudioController.class);

    // Whitelist des types MIME audio acceptés
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "audio/wav", "audio/x-wav",
            "audio/mpeg", "audio/mp3",
            "audio/aiff", "audio/x-aiff"
    );

    @PostMapping("/analyze-bpm")
    public ResponseEntity<AudioAnalysisResponse> analyzeBpm(@RequestParam("file") MultipartFile file) {

        // 1. Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            return ResponseEntity.status(400).body(AudioAnalysisResponse.builder()
                    .status("Error")
                    .message("Type de fichier non supporté. Formats acceptés : WAV, MP3, AIFF.")
                    .build());
        }

        // 2. Filename sanitization against path traversal
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown");
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            return ResponseEntity.status(400).body(AudioAnalysisResponse.builder()
                    .status("Error")
                    .message("Nom de fichier invalide.")
                    .build());
        }

        log.info("Fichier reçu pour analyse : {}", originalFilename);
        File tempFile = null;
        try {
            // 3. Create neutralized temporary file
            tempFile = File.createTempFile("bpm-analysis-", ".audio");
            file.transferTo(tempFile);

            // 4. Process BPM analysis
            double detectedBpm = performBpmAnalysis(tempFile);

            return ResponseEntity.ok(AudioAnalysisResponse.builder()
                    .bpm(Math.round(detectedBpm * 10.0) / 10.0)
                    .fileName(originalFilename)
                    .status("Success")
                    .serverMessage("Analyse terminée avec succès !")
                    .build());

        } catch (Exception e) {
            log.error("Erreur lors de l'analyse BPM pour le fichier '{}'", originalFilename, e);
            return ResponseEntity.status(500).body(AudioAnalysisResponse.builder()
                    .status("Error")
                    .message("Une erreur est survenue lors de l'analyse. Vérifiez que le fichier est valide.")
                    .build());
        } finally {
            // 5. Hard guarantee cleanup of temp artifacts
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private double performBpmAnalysis(File audioFile) throws IOException, UnsupportedAudioFileException {
        final List<Double> onsets = new ArrayList<>();
        int bufferSize = 2048; // Augmenté pour une meilleure précision sur les samples mélodiques
        int overlap = 1024;

        // 1. Direct stream loading for SPI format detection reliability
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat baseFormat = stream.getFormat();

            // 2. Force constraint to PCM_SIGNED 16-bit Mono (TarsosDSP requisite)
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    1,
                    2,
                    baseFormat.getSampleRate(),
                    false);

            try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(targetFormat, stream)) {
                AudioDispatcher dispatcher = new AudioDispatcher(new JVMAudioInputStream(pcmStream), bufferSize,
                        overlap);

                // 3. Sensitive melodic ComplexOnsetDetector
                ComplexOnsetDetector detector = new ComplexOnsetDetector(bufferSize, 0.35);
                detector.setHandler((time, salience) -> {
                    // Depress adjacent proximate micro-impacts (debounce 150ms)
                    if (onsets.isEmpty() || (time - onsets.get(onsets.size() - 1) > 0.15)) {
                        onsets.add(time);
                    }
                });

                dispatcher.addAudioProcessor(detector);
                dispatcher.run(); // Bloquant jusqu'à la fin du fichier
            }
        }



        if (onsets.size() < 2)
            return 128.0;

        // Deduce overall tempo from median inter-onset intervals
        List<Double> intervals = new ArrayList<>();
        for (int i = 1; i < onsets.size(); i++) {
            double interval = onsets.get(i) - onsets.get(i - 1);
            // Filter logic strictly within plausible music 60-200 BPM interval parameters
            if (interval >= 0.3 && interval <= 1.0) {
                intervals.add(interval);
            }
        }

        if (intervals.isEmpty())
            return 128.0;

        Collections.sort(intervals);
        double medianInterval = intervals.get(intervals.size() / 2);

        if (medianInterval <= 0)
            return 128.0; // Fail-safe handling zero division division
        double detectedBpm = 60.0 / medianInterval;

        // Constrain extreme multiplier boundaries back to canonical (75-175) interval
        if (Double.isInfinite(detectedBpm) || Double.isNaN(detectedBpm))
            return 128.0;

        while (detectedBpm > 0 && detectedBpm < 75)
            detectedBpm *= 2;
        while (detectedBpm > 175)
            detectedBpm /= 2;

        return detectedBpm;
    }
}