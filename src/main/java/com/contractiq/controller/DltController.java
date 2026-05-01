package com.contractiq.controller;

import com.contractiq.service.DltReprocessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dlt")
@RequiredArgsConstructor
public class DltController {

    private final DltReprocessService dltReprocessService;

    @PostMapping("/reprocess")
    public Map<String, Object> reprocessDlt() throws InterruptedException {
        int count = dltReprocessService.reprocessDltMessages();
        return Map.of(
                "message", "Dlt reprocess completed",
                "reprocessedCount", count
        );
    }
}
