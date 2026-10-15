package com.bizengine.system.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {
    @GetMapping("/status")
    public SystemStatusResponse status() {
        return new SystemStatusResponse("biz-engine", "UP", Instant.now());
    }
    public record SystemStatusResponse(String application, String status, Instant timestamp) {}
}