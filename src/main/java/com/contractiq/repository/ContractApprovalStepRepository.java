package com.contractiq.repository;

import com.contractiq.domain.workflow.ApprovalStepStatus;
import com.contractiq.domain.workflow.ContractApprovalStep;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface ContractApprovalStepRepository extends JpaRepository<ContractApprovalStep, UUID> {
    @EntityGraph(attributePaths = {"contract", "approver"})
    List<ContractApprovalStep> findByContractIdOrderByStepOrderAsc(UUID contractId);
    Optional<ContractApprovalStep>findFirstByContractIdAndStatusOrderByStepOrderAsc(
            UUID contractId,
            ApprovalStepStatus approvalStepStatus
    );

    List<ContractApprovalStep> findByApproverId(UUID approverId);
    @EntityGraph(attributePaths = {"contract", "approver"})
    List<ContractApprovalStep> findByApproverIdAndStatus(UUID approverId, ApprovalStepStatus status);

    @EntityGraph(attributePaths = {"contract", "approver"})
    @Query("""
            select s
            from ContractApprovalStep s
            where s.contract.id = :contractId
              and coalesce(s.approvalRound, 1) = :approvalRound
            order by s.stepOrder asc
            """)
    List<ContractApprovalStep> findByContractIdAndApprovalRoundOrderByStepOrderAsc(
            @Param("contractId") UUID contractId,
            @Param("approvalRound") Integer approvalRound
    );

    @Query("""
            select coalesce(max(coalesce(s.approvalRound, 1)), 0)
            from ContractApprovalStep s
            where s.contract.id = :contractId
            """)
    Integer findMaxApprovalRoundByContractId(@Param("contractId") UUID contractId);

    @EntityGraph(attributePaths = {"contract", "approver"})
    @Query("""
        select s
        from ContractApprovalStep s
        where s.contract.id = :contractId
          and coalesce(s.approvalRound, 1) = :approvalRound
          and s.status = com.contractiq.domain.workflow.ApprovalStepStatus.PENDING
        order by s.stepOrder asc
        """)
    List<ContractApprovalStep> findPendingStepsByContractIdAndApprovalRound(
            @Param("contractId") UUID contractId,
            @Param("approvalRound") Integer approvalRound
    );

    List<ContractApprovalStep> findByStatusAndCreatedAtBefore(
            ApprovalStepStatus status,
            LocalDateTime cutoff
    );
}
