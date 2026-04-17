package com.justmakeit.api.service;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.springframework.stereotype.Service;

@Service
public class BpmAnalyzerService {

    public double analyze(File audioFile) throws IOException, UnsupportedAudioFileException {
        List<Double> onsets = detectOnsets(audioFile);
        if (onsets.size() < 2) return 128.0;

        List<Double> intervals = extractValidIntervals(onsets);
        if (intervals.isEmpty()) return 128.0;

        return calculateBpmFromIntervals(intervals);
    }

    private List<Double> detectOnsets(File audioFile)
        throws IOException, UnsupportedAudioFileException {
        final List<Double> onsets = new ArrayList<>();
        int bufferSize = 2048;
        int overlap = 1024;

        try (AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat targetFormat = getTargetFormat(stream.getFormat());

            try (
                AudioInputStream pcmStream = AudioSystem.getAudioInputStream(targetFormat, stream)
            ) {
                AudioDispatcher dispatcher = new AudioDispatcher(
                    new JVMAudioInputStream(pcmStream),
                    bufferSize,
                    overlap
                );

                ComplexOnsetDetector detector = new ComplexOnsetDetector(bufferSize, 0.35);
                detector.setHandler((time, salience) -> {
                    if (onsets.isEmpty() || (time - onsets.get(onsets.size() - 1) > 0.15)) {
                        onsets.add(time);
                    }
                });

                dispatcher.addAudioProcessor(detector);
                dispatcher.run();
            }
        }
        return onsets;
    }

    private AudioFormat getTargetFormat(AudioFormat baseFormat) {
        return new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            baseFormat.getSampleRate(),
            16,
            1,
            2,
            baseFormat.getSampleRate(),
            false
        );
    }

    private List<Double> extractValidIntervals(List<Double> onsets) {
        List<Double> intervals = new ArrayList<>();
        for (int i = 1; i < onsets.size(); i++) {
            double interval = onsets.get(i) - onsets.get(i - 1);
            if (interval >= 0.3 && interval <= 1.0) {
                intervals.add(interval);
            }
        }
        return intervals;
    }

    private double calculateBpmFromIntervals(List<Double> intervals) {
        Collections.sort(intervals);
        double medianInterval = intervals.get(intervals.size() / 2);

        if (medianInterval <= 0) return 128.0;

        double detectedBpm = 60.0 / medianInterval;
        return normalizeBpm(detectedBpm);
    }

    private double normalizeBpm(double detectedBpm) {
        if (Double.isInfinite(detectedBpm) || Double.isNaN(detectedBpm)) return 128.0;

        while (detectedBpm > 0 && detectedBpm < 75) detectedBpm *= 2;
        while (detectedBpm > 175) detectedBpm /= 2;

        return detectedBpm;
    }
}
