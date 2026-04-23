package com.contractiq.controller;

import com.contractiq.dto.response.ContractAuditLogResponse;
import com.contractiq.service.ContractAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractAuditController {

    private final ContractAuditService contractAuditService;

    @GetMapping("/{id}/audit-logs")
    public List<ContractAuditLogResponse> getAuditLogs(@PathVariable String id) {
        return contractAuditService.getLogsForContract(id);
    }
}
