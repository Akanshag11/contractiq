package com.contractiq.service;

import com.contractiq.domain.contract.Contract;
import com.contractiq.domain.party.Role;
import com.contractiq.domain.party.User;
import com.contractiq.domain.party.UserRepository;
import com.contractiq.domain.workflow.ApprovalStepStatus;
import com.contractiq.domain.workflow.ContractApprovalStep;
import com.contractiq.repository.ContractApprovalStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService {

    private final UserRepository userRepository;
    private final ContractApprovalStepRepository approvalStepRepository;
    public void createInitialApprovalSteps(Contract contract){
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
                        .status(ApprovalStepStatus.PENDING)
                        .build(),

                ContractApprovalStep.builder().
                        contract(contract).
                        approver(finance)
                        .stepOrder(2)
                        .status(ApprovalStepStatus.PENDING)
                        .build(),

                ContractApprovalStep.builder()
                        .contract(contract)
                        .approver(admin)
                        .stepOrder(3)
                        .status(ApprovalStepStatus.PENDING)
                        .build()
        );


        approvalStepRepository.saveAll(steps);
    }

    public boolean approveCurrentStep(Contract contract, User actor)
    {
        List<ContractApprovalStep> steps = approvalStepRepository.findByContractIdOrderByStepOrderAsc(contract.getId());

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

        boolean allApproved = steps.stream().allMatch(step -> step.getStatus() == ApprovalStepStatus.APPROVED);
        return allApproved;
    }

    public void rejectCurrentStep(Contract contract, User actor, String remarks)
    {
        ContractApprovalStep currentStep=approvalStepRepository
                .findFirstByContractIdAndStatusOrderByStepOrderAsc(
                        contract.getId(), ApprovalStepStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("No pending step found"));

        if(!currentStep.getApprover().getId().equals(actor.getId()))
        {
            throw new RuntimeException("You are not assigned to this approval step");
        }

        currentStep.setStatus(ApprovalStepStatus.REJECTED);
        currentStep.setRemarks(remarks);
        currentStep.setActedAt(LocalDateTime.now());
        approvalStepRepository.save(currentStep);
    }

    public List<ContractApprovalStep> getContractApprovalSteps(UUID contractId)
    {
        return approvalStepRepository.findByContractIdOrderByStepOrderAsc(contractId);
    }
}
