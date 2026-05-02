package com.contractiq.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovalEscalationScheduler {

    private final JobLauncher jobLauncher;
    private final Job approvalEscalationJob;

    @Scheduled(cron="0 30 9 * * *", zone = "Asia/Kolkata")
    public void runApprovalEscalationJob() throws Exception{
        jobLauncher.run(approvalEscalationJob,
                new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters());
    }
}
