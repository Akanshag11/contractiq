package com.contractiq.batch;

import com.contractiq.domain.party.Role;
import com.contractiq.domain.party.User;
import com.contractiq.domain.party.UserRepository;
import com.contractiq.domain.workflow.ApprovalStepStatus;
import com.contractiq.domain.workflow.ContractApprovalStep;
import com.contractiq.kafka.ContractEventMessage;
import com.contractiq.kafka.ContractEventProducer;
import com.contractiq.repository.ContractApprovalStepRepository;
import com.contractiq.service.ContractAuditService;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.C;
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
public class ApprovalEscalationBatchConfig {

    private final ContractApprovalStepRepository approvalStepRepository;
    private final UserRepository userRepository;
    private final ContractAuditService contractAuditService;
    private final ContractEventProducer eventProducer;

    @Bean
    public Job approvalEscalationJob(JobRepository jobRepository, Step approvalEscalationStep)
    {
        return new JobBuilder("approvalEscalationJob", jobRepository)
                .start(approvalEscalationStep)
                .build();
    }

    public Step approvalEscalationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager)
    {
        return new StepBuilder("approvalEscalationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDateTime cutoff=LocalDateTime.now().minusDays(5);

                    List<ContractApprovalStep> staleApprovals=
                            approvalStepRepository.findByStatusAndCreatedAtBefore(ApprovalStepStatus.PENDING, cutoff);

                    User admin=userRepository.findAll().stream()
                            .filter(user->user.getRole()== Role.ADMIN)
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Admin not found"));

                    for(ContractApprovalStep step:staleApprovals)
                    {
                        eventProducer.sendEvent(
                                ContractEventMessage.builder()
                                        .eventId(UUID.randomUUID().toString())
                                        .eventVersion("v1")
                                        .type("APPROVAL_ESCALATION")
                                        .contractId(step.getContract().getId().toString())
                                        .toEmail(admin.getEmail())
                                        .message(
                                                "Escalation: Contract '" +
                                                        step.getContract().getTitle() +
                                                        "' has been pending approval for more than 5 days. " +
                                                        "Current approver: " +
                                                        step.getApprover().getEmail()
                                        )
                                        .build()
                        );

                        contractAuditService.log(
                                step.getContract().getId().toString(),
                                "APPROVAL_ESCALATED",
                                "SYSTEM",
                                "SYSTEM",
                                step.getContract().getStatus().name(),
                                step.getContract().getStatus().name(),
                                "Approval escalated to admin. Current approver: " + step.getApprover().getEmail(),
                                step.getApprovalRound(),
                                step.getStepOrder()
                        );
                    }
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

}
