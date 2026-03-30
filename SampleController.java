package com.justmakeit.api.controller;

import com.justmakeit.api.dto.BpmResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/samples")
@CrossOrigin(origins = {
        "http://localhost:3000",
        "https://justmakeit.maximezoppini.fr"
})
public class SampleController {

    @PostMapping("/analyze")
    public ResponseEntity<BpmResponse> analyzeSample(@RequestParam("file") MultipartFile file) {
        // TODO: Intégrer TarsosDSP pour la détection réelle
        // Simulation d'une analyse
        String fileName = file.getOriginalFilename();
        double mockBpm = 128.0;
        String mockKey = "Am";

        return ResponseEntity.ok(new BpmResponse(fileName, mockBpm, mockKey));
    }
}