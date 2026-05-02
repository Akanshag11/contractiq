package com.contractiq.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovalReminderScheduler {

    private final JobLauncher jobLauncher;
    private final Job approvalReminderJob;

    @Scheduled(cron = "0 0 9 * * *")
    public void runApprovalReminderJob() throws Exception{
        jobLauncher.run(
                approvalReminderJob,
                new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters()
        );
    }
}
