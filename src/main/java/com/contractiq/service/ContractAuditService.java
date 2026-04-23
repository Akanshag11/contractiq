package com.contractiq.service;

import com.contractiq.domain.audit.ContractAuditLog;
import com.contractiq.dto.response.ContractAuditLogResponse;
import com.contractiq.repository.ContractAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractAuditService {
    private final ContractAuditLogRepository auditLogRepository;
    public void log(String contractId,
                    String action,
                    String actorEmail,
                    String actorRole,
                    String oldStatus,
                    String newStatus,
                    String remarks,
                    Integer approvalRound,
                    Integer stepOrder) {

        ContractAuditLog log = ContractAuditLog.builder()
                .contractId(contractId)
                .action(action)
                .actorEmail(actorEmail)
                .actorRole(actorRole)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .remarks(remarks)
                .approvalRound(approvalRound)
                .stepOrder(stepOrder)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

   public List<ContractAuditLogResponse> getLogsForContract(String contractId) {
        return auditLogRepository.findByContractIdOrderByTimestampDesc(contractId)
                .stream()
                .map(log -> ContractAuditLogResponse.builder()
                        .contractId(log.getContractId())
                        .action(log.getAction())
                        .actorEmail(log.getActorEmail())
                        .actorRole(log.getActorRole())
                        .oldStatus(log.getOldStatus())
                        .newStatus(log.getNewStatus())
                        .remarks(log.getRemarks())
                        .approvalRound(log.getApprovalRound())
                        .stepOrder(log.getStepOrder())
                        .timestamp(log.getTimestamp())
                        .build())
                .toList();
    }

}
