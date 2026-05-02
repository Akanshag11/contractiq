package com.contractiq.batch;

import com.contractiq.domain.workflow.ApprovalStepStatus;
import com.contractiq.domain.workflow.ContractApprovalStep;
import com.contractiq.kafka.ContractEventMessage;
import com.contractiq.kafka.ContractEventProducer;
import com.contractiq.repository.ContractApprovalStepRepository;
import com.contractiq.service.ContractAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class ApprovalReminderBatchConfig {
    private final ContractApprovalStepRepository approvalStepRepository;
    private final ContractEventProducer eventProducer;

    private final ContractAuditService contractAuditService;


    @Bean
    public Job approvalReminderJob(JobRepository jobRepository, Step approvalReminderStep) {
        return new JobBuilder("approvalReminderJob", jobRepository)
                .start(approvalReminderStep)
                .build();
    }

    @Bean
    public Step approvalReminderStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("approvalReminderStep",jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDateTime cutoff=LocalDateTime.now().minusHours(48);

                    List<ContractApprovalStep> staleApprovals=
                            approvalStepRepository.findByStatusAndCreatedAtBefore(
                                    ApprovalStepStatus.PENDING,
                                    cutoff
                            );

                    for(ContractApprovalStep step:staleApprovals)
                    {
                        eventProducer.sendEvent(
                                ContractEventMessage.builder()
                                        .eventId(UUID.randomUUID().toString())
                                        .eventVersion("v1")
                                        .type("APPROVAL_REMINDER")
                                        .contractId(step.getContract().getId().toString())
                                        .toEmail(step.getApprover().getEmail())
                                        .message(
                                                "Reminder: Contract '"+ step.getContract().getTitle() +"' is pending your approval"
                                        )
                                        .build()
                        );

                        contractAuditService.log(
                                step.getContract().getId().toString(),
                                "APPROVAL_REMINDER_SENT",
                                "SYSTEM",
                                "SYSTEM",
                                step.getContract().getStatus().name(),
                                step.getContract().getStatus().name(),
                                "Approval remonder sent to" + step.getApprover().getEmail(),
                                step.getApprovalRound(),
                                step.getStepOrder()
                        );
                    }
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                },transactionManager)
                .build();
    }


}
