package com.contractiq.service;

import com.contractiq.domain.contract.Contract;
import com.contractiq.domain.contract.ContractStatus;
import com.contractiq.domain.party.Role;
import com.contractiq.domain.party.User;
import com.contractiq.domain.party.UserRepository;
import com.contractiq.dto.request.CreateContractRequest;
import com.contractiq.dto.response.ContractResponse;
import com.contractiq.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.net.Authenticator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final UserRepository userRepository;

    public ContractResponse createContract(CreateContractRequest req, Authentication authentication){
        String email=authentication.getName();

        User owner=userRepository.findByEmail(email).orElseThrow(() ->new RuntimeException("User NOT FOUND"));

        if(owner.getRole() != Role.VENDOR && owner.getRole() != Role.ADMIN){
            throw new RuntimeException("Only VENDOR or ADMIN can create contracts");
        }

        Contract contract =Contract.builder()
                .title(req.getTitle())
                .contractType(req.getContractType())
                .vendorName(req.getVendorName())
                .contractValue(req.getContractValue())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .description(req.getDescription())
                .status(ContractStatus.DRAFT)
                .owner(owner)
                .build();

        Contract saved=contractRepository.save(contract);
        return mapToResponse(saved);

    }

    public List<ContractResponse> getMyContracts(Authentication authentication)
    {
        String email=authentication.getName();
        User owner=userRepository.findByEmail(email).orElseThrow(() ->new RuntimeException("User NOT FOUND"));
        return contractRepository.findByOwnerId(owner.getId()).stream().map(this::mapToResponse).toList();
    }

    private ContractResponse mapToResponse(Contract contract)
    {
        return ContractResponse.builder().
                id(contract.getId()).
                title(contract.getTitle())
                .contractType(contract.getContractType())
                .vendorName(contract.getVendorName())
                .contractValue(contract.getContractValue())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .status(contract.getStatus())
                .description(contract.getDescription())
                .ownerEmail(contract.getOwner().getEmail())
                .createdAt(contract.getCreatedAt())
                .build();
    }

}
