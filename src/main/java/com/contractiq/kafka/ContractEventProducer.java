package com.contractiq.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContractEventProducer {

    private final KafkaTemplate<String, ContractEventMessage> kafkaTemplate;

    public void sendEvent(ContractEventMessage event) {
        kafkaTemplate.send("contract-events", event);
    }
}
