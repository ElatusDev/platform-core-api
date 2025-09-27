/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Date;
import java.time.LocalDate;

@Configuration
public class MapperModelConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        addLocalDateToSqlDateConverter(modelMapper);
        addSqlDateToLocalDateConverter(modelMapper);

        return modelMapper;
    }

    private void addLocalDateToSqlDateConverter(ModelMapper modelMapper) {
        Converter<LocalDate, Date> localDateToSqlDate = mappingContext ->
                mappingContext.getSource() == null ? null : Date.valueOf(mappingContext.getSource());
        modelMapper.addConverter(localDateToSqlDate, LocalDate.class, Date.class);
    }

    private void addSqlDateToLocalDateConverter(ModelMapper modelMapper) {
        Converter<Date, LocalDate> sqlDateToLocalDate = mappingContext ->
                mappingContext.getSource() == null ? null : mappingContext.getSource().toLocalDate();
        modelMapper.addConverter(sqlDateToLocalDate, Date.class, LocalDate.class);
    }
}
