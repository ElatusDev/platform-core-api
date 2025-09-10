package com.makani.people.usecases;

import com.makani.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.makani.customer.adultstudent.usecases.AdultStudentCreationUseCase;
import com.makani.people.customer.AdultStudentDataModel;
import com.makani.util.AbstractLoadMockData;
import com.makani.util.DataCleanUp;
import com.makani.util.DataLoader;
import openapi.makani.domain.people.dto.AdultStudentCreationRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoadAdultStudentMockDataUseCase extends AbstractLoadMockData<AdultStudentCreationRequestDTO, AdultStudentDataModel, Integer> {

    public LoadAdultStudentMockDataUseCase(@Value("${adult.student.mock.data.location}") String adultStudentMockDataLocation,
                                           AdultStudentCreationUseCase adultStudentCreationUseCase,
                                           AdultStudentRepository adultStudentRepository,
                                           DataLoader<AdultStudentCreationRequestDTO, AdultStudentDataModel, Integer> dataLoader,
                                           DataCleanUp<AdultStudentDataModel, Integer> dataCleanUp) {
        super(adultStudentMockDataLocation, adultStudentCreationUseCase::transform, AdultStudentCreationRequestDTO.class,
                adultStudentRepository, AdultStudentDataModel.class, dataLoader, dataCleanUp);
    }

}
