package com.akademiaplus.program.interfaceadapters;

import com.akademiaplus.courses.program.ScheduleDataModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleDataModel,  Integer> {
}
