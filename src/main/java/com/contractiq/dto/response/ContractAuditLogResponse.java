package com.contractiq.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ContractAuditLogResponse {
    private String contractId;
    private String action;
    private String actorEmail;
    private String actorRole;
    private String oldStatus;
    private String newStatus;
    private String remarks;
    private Integer approvalRound;
    private Integer stepOrder;
    private LocalDateTime timestamp;
}
