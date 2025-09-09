package com.makani.internal.interfaceadapters;

import com.makani.security.user.InternalAuthDataModel;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InternalAuthRepository extends JpaRepository<InternalAuthDataModel, Integer> {
    Optional<InternalAuthDataModel> findByUsername(String username);

    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE internal_auth AUTO_INCREMENT = 1", nativeQuery = true)
    void restIdCounter();
}
