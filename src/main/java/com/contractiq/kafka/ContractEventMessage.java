package com.contractiq.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractEventMessage {
    private String type;
    private String contractId;
    private String toEmail;
    private String message;
}
