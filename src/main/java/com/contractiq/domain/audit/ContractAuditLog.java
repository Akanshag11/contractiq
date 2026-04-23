package com.contractiq.domain.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "contract_audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractAuditLog {

    @Id
    private String id;
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
