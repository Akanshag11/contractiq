package com.contractiq.service;

import com.contractiq.domain.party.User;
import com.contractiq.domain.party.UserRepository;
import com.contractiq.domain.workflow.ApprovalStepStatus;
import com.contractiq.dto.response.ApprovalStepResponse;
import com.contractiq.repository.ContractApprovalStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalQueryService {

    private final ContractApprovalStepRepository approvalStepRepository;
    private final UserRepository userRepository;

    public List<ApprovalStepResponse> getApprovalStepsForContract(UUID contractId) {
        return approvalStepRepository.findByContractIdOrderByStepOrderAsc(contractId)
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

    public List<ApprovalStepResponse> getMyPendingApprovals(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return approvalStepRepository.findByApproverIdAndStatus(user.getId(), ApprovalStepStatus.PENDING)
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