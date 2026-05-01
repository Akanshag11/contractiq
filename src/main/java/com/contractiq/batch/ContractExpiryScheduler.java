package com.contractiq.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContractExpiryScheduler {

    private final JobLauncher jobLauncher;
    private final Job contractExpiryJob;

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Kolkata")
    public void runContractExpiryJob() throws Exception {
        jobLauncher.run(contractExpiryJob,new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters());
    }
}
