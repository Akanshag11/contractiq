package com.contractiq.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job contractExpiryJob;

    @GetMapping("/contracts/expire")
    public Map<String,Object> runContractExpiryJob() throws Exception{
        jobLauncher.run(
                contractExpiryJob,
                new JobParametersBuilder()
                        .addLong("runAt", System.currentTimeMillis())
                        .toJobParameters()
        );

        return Map.of("message","Contract expiry batch job triggered");

    }
}
