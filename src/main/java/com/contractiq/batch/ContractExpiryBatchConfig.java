package com.contractiq.batch;

import com.contractiq.domain.contract.Contract;
import com.contractiq.domain.contract.ContractStatus;
import com.contractiq.kafka.ContractEventMessage;
import com.contractiq.kafka.ContractEventProducer;
import com.contractiq.repository.ContractRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class ContractExpiryBatchConfig {

    private final ContractRepository contractRepository;
    private final ContractAuditService contractAuditService;
    private final ContractEventProducer eventProducer;

    @Bean
    public Job contractExpiryJob(JobRepository jobRepository, Step contractExpiryStep) {
        return new JobBuilder("contractExpiryJob", jobRepository)
                .start(contractExpiryStep)
                .build();
    }

    @Bean
    public Step contractExpiryStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager) {
        return new StepBuilder("contractExpiryStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    List<Contract> expiredContracts=
                            contractRepository.findByStatusAndEndDateBefore(
                                    ContractStatus.ACTIVE,
                                    LocalDate.now()
                            );
                    for(Contract contract:expiredContracts){
                        String oldStatus=contract.getStatus().name();

                        contract.setStatus(ContractStatus.EXPIRED);
                        Contract saved=contractRepository.save(contract);

                        contractAuditService.log(
                                saved.getId().toString(),
                                "CONTRACT_EXPIRED",
                                "SYSTEM",
                                "SYSTEM",
                                oldStatus,
                                saved.getStatus().name(),
                                "Contract expired automatically by batch job",
                                null,
                                null
                        );

                        eventProducer.sendEvent(
                                ContractEventMessage.builder()
                                        .eventId(UUID.randomUUID().toString())
                                        .eventVersion("v1")
                                        .type("CONTRACT_EXPIRED")
                                        .contractId(saved.getId().toString())
                                        .toEmail(contract.getOwner().getEmail())
                                        .message("Your contract'"+saved.getTitle()+"' has expired.")
                                        .build()
                        );
                    }
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;

                }, transactionManager)
                .build();
    }

}
