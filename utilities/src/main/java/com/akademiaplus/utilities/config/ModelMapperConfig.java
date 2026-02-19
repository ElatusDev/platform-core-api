/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.config;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Centralized {@link ModelMapper} configuration for all modules.
 * <p>
 * Registers type converters that bridge mismatches between OpenAPI-generated
 * DTOs and JPA data models:
 * <ul>
 *   <li>{@code LocalDate ↔ java.sql.Date} — used by date columns</li>
 *   <li>{@code URI ↔ String} — OpenAPI {@code format: uri} vs JPA VARCHAR</li>
 *   <li>{@code LocalDateTime → OffsetDateTime} — JPA audit fields vs OpenAPI
 *       {@code format: date-time}</li>
 * </ul>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        addLocalDateToSqlDateConverter(modelMapper);
        addSqlDateToLocalDateConverter(modelMapper);
        addUriToStringConverter(modelMapper);
        addStringToUriConverter(modelMapper);
        addLocalDateTimeToOffsetDateTimeConverter(modelMapper);
        addOffsetDateTimeToLocalDateTimeConverter(modelMapper);

        return modelMapper;
    }

    private void addLocalDateToSqlDateConverter(ModelMapper modelMapper) {
        Converter<LocalDate, Date> converter = ctx ->
                ctx.getSource() == null ? null : Date.valueOf(ctx.getSource());
        modelMapper.addConverter(converter, LocalDate.class, Date.class);
    }

    private void addSqlDateToLocalDateConverter(ModelMapper modelMapper) {
        Converter<Date, LocalDate> converter = ctx ->
                ctx.getSource() == null ? null : ctx.getSource().toLocalDate();
        modelMapper.addConverter(converter, Date.class, LocalDate.class);
    }

    private void addUriToStringConverter(ModelMapper modelMapper) {
        Converter<URI, String> converter = ctx ->
                ctx.getSource() == null ? null : ctx.getSource().toString();
        modelMapper.addConverter(converter, URI.class, String.class);
    }

    private void addStringToUriConverter(ModelMapper modelMapper) {
        Converter<String, URI> converter = ctx ->
                ctx.getSource() == null ? null : URI.create(ctx.getSource());
        modelMapper.addConverter(converter, String.class, URI.class);
    }

    private void addLocalDateTimeToOffsetDateTimeConverter(ModelMapper modelMapper) {
        Converter<LocalDateTime, OffsetDateTime> converter = ctx ->
                ctx.getSource() == null ? null : ctx.getSource().atOffset(ZoneOffset.UTC);
        modelMapper.addConverter(converter, LocalDateTime.class, OffsetDateTime.class);
    }

    private void addOffsetDateTimeToLocalDateTimeConverter(ModelMapper modelMapper) {
        Converter<OffsetDateTime, LocalDateTime> converter = ctx ->
                ctx.getSource() == null ? null : ctx.getSource().toLocalDateTime();
        modelMapper.addConverter(converter, OffsetDateTime.class, LocalDateTime.class);
    }
}
