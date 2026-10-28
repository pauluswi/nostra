package com.pswied.core.nostra.cob;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CobController {

    private final CobService cobService;

    public CobController(CobService cobService) {
        this.cobService = cobService;
    }

    @PostMapping("/cob/run")
    public ResponseEntity<CobRunResponse> runCob() {
        return ResponseEntity.status(HttpStatus.CREATED).body(cobService.runDailyCob());
    }

    @GetMapping("/cob/latest")
    public CobRunResponse getLatestCob() {
        return cobService.getLatestCob();
    }
}
