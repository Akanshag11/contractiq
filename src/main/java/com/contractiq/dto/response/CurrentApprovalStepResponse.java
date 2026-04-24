package com.contractiq.dto.response;

import com.contractiq.domain.workflow.ApprovalStepStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CurrentApprovalStepResponse {
    private UUID contractId;
    private String contractTitle;
    private Integer approvalRound;
    private Integer stepOrder;
    private String approverEmail;
    private String approverRole;
    private ApprovalStepStatus status;
    private boolean completed;
    private String message;
    private String contractStatus;
}
