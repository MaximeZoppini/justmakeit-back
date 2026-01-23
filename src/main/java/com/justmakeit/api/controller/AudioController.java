package com.justmakeit.api.controller;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/audio")
@CrossOrigin(origins = "http://localhost:3000")
public class AudioController {

    @PostMapping("/analyze-bpm")
    public ResponseEntity<Map<String, Object>> analyzeBpm(@RequestParam("file") MultipartFile file) {
        System.out.println("Fichier reçu pour analyse : " + file.getOriginalFilename());
        try {
            // 1. Créer un fichier temporaire pour TarsosDSP
            File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
            file.transferTo(tempFile);

            // 2. Analyser le BPM
            double detectedBpm = performBpmAnalysis(tempFile);

            // 3. Nettoyer le fichier temporaire
            tempFile.delete();

            Map<String, Object> response = new HashMap<>();
            response.put("bpm", Math.round(detectedBpm * 10.0) / 10.0); // Arrondi à 1 décimale
            response.put("fileName", file.getOriginalFilename());
            response.put("status", "Success");
            response.put("serverMessage", "Analyse terminée avec succès !");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("status", "Error");
            error.put("message", "Erreur lors de l'analyse : " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    private double performBpmAnalysis(File audioFile) throws IOException, UnsupportedAudioFileException {
        final List<Double> onsets = new ArrayList<>();
        int bufferSize = 2048; // Augmenté pour une meilleure précision sur les samples mélodiques
        int overlap = 1024;
        
        // 1. Charger le flux directement depuis le fichier (plus fiable pour la détection de format par les SPI)
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat baseFormat = stream.getFormat();
            
            // 2. Forcer la conversion en PCM_SIGNED 16-bit Mono (format requis par TarsosDSP)
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    1,
                    2,
                    baseFormat.getSampleRate(),
                    false
            );
            
            try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(targetFormat, stream)) {
                AudioDispatcher dispatcher = new AudioDispatcher(new JVMAudioInputStream(pcmStream), bufferSize, overlap);
                float sampleRate = targetFormat.getSampleRate();
                
                // 3. Détecteur d'onsets plus sensible pour les cloches/plucks (sensibilité 8.0, seuil -70.0dB)
                PercussionOnsetDetector detector = new PercussionOnsetDetector(sampleRate, bufferSize, 
                    (time, salience) -> onsets.add(time), 8.0, -70.0);
                
                dispatcher.addAudioProcessor(detector);
                dispatcher.run(); // Bloquant jusqu'à la fin du fichier
            }
        }

        System.out.println("Analyse terminée. Onsets détectés : " + onsets.size() + " pour " + audioFile.getName());

        if (onsets.size() < 2) return 128.0;

        // Calcul des intervalles entre les impacts pour déduire le BPM
        List<Double> intervals = new ArrayList<>();
        for (int i = 1; i < onsets.size(); i++) {
            double diff = onsets.get(i) - onsets.get(i - 1);
            if (diff > 0.2) { // On ignore les onsets trop rapprochés (> 300 BPM)
                intervals.add(diff);
            }
        }
        
        if (intervals.isEmpty()) return 128.0;

        Collections.sort(intervals);
        double medianInterval = intervals.get(intervals.size() / 2);
        
        double detectedBpm = 60.0 / medianInterval;
        
        // Normalisation pour rester dans une plage de BPM standard (ex: 75-170)
        while (detectedBpm < 75) detectedBpm *= 2;
        while (detectedBpm > 175) detectedBpm /= 2;

        return detectedBpm;
    }
}