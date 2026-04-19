package com.contractiq.service;

import com.contractiq.domain.contract.Contract;
import com.contractiq.domain.party.Role;
import com.contractiq.domain.party.User;
import com.contractiq.domain.party.UserRepository;
import com.contractiq.domain.workflow.ApprovalStepStatus;
import com.contractiq.domain.workflow.ContractApprovalStep;
import com.contractiq.dto.response.ApprovalStepResponse;
import com.contractiq.repository.ContractApprovalStepRepository;
import com.contractiq.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalQueryService {

    private final ContractApprovalStepRepository approvalStepRepository;
    private final UserRepository userRepository;
    private final ContractRepository contractRepository;

    public List<ApprovalStepResponse> getApprovalStepsForContract(UUID contractId, Authentication authentication) {

        User user= userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));

        Contract contract=contractRepository.findById(contractId).orElseThrow(() -> new RuntimeException("Contract not found"));
        List<ContractApprovalStep> steps = approvalStepRepository.findByContractIdOrderByStepOrderAsc(contractId);

        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = contract.getOwner().getId().equals(user.getId());

        boolean isApprover = steps
                .stream()
                .anyMatch(step -> step.getApprover().getId().equals(user.getId()));

        if (!isAdmin && !isOwner && !isApprover) {
            throw new RuntimeException("You are not allowed to view approval steps for this contract");
        }
        return mapToApprovalStepResponse(steps);
    }

    public List<ApprovalStepResponse> getMyPendingApprovals(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ContractApprovalStep> steps = approvalStepRepository.findByApproverIdAndStatus(user.getId(), ApprovalStepStatus.PENDING);
        return mapToApprovalStepResponse(steps);
    }

    private List<ApprovalStepResponse> mapToApprovalStepResponse(List<ContractApprovalStep> steps) {
        return steps
                .stream()
                .map(step -> ApprovalStepResponse.builder()
                        .id(step.getId())
                        .contractId(step.getContract().getId())
                        .contractTitle(step.getContract().getTitle())
                        .approverEmail(step.getApprover().getEmail())
                        .stepOrder(step.getStepOrder())
                        .status(step.getStatus())
                        .remarks(step.getRemarks())
                        .actedAt(step.getActedAt())
                        .build())
                .toList();
    }
}
