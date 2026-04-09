package com.contractiq.dto.response;

import com.contractiq.domain.contract.ContractStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ContractResponse {
    private UUID id;
    private String title;
    private String contractType;
    private String vendorName;
    private BigDecimal contractValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private ContractStatus status;
    private String description;
    private String ownerEmail;
    private LocalDateTime createdAt;
}
