package com.contractiq.service;

import com.contractiq.domain.contract.Contract;
import com.contractiq.domain.party.Role;
import com.contractiq.domain.party.User;
import com.contractiq.domain.party.UserRepository;
import com.contractiq.domain.workflow.ApprovalStepStatus;
import com.contractiq.domain.workflow.ContractApprovalStep;
import com.contractiq.dto.notification.NotificationMessage;
import com.contractiq.kafka.ContractEventMessage;
import com.contractiq.kafka.ContractEventProducer;
import com.contractiq.repository.ContractApprovalStepRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalWorkflowService {

    private final UserRepository userRepository;
    private final ContractApprovalStepRepository approvalStepRepository;
    private final ContractAuditService contractAuditService;
    private final NotificationService notificationService;
    private final ContractEventProducer eventProducer;

    public void createInitialApprovalSteps(Contract contract){
        int nextRound = getLatestApprovalRound(contract.getId()) + 1;

        User legalManager = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.LEGAL_MGR).findFirst().orElseThrow(() -> new RuntimeException("Legal Manager not found"));

        User finance = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.FINANCE).findFirst().orElseThrow(() -> new RuntimeException("Finance not found"));

        User admin = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN).findFirst().orElseThrow(() -> new RuntimeException("Admin not found"));

        List<ContractApprovalStep> steps =List.of(
                ContractApprovalStep.builder()
                        .contract(contract)
                        .approver(legalManager)
                        .stepOrder(1)
                        .approvalRound(nextRound)
                        .status(ApprovalStepStatus.PENDING)
                        .build(),

                ContractApprovalStep.builder().
                        contract(contract).
                        approver(finance)
                        .stepOrder(2)
                        .approvalRound(nextRound)
                        .status(ApprovalStepStatus.PENDING)
                        .build(),

                ContractApprovalStep.builder()
                        .contract(contract)
                        .approver(admin)
                        .stepOrder(3)
                        .approvalRound(nextRound)
                        .status(ApprovalStepStatus.PENDING)
                        .build()
        );


        approvalStepRepository.saveAll(steps);

        ContractApprovalStep firstStep = steps.stream()
                .filter(step -> step.getStepOrder() == 1)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("First approval step not found"));

        eventProducer.sendEvent(
                ContractEventMessage.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventVersion("v1")
                        .type("CONTRACT_SUBMITTED")
                        .contractId(contract.getId().toString())
                        .toEmail(firstStep.getApprover().getEmail())
                        .message("Contract '" + contract.getTitle() + "' is waiting for your approval.")
                        .build()
        );
    }

    public boolean approveCurrentStep(Contract contract, User actor)
    {
        int currentRound = getLatestApprovalRound(contract.getId());
        List<ContractApprovalStep> steps = approvalStepRepository
                .findByContractIdAndApprovalRoundOrderByStepOrderAsc(contract.getId(), currentRound);

        ContractApprovalStep currentStep=steps.stream().filter(step -> step.getStatus() == ApprovalStepStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No pending step found"));

        if(!currentStep.getApprover().getId().equals(actor.getId()))
        {
            throw new RuntimeException("You are not assigned to this approval step");
        }

        currentStep.setStatus(ApprovalStepStatus.APPROVED);
        currentStep.setActedAt(LocalDateTime.now());
        approvalStepRepository.save(currentStep);
        ContractApprovalStep nextPendingStep = steps.stream()
                .filter(step -> step.getStatus() == ApprovalStepStatus.PENDING)
                .findFirst()
                .orElse(null);

        if (nextPendingStep != null) {
            eventProducer.sendEvent(
                    ContractEventMessage.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventVersion("v1")
                            .type("NEXT_APPROVER")
                            .contractId(contract.getId().toString())
                            .toEmail(nextPendingStep.getApprover().getEmail())
                            .message("Contract '" + contract.getTitle() + "' is waiting for your approval.")
                            .build()
            );
        }
        contractAuditService.log(
                contract.getId().toString(),
                "APPROVAL_STEP_APPROVED",
                actor.getEmail(),
                actor.getRole().name(),
                contract.getStatus().name(),
                contract.getStatus().name(),
                "Approval step approved",
                currentRound,
                currentStep.getStepOrder()
        );



        boolean allApproved = steps.stream().allMatch(step -> step.getStatus() == ApprovalStepStatus.APPROVED);
        if (allApproved) {
            eventProducer.sendEvent(
                    ContractEventMessage.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventVersion("v1")
                            .type("CONTRACT_APPROVED")
                            .contractId(contract.getId().toString())
                            .toEmail(contract.getOwner().getEmail())
                            .message("Your contract '" + contract.getTitle() + "' is fully approved.")
                            .build()
            );
        }
        return allApproved;
    }

    public void rejectCurrentStep(Contract contract, User actor, String remarks)
    {
        int currentRound = getLatestApprovalRound(contract.getId());
        List<ContractApprovalStep> steps = approvalStepRepository
                .findByContractIdAndApprovalRoundOrderByStepOrderAsc(contract.getId(), currentRound);

        ContractApprovalStep currentStep=steps.stream()
                .filter(step -> step.getStatus() == ApprovalStepStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No pending step found"));

        if(!currentStep.getApprover().getId().equals(actor.getId()))
        {
            throw new RuntimeException("You are not assigned to this approval step");
        }

        LocalDateTime actedAt = LocalDateTime.now();
        for (ContractApprovalStep step : steps) {
            if (step.getStatus() != ApprovalStepStatus.PENDING) {
                continue;
            }
            step.setStatus(ApprovalStepStatus.REJECTED);
            step.setActedAt(actedAt);
            if (step.getId().equals(currentStep.getId())) {
                step.setRemarks(remarks);
            } else {
                step.setRemarks("Rejected in the same approval round");
            }
        }
        contractAuditService.log(
                contract.getId().toString(),
                "APPROVAL_STEP_REJECTED",
                actor.getEmail(),
                actor.getRole().name(),
                contract.getStatus().name(),
                contract.getStatus().name(),
                remarks,
                currentRound,
                currentStep.getStepOrder()
        );
        approvalStepRepository.saveAll(steps);

        eventProducer.sendEvent(
                ContractEventMessage.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventVersion("v1")
                        .type("CONTRACT_REJECTED")
                        .contractId(contract.getId().toString())
                        .toEmail(contract.getOwner().getEmail())
                        .message("Your contract '" + contract.getTitle() + "' was rejected.")
                        .build()
        );
    }

    public List<ContractApprovalStep> getContractApprovalSteps(UUID contractId)
    {
        int currentRound = getLatestApprovalRound(contractId);
        return approvalStepRepository.findByContractIdAndApprovalRoundOrderByStepOrderAsc(contractId, currentRound);
    }

    private int getLatestApprovalRound(UUID contractId) {
        Integer maxRound = approvalStepRepository.findMaxApprovalRoundByContractId(contractId);
        if (maxRound == null) {
            return 0;
        }
        return maxRound;
    }
}
