package com.bank.atm.repository;

import com.bank.atm.entity.AtmMachine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AtmMachineRepository extends JpaRepository<AtmMachine, Long> {
    Optional<AtmMachine> findBySimNumber(String simNumber);
}