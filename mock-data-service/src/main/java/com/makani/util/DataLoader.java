package com.makani.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.makani.exceptions.FailToGenerateMockDataException;
import jakarta.transaction.Transactional;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

@Component
@Scope("prototype")
public class DataLoader<D, M, I> {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    @Setter
    private String location;
    @Setter
    private JpaRepository<M, I> repository;
    @Setter
    private Function<D, M> transformer;
    @Setter
    private Class<D> dtoClass;

    public DataLoader(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void load() {
        try {
            String jsonMockData = new String(resourceLoader.getResource(location)
                    .getInputStream()
                    .readAllBytes(), StandardCharsets.UTF_8);
            JavaType listType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, dtoClass);
            List<D> dtos = objectMapper.readValue(jsonMockData, listType);
            List<M> employeeDataModels = dtos.stream().map(transformer).toList();
            repository.saveAll(employeeDataModels);
        } catch (Exception e) {
            throw new FailToGenerateMockDataException(e);
        }
    }


}
