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

        // 1. Validation du type MIME
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            return ResponseEntity.status(400).body(AudioAnalysisResponse.builder()
                    .status("Error")
                    .message("Type de fichier non supporté. Formats acceptés : WAV, MP3, AIFF.")
                    .build());
        }

        // 2. Sanitisation du nom de fichier (prévention Path Traversal)
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
            // 3. Fichier temp avec nom neutre (pas de suffixe original)
            tempFile = File.createTempFile("bpm-analysis-", ".audio");
            file.transferTo(tempFile);

            // 4. Analyser le BPM
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
            // 5. Nettoyage garanti du fichier temporaire
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private double performBpmAnalysis(File audioFile) throws IOException, UnsupportedAudioFileException {
        final List<Double> onsets = new ArrayList<>();
        int bufferSize = 2048; // Augmenté pour une meilleure précision sur les samples mélodiques
        int overlap = 1024;

        // 1. Charger le flux directement depuis le fichier (plus fiable pour la
        // détection de format par les SPI)
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat baseFormat = stream.getFormat();

            // 2. Forcer la conversion en PCM_SIGNED 16-bit Mono (format requis par
            // TarsosDSP)
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

                // 3. Utilisation du ComplexOnsetDetector : bien meilleur pour les cloches et
                // sons mélodiques
                // Le seuil (0.25) définit la sensibilité. Plus il est bas, plus il détecte de
                // petits pics.
                ComplexOnsetDetector detector = new ComplexOnsetDetector(bufferSize, 0.35); // Seuil légèrement augmenté
                detector.setHandler((time, salience) -> {
                    // DEBOUNCING : On ignore les impacts trop proches (moins de 150ms)
                    if (onsets.isEmpty() || (time - onsets.get(onsets.size() - 1) > 0.15)) {
                        onsets.add(time);
                    }
                });

                dispatcher.addAudioProcessor(detector);
                dispatcher.run(); // Bloquant jusqu'à la fin du fichier
            }
        }

        System.out.println("Analyse terminée. Onsets détectés : " + onsets.size() + " pour " + audioFile.getName());

        if (onsets.size() < 2)
            return 128.0;

        // Calcul des intervalles entre les impacts pour déduire le BPM
        List<Double> intervals = new ArrayList<>();
        for (int i = 1; i < onsets.size(); i++) {
            double interval = onsets.get(i) - onsets.get(i - 1);
            // On ne garde que les intervalles qui correspondent à un BPM entre 60 et 200
            if (interval >= 0.3 && interval <= 1.0) {
                intervals.add(interval);
            }
        }

        if (intervals.isEmpty())
            return 128.0;

        Collections.sort(intervals);
        double medianInterval = intervals.get(intervals.size() / 2);

        if (medianInterval <= 0)
            return 128.0; // Sécurité anti-division par zéro
        double detectedBpm = 60.0 / medianInterval;

        // Normalisation sécurisée pour rester dans une plage de BPM standard (ex:
        // 75-175)
        if (Double.isInfinite(detectedBpm) || Double.isNaN(detectedBpm))
            return 128.0;

        while (detectedBpm > 0 && detectedBpm < 75)
            detectedBpm *= 2;
        while (detectedBpm > 175)
            detectedBpm /= 2;

        return detectedBpm;
    }
}