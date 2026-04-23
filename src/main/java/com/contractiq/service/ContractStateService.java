package com.contractiq.service;

import com.contractiq.domain.contract.Contract;
import com.contractiq.domain.contract.ContractEvent;
import com.contractiq.domain.contract.ContractStatus;
import com.contractiq.domain.party.Role;
import com.contractiq.domain.party.User;
import com.contractiq.domain.party.UserRepository;
import com.contractiq.exception.InvalidContractStateException;
import com.contractiq.repository.ContractRepository;
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
    private final ContractAuditService contractAuditService;

    @Transactional
    public Contract moveState(UUID contractId, ContractEvent event, Authentication authentication){
        Contract contract=contractRepository.findById(contractId).orElseThrow(() -> new RuntimeException("Contract not found"));
        String oldStatus = contract.getStatus().name();
        User actor= userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
        validateRoleAccess(contract,event,actor);

        if (event == ContractEvent.APPROVE) {
            boolean allApproved=approvalWorkflowService.approveCurrentStep(contract, actor);
            if(!allApproved){
                return contract;
            }
            contract.setStatus(ContractStatus.APPROVED);
            Contract saved = contractRepository.save(contract);

            contractAuditService.log(
                    saved.getId().toString(),
                    "CONTRACT_APPROVED",
                    actor.getEmail(),
                    actor.getRole().name(),
                    oldStatus,
                    saved.getStatus().name(),
                    "All approval steps completed",
                    null,
                    null
            );

            return saved;
        }

        if (event == ContractEvent.REJECT) {
            approvalWorkflowService.rejectCurrentStep(contract, actor, "Rejected by approver");
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
            contractAuditService.log(
                    saved.getId().toString(),
                    "CONTRACT_SUBMITTED",
                    actor.getEmail(),
                    actor.getRole().name(),
                    oldStatus,
                    saved.getStatus().name(),
                    "Contract submitted for review",
                    null,
                    null
            );
        }
        if (event == ContractEvent.REJECT) {
            contractAuditService.log(
                    saved.getId().toString(),
                    "CONTRACT_REJECTED",
                    actor.getEmail(),
                    actor.getRole().name(),
                    oldStatus,
                    saved.getStatus().name(),
                    "Contract rejected in approval workflow",
                    null,
                    null
            );
        }

        if (event == ContractEvent.SIGN) {
            contractAuditService.log(
                    saved.getId().toString(),
                    "CONTRACT_SIGNED",
                    actor.getEmail(),
                    actor.getRole().name(),
                    oldStatus,
                    saved.getStatus().name(),
                    "Contract signed",
                    null,
                    null
            );
        }

        if (event == ContractEvent.ACTIVATE) {
            contractAuditService.log(
                    saved.getId().toString(),
                    "CONTRACT_ACTIVATED",
                    actor.getEmail(),
                    actor.getRole().name(),
                    oldStatus,
                    saved.getStatus().name(),
                    "Contract activated",
                    null,
                    null
            );
        }

        if (event == ContractEvent.TERMINATE) {
            contractAuditService.log(
                    saved.getId().toString(),
                    "CONTRACT_TERMINATED",
                    actor.getEmail(),
                    actor.getRole().name(),
                    oldStatus,
                    saved.getStatus().name(),
                    "Contract terminated",
                    null,
                    null
            );
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
                if(role == Role.VENDOR) {
                    throw new InvalidContractStateException("Vendor cannot approve or reject contracts");
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
