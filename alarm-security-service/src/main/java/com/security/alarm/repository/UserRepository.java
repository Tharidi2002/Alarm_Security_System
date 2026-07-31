package com.security.alarm.repository;

import com.security.alarm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByRole(String role);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'ADMIN'")
    long countAdmins();
    
    @Query("SELECT u FROM User u WHERE u.role = 'ADMIN' ORDER BY u.id ASC")
    List<User> findAllAdmins();
    
    Optional<User> findFirstByRoleOrderByIdAsc(String role);
}