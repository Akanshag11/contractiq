package com.contractiq.service;

import com.contractiq.domain.contract.Contract;
import com.contractiq.domain.contract.ContractEvent;
import com.contractiq.domain.contract.ContractStatus;
import com.contractiq.domain.party.Role;
import com.contractiq.domain.party.User;
import com.contractiq.domain.party.UserRepository;
import com.contractiq.exception.InvalidContractStateException;
import com.contractiq.repository.ContractRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractStateService {

    private final ContractRepository contractRepository;
    private final StateMachineFactory stateMachineFactory;
    private final UserRepository userRepository;
    private final ApprovalWorkflowService approvalWorkflowService;

    @Transactional
    public Contract moveState(UUID contractId, ContractEvent event, Authentication authentication){
        Contract contract=contractRepository.findById(contractId).orElseThrow(() -> new RuntimeException("Contract not found"));
        User actor= userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
        validateRoleAccess(contract,event,actor);

        if (event == ContractEvent.APPROVE) {
            approvalWorkflowService.approveCurrentStep(contract, actor);
        }

        if (event == ContractEvent.REJECT) {
            approvalWorkflowService.rejectCurrentStep(contract, actor, "Rejected by legal manager");
        }

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
        Contract saved=contractRepository.save(contract);
        if(event== ContractEvent.SUBMIT_FOR_REVIEW)
        {
            approvalWorkflowService.createInitialApprovalSteps(saved);
        }
        return saved;
    }

    private void validateRoleAccess(Contract contract,ContractEvent event, User actor){
        Role role=actor.getRole();

        switch(event){
            case SUBMIT_FOR_REVIEW -> {
                if(role!=Role.VENDOR && role!=Role.ADMIN) {
                    throw new InvalidContractStateException("Only VENDOR or ADMIN authorized to submit contract for review.");
                }

                if(role == Role.VENDOR && !contract.getOwner().getId().equals(actor.getId()))
                {
                    throw new InvalidContractStateException("Vendor can only submit their own contracts");
                }
            }

            case APPROVE,REJECT -> {
                if(role!=Role.LEGAL_MGR) {
                    throw new InvalidContractStateException("Only LEGAL_MGR authorized to approve OR REJECT contract.");
                }
            }

            case SIGN, ACTIVATE, TERMINATE -> {
                if(role!=Role.ADMIN) {
                    throw new InvalidContractStateException("Only ADMIN authorized to sign, activate, or terminate contract.");
                }
            }

            default -> {
                throw new InvalidContractStateException("Unsupported contract event: " + event);
            }
        }
    }
}
