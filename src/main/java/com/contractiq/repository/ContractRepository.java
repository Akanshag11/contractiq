package com.contractiq.repository;

import com.contractiq.domain.contract.Contract;
import com.contractiq.domain.contract.ContractStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


public interface ContractRepository extends JpaRepository<Contract, UUID> {

    @Override
    @EntityGraph(attributePaths = {"owner"})
    java.util.Optional<Contract> findById(UUID id);

    @EntityGraph(attributePaths = {"owner"})
    List<Contract> findByOwnerId(UUID ownerId);

    List<Contract> findByStatus(ContractStatus status);

    @EntityGraph(attributePaths = {"owner"})
    List<Contract> findByOwnerIdAndStatus(UUID ownerId,ContractStatus status);

    List<Contract> findByStatusAndEndDateBefore(ContractStatus status, LocalDate endDate);



}
