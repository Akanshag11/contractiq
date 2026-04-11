package com.contractiq.dto.response;

import com.contractiq.domain.workflow.ApprovalStepStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ApprovalStepResponse {
    private UUID id;
    private UUID contractId;
    private String contractTitle;
    private String approverEmail;
    private Integer stepOrder;
    private ApprovalStepStatus status;
    private String remarks;
    private LocalDateTime actedAt;
}