package com.contractiq.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectApprovalRequest {

    @NotBlank
    private String remarks;
}