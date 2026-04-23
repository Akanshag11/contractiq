package com.contractiq.service;

import com.contractiq.domain.contract.Contract;
import com.contractiq.domain.party.Role;
import com.contractiq.domain.party.User;
import com.contractiq.domain.party.UserRepository;
import com.contractiq.domain.workflow.ApprovalStepStatus;
import com.contractiq.domain.workflow.ContractApprovalStep;
import com.contractiq.dto.response.ApprovalStepResponse;
import com.contractiq.dto.response.CurrentApprovalStepResponse;
import com.contractiq.repository.ContractApprovalStepRepository;
import com.contractiq.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        List<ContractApprovalStep> steps = getLatestRoundSteps(contractId);

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

        List<ContractApprovalStep> pendingSteps = approvalStepRepository
                .findByApproverIdAndStatus(user.getId(), ApprovalStepStatus.PENDING);
        Map<UUID, Integer> latestRoundByContract = pendingSteps.stream()
                .map(step -> step.getContract().getId())
                .distinct()
                .collect(Collectors.toMap(contractId -> contractId, this::getLatestApprovalRound));

        List<ContractApprovalStep> steps = pendingSteps.stream()
                .filter(step -> normalizeRound(step.getApprovalRound())
                        .equals(latestRoundByContract.get(step.getContract().getId())))
                .toList();
        return mapToApprovalStepResponse(steps);
    }

    public CurrentApprovalStepResponse getCurrentApprovalStep(UUID contractId,Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("user not found"));
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new RuntimeException("contract not found"));
        Integer currentRound = getLatestApprovalRound(contractId);
        if (currentRound == 0) {
            throw new RuntimeException("No approval steps found for contract");
        }

        List<ContractApprovalStep> steps = approvalStepRepository.findByContractIdAndApprovalRoundOrderByStepOrderAsc(contractId, currentRound);
        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = contract.getOwner().getId().equals(user.getId());
        boolean isApprover = steps.stream().anyMatch(step -> step.getApprover().getId().equals(user.getId()));

        if (!isAdmin && !isOwner && !isApprover) {
            throw new RuntimeException("You are not allowed to view current approval steps for this contract");
        }

        ContractApprovalStep currentStep = steps.stream().filter(step -> step.getStatus() == ApprovalStepStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No pending approval steps found for contract"));

        return CurrentApprovalStepResponse.builder()
                .contractId(contract.getId())
                .contractTitle(contract.getTitle())
                .approvalRound(currentRound)
                .stepOrder(currentStep.getStepOrder())
                .approverEmail(currentStep.getApprover().getEmail())
                .approverRole(currentStep.getApprover().getRole().name())
                .status(currentStep.getStatus())
                .build();
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

    private List<ContractApprovalStep> getLatestRoundSteps(UUID contractId) {
        int latestRound = getLatestApprovalRound(contractId);
        if (latestRound == 0) {
            return List.of();
        }
        return approvalStepRepository.findByContractIdAndApprovalRoundOrderByStepOrderAsc(contractId, latestRound);
    }

    private Integer getLatestApprovalRound(UUID contractId) {
        Integer maxRound = approvalStepRepository.findMaxApprovalRoundByContractId(contractId);
        if (maxRound == null) {
            return 0;
        }
        return maxRound;
    }

    private Integer normalizeRound(Integer round) {
        if (round == null) {
            return 1;
        }
        return round;
    }
}
