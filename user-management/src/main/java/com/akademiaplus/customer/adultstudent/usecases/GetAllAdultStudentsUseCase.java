package com.akademiaplus.customer.adultstudent.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import openapi.akademiaplus.domain.user.management.dto.GetAdultStudentResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetAllAdultStudentsUseCase {
    private final AdultStudentRepository adultStudentRepository;
    private final ModelMapper modelMapper;

    public GetAllAdultStudentsUseCase(AdultStudentRepository adultStudentRepository,
                                      ModelMapper modelMapper) {
        this.adultStudentRepository = adultStudentRepository;
        this.modelMapper = modelMapper;
    }

    public List<GetAdultStudentResponseDTO> getAll() {
        return adultStudentRepository.findAll() .stream()
                .map(dataModel -> {
                    GetAdultStudentResponseDTO dto =  modelMapper.map(dataModel, GetAdultStudentResponseDTO.class);
                    modelMapper.map(dataModel.getPersonPII(), dto);
                    return dto;
                })
                .toList();
    }
}
