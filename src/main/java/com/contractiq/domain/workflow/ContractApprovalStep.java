package com.contractiq.domain.workflow;

import com.contractiq.domain.contract.Contract;
import com.contractiq.domain.party.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="contract_approval_steps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractApprovalStep {

    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="approver_id", nullable = false)
    private User approver;

    @Column(nullable = false)
    private Integer stepOrder;

    @Builder.Default
    @Column(name = "approval_round", nullable = false)
    private Integer approvalRound = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApprovalStepStatus status=ApprovalStepStatus.PENDING;

    @Column(length =500)
    private String remarks;

    private LocalDateTime actedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
