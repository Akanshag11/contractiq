package com.contractiq.controller;

import com.contractiq.domain.contract.ContractEvent;
import com.contractiq.dto.request.CreateContractRequest;
import com.contractiq.dto.response.ContractResponse;
import com.contractiq.service.ContractService;
import com.contractiq.service.ContractStateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {
    private final ContractService contractService;
    private final ContractStateService contractStateService;

    @PostMapping
    public ContractResponse createContract(@Valid @RequestBody CreateContractRequest req, Authentication authentication) {
        return contractService.createContract(req, authentication);
    }

    @GetMapping("/my")
    public List<ContractResponse> getMyContact(Authentication authentication)
    {
        return contractService.getMyContracts(authentication);
    }

    @PostMapping("/{id}/submit")
    public String submitForReview(@PathVariable UUID id) {
        contractStateService.moveState(id, ContractEvent.SUBMIT_FOR_REVIEW);
        return "Contract submitted for review";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable UUID id){
        contractStateService.moveState(id, ContractEvent.APPROVE);
        return "Contract submitted for approve";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable UUID id) {
        contractStateService.moveState(id, ContractEvent.REJECT);
        return "Contract moved back to DRAFT";
    }

    @PostMapping("/{id}/sign")
    public String sign(@PathVariable UUID id) {
        contractStateService.moveState(id, ContractEvent.SIGN);
        return "Contract moved to SIGNED";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable UUID id) {
        contractStateService.moveState(id, ContractEvent.ACTIVATE);
        return "Contract moved to ACTIVE";
    }

    @PostMapping("/{id}/terminate")
    public String terminate(@PathVariable UUID id) {
        contractStateService.moveState(id, ContractEvent.TERMINATE);
        return "Contract moved to TERMINATED";
    }
}
