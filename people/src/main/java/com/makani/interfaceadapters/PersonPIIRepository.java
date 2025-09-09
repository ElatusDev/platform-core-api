package com.makani.interfaceadapters;

import com.makani.PersonPIIDataModel;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PersonPIIRepository extends JpaRepository<PersonPIIDataModel, Integer> {

    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE person_pii AUTO_INCREMENT = 1", nativeQuery = true)
    void restIdCounter();
}
