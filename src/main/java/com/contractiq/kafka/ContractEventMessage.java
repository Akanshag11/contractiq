package com.contractiq.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractEventMessage {
    @Indexed(unique = true)
    private String eventId;
    private String type;
    private String contractId;
    private String toEmail;
    private String message;
}
