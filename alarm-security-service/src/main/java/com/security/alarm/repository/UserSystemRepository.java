package com.security.alarm.repository;

import com.security.alarm.entity.UserSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface UserSystemRepository extends JpaRepository<UserSystem, Long> {
    List<UserSystem> findAllByUserId(Long userId);
    
    @Transactional
    void deleteByUserId(Long userId);

    @Transactional
    void deleteBySystemId(Long systemId);
}
