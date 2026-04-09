package com.contractiq.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateContractRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String contractType;

    @NotBlank
    private String vendorName;

    @NotNull
    @DecimalMin(value="0.0", inclusive = false)
    private BigDecimal contractValue;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private String description;
}
