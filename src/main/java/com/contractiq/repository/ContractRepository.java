package com.contractiq.repository;

import com.contractiq.domain.contract.Contract;
import com.contractiq.domain.contract.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;


public interface ContractRepository extends JpaRepository<Contract, UUID> {

    List<Contract> findOwnerById(UUID ownerId);

    List<Contract> findByStatus(ContractStatus status);

    List<Contract> findByOwnerIdAndStatus(UUID ownerId,ContractStatus status);



}
