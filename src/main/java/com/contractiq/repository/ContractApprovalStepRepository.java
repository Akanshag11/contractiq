package com.contractiq.repository;

import com.contractiq.domain.workflow.ApprovalStepStatus;
import com.contractiq.domain.workflow.ContractApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface ContractApprovalStepRepository extends JpaRepository<ContractApprovalStep, UUID> {
    List<ContractApprovalStep> findByContractIdOrderByStepOrderAsc(UUID contractId);
    Optional<ContractApprovalStep>findFirstByContractIdAndStatusOrderByStepOrderAsc(
            UUID contractId,
            ApprovalStepStatus approvalStepStatus
    );

    List<ContractApprovalStep> findByApproverId(UUID approverId);
    List<ContractApprovalStep> findByApproverIdAndStatus(UUID approverId, ApprovalStepStatus status);
}
