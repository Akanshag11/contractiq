package com.contractiq.service;

import com.contractiq.domain.contract.Contract;
import com.contractiq.domain.contract.ContractEvent;
import com.contractiq.domain.contract.ContractStatus;
import com.contractiq.exception.InvalidContractStateException;
import com.contractiq.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractStateService {

    private final ContractRepository contractRepository;
    private final StateMachineFactory stateMachineFactory;

    public Contract moveState(UUID contractId, ContractEvent event){
        Contract contract=contractRepository.findById(contractId).orElseThrow(() -> new RuntimeException("Contract not found"));
        StateMachine<ContractStatus,ContractEvent> stateMachine = stateMachineFactory.getStateMachine();
        stateMachine.start();

        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(accessor ->
                        accessor.resetStateMachine(
                                new org.springframework.statemachine.support.DefaultStateMachineContext<>(
                                        contract.getStatus(),
                                        null,
                                        null,
                                        null)));

        boolean accepted=stateMachine.sendEvent(MessageBuilder.withPayload(event).build());

        if (!accepted) {
            throw new InvalidContractStateException(
                    "Cannot apply event " + event + " on contract in state " + contract.getStatus()
            );
        }

        contract.setStatus(stateMachine.getState().getId());
        return contractRepository.save(contract);
    }
}
