package com.contractiq.repository;

import com.contractiq.domain.audit.ContractAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ContractAuditLogRepository extends MongoRepository<ContractAuditLog, String> {
    List<ContractAuditLog> findByContractIdOrderByTimestampDesc(String contractId);

}
