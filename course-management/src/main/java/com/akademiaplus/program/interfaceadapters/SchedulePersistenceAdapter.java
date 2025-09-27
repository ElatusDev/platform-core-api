package com.akademiaplus.program.interfaceadapters;


import com.akademiaplus.courses.program.ScheduleDataModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SchedulePersistenceAdapter implements SchedulePersistenceOutputPort {

    private final ScheduleRepository scheduleRepository;

    public SchedulePersistenceAdapter(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public Optional<ScheduleDataModel> findById(Integer id) {
        return scheduleRepository.findById(id);
    }

    @Override
    public List<ScheduleDataModel> findAllById(List<Integer> ids) {
        return scheduleRepository.findAllById(ids);
    }

    @Override
    public void update(List<ScheduleDataModel> schedules) {
         scheduleRepository.saveAll(schedules);
    }

    public void set() {

    }
}