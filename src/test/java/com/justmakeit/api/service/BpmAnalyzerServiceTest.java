package com.justmakeit.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BpmAnalyzerServiceTest {

    private BpmAnalyzerService bpmAnalyzerService;

    @BeforeEach
    void setUp() {
        bpmAnalyzerService = new BpmAnalyzerService();
    }

    @Test
    void testNormalizeBpm() throws Exception {
        Method method = BpmAnalyzerService.class.getDeclaredMethod("normalizeBpm", double.class);
        method.setAccessible(true);

        // Test normal range
        assertEquals(120.0, (double) method.invoke(bpmAnalyzerService, 120.0));
        
        // Test too low (should double)
        assertEquals(120.0, (double) method.invoke(bpmAnalyzerService, 60.0));
        assertEquals(80.0, (double) method.invoke(bpmAnalyzerService, 40.0));

        // Test too high (should divide)
        assertEquals(100.0, (double) method.invoke(bpmAnalyzerService, 200.0));
        assertEquals(125.0, (double) method.invoke(bpmAnalyzerService, 250.0));

        // Test special values
        assertEquals(128.0, (double) method.invoke(bpmAnalyzerService, Double.NaN));
        assertEquals(128.0, (double) method.invoke(bpmAnalyzerService, Double.POSITIVE_INFINITY));
    }

    @Test
    void testExtractValidIntervals() throws Exception {
        Method method = BpmAnalyzerService.class.getDeclaredMethod("extractValidIntervals", List.class);
        method.setAccessible(true);

        List<Double> onsets = Arrays.asList(0.0, 0.5, 1.0, 1.2, 2.0);
        // Intervals: 0.5, 0.5, 0.2 (skipped), 0.8
        List<Double> intervals = (List<Double>) method.invoke(bpmAnalyzerService, onsets);

        assertEquals(3, intervals.size());
        assertEquals(0.5, intervals.get(0));
        assertEquals(0.5, intervals.get(1));
        assertEquals(0.8, intervals.get(2));
    }

    @Test
    void testCalculateBpmFromIntervals() throws Exception {
        Method method = BpmAnalyzerService.class.getDeclaredMethod("calculateBpmFromIntervals", List.class);
        method.setAccessible(true);

        // Median 0.5s -> 60/0.5 = 120 BPM
        List<Double> intervals = Arrays.asList(0.5, 0.5, 0.6);
        double bmp = (double) method.invoke(bpmAnalyzerService, intervals);
        assertEquals(120.0, bmp);
    }
}
