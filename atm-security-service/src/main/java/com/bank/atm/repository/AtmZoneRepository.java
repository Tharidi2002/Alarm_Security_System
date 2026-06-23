package com.bank.atm.repository;

import com.bank.atm.entity.AtmMachine;
import com.bank.atm.entity.AtmZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AtmZoneRepository extends JpaRepository<AtmZone, Long> {
    
    List<AtmZone> findByAtmMachine(AtmMachine atmMachine);
    
    Optional<AtmZone> findByAtmMachineAndZoneNumber(AtmMachine atmMachine, Integer zoneNumber);
    
    List<AtmZone> findByAtmMachineOrderByZoneNumber(AtmMachine atmMachine);
    
    boolean existsByAtmMachineAndZoneNumber(AtmMachine atmMachine, Integer zoneNumber);
}