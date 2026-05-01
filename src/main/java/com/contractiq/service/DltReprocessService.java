package com.contractiq.service;

import com.contractiq.kafka.ContractEventMessage;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class DltReprocessService {
    private static final String DLT_TOPIC = "contract-events-dlt";
    private static final String MAIN_TOPIC = "contract-events";

    private final KafkaTemplate<String, ContractEventMessage> kafkaTemplate;
    private final ConsumerFactory<String, ContractEventMessage> consumerFactory;

    public int reprocessDltMessages() {
        int reprocessedCount = 0;
        Consumer<String, ContractEventMessage> consumer=consumerFactory.createConsumer("dlt-reprocessor-group", "dlt-reprocessor-client");
        consumer.subscribe(Collections.singletonList(DLT_TOPIC));
        ConsumerRecords<String, ContractEventMessage> records=consumer.poll(Duration.ofSeconds(5));
        for(ConsumerRecord<String, ContractEventMessage> record: records){
            ContractEventMessage event= record.value();
            kafkaTemplate.send(MAIN_TOPIC, event.getEventId(), event);
            reprocessedCount++;
        }

        consumer.commitSync();
        consumer.close();
        return reprocessedCount;
    }
}
