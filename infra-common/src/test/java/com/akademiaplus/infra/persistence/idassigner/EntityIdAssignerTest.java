package com.akademiaplus.infra.persistence.idassigner;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.infra.persistence.exceptions.IdAssignmentException;
import com.akademiaplus.utilities.idgeneration.IDGenerator;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.event.spi.PreInsertEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EntityIdAssigner
 */
@ExtendWith(MockitoExtension.class)
class EntityIdAssignerTest {

    private static final Long GENERATED_ID = 12345L;
    private static final Long TENANT_ID = 100L;
    private static final String TABLE_NAME = "users";
    private static final String ID_FIELD_NAME = "userId";
    private static final String TEST_ENTITY_NAME = "TestEntity";

    @Mock
    private IDGenerator idGenerator;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private EntityMetadataResolver metadataResolver;

    @Mock
    private IdGenerationStrategy idGenerationStrategy;

    @Mock
    private HibernateStateUpdater hibernateStateUpdater;

    @Mock
    private PreInsertEvent event;

    @Mock
    private EntityMetadata metadata;

    @Mock
    private Method getter;

    @Mock
    private Method setter;

    private EntityIdAssigner assigner;
    private TestEntity entity;

    @BeforeEach
    void setUp() {
        assigner = new EntityIdAssigner(
                idGenerator,
                tenantContextHolder,
                metadataResolver,
                idGenerationStrategy,
                hibernateStateUpdater
        );
        entity = new TestEntity();
    }

    @Test
    void shouldAssignIdWhenEntityHasNoId() throws Exception {
        // Given
        when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
        when(metadata.isSkip()).thenReturn(false);
        when(metadata.getGetter()).thenReturn(getter);
        when(metadata.getSetter()).thenReturn(setter);
        when(metadata.getTableName()).thenReturn(TABLE_NAME);
        when(metadata.getIdFieldName()).thenReturn(ID_FIELD_NAME);
        when(getter.invoke(entity)).thenReturn(null);
        when(idGenerationStrategy.shouldGenerateId(null)).thenReturn(true);
        when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
        when(idGenerator.generateId(TABLE_NAME, TENANT_ID)).thenReturn(GENERATED_ID);

        // When
        assigner.assignIdIfNeeded(entity, event);

        // Then
        verify(setter).invoke(entity, GENERATED_ID);
        verify(hibernateStateUpdater).updatePropertyInState(event, ID_FIELD_NAME, GENERATED_ID);
        verify(idGenerator).generateId(TABLE_NAME, TENANT_ID);
    }

    @Test
    void shouldAssignIdWhenNoTenantContext() throws Exception {
        // Given
        when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
        when(metadata.isSkip()).thenReturn(false);
        when(metadata.getGetter()).thenReturn(getter);
        when(metadata.getSetter()).thenReturn(setter);
        when(metadata.getTableName()).thenReturn(TABLE_NAME);
        when(metadata.getIdFieldName()).thenReturn(ID_FIELD_NAME);
        when(getter.invoke(entity)).thenReturn(null);
        when(idGenerationStrategy.shouldGenerateId(null)).thenReturn(true);
        when(tenantContextHolder.getTenantId()).thenReturn(Optional.empty());
        when(idGenerator.generateId(TABLE_NAME, null)).thenReturn(GENERATED_ID);

        // When
        assigner.assignIdIfNeeded(entity, event);

        // Then
        verify(setter).invoke(entity, GENERATED_ID);
        verify(idGenerator).generateId(TABLE_NAME, null);
    }

    @Test
    void shouldOverridePreSetIdAndLogWarning() throws Exception {
        // Given
        Long preSetId = 999L;
        when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
        when(metadata.isSkip()).thenReturn(false);
        when(metadata.getGetter()).thenReturn(getter);
        when(metadata.getSetter()).thenReturn(setter);
        when(metadata.getTableName()).thenReturn(TABLE_NAME);
        when(metadata.getIdFieldName()).thenReturn(ID_FIELD_NAME);
        when(getter.invoke(entity)).thenReturn(preSetId);
        when(idGenerationStrategy.shouldGenerateId(preSetId)).thenReturn(false); // ID is already set
        when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
        when(idGenerator.generateId(TABLE_NAME, TENANT_ID)).thenReturn(GENERATED_ID);

        // When
        assigner.assignIdIfNeeded(entity, event);

        // Then - should still assign new ID despite pre-set value
        verify(setter).invoke(entity, GENERATED_ID);
        verify(hibernateStateUpdater).updatePropertyInState(event, ID_FIELD_NAME, GENERATED_ID);
        verify(idGenerator).generateId(TABLE_NAME, TENANT_ID);
    }

    @Test
    void shouldSkipIdAssignment_whenMetadataIndicatesSkip() {
        // Given
        when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
        when(metadata.isSkip()).thenReturn(true);

        // When
        assigner.assignIdIfNeeded(entity, event);

        // Then
        verify(metadata, never()).getGetter();
        verifyNoInteractions(idGenerator);
        verifyNoInteractions(setter);
    }

    @Test
    void shouldThrowIdAssignmentExceptionWhenSetterFails() throws Exception {
        // Given
        InvocationTargetException setterException = new InvocationTargetException(
                new IllegalAccessException("Cannot access setter"));

        when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
        when(metadata.isSkip()).thenReturn(false);
        when(metadata.getGetter()).thenReturn(getter);
        when(metadata.getSetter()).thenReturn(setter);
        when(metadata.getTableName()).thenReturn(TABLE_NAME);
        when(getter.invoke(entity)).thenReturn(null);
        when(idGenerationStrategy.shouldGenerateId(null)).thenReturn(true);
        when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
        when(idGenerator.generateId(TABLE_NAME, TENANT_ID)).thenReturn(GENERATED_ID);
        when(setter.invoke(entity, GENERATED_ID)).thenThrow(setterException);

        // When/Then
        String expectedMessage = formatErrorMessage(EntityIdAssigner.ERROR_CANNOT_GENERATE_ID, TEST_ENTITY_NAME);
        assertThatThrownBy(() -> assigner.assignIdIfNeeded(entity, event))
                .isInstanceOf(IdAssignmentException.class)
                .hasMessageContaining(expectedMessage);

        verifyNoInteractions(hibernateStateUpdater);
    }

    @Test
    void shouldThrowIdAssignmentExceptionWhenGetterFails() throws Exception {
        // Given
        InvocationTargetException getterException = new InvocationTargetException(
                new IllegalAccessException("Cannot access getter"));

        when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
        when(metadata.isSkip()).thenReturn(false);
        when(metadata.getGetter()).thenReturn(getter);
        when(getter.invoke(entity)).thenThrow(getterException);

        // When/Then
        String expectedMessage = formatErrorMessage(EntityIdAssigner.ERROR_CANNOT_GENERATE_ID, TEST_ENTITY_NAME);
        assertThatThrownBy(() -> assigner.assignIdIfNeeded(entity, event))
                .isInstanceOf(IdAssignmentException.class)
                .hasMessageContaining(expectedMessage);

        verifyNoInteractions(idGenerator);
    }

    @Test
    void shouldThrowIdAssignmentExceptionWhenIdGeneratorFails() throws Exception {
        // Given
        RuntimeException generatorException = new RuntimeException("ID generation failed");

        when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
        when(metadata.isSkip()).thenReturn(false);
        when(metadata.getGetter()).thenReturn(getter);
        when(metadata.getTableName()).thenReturn(TABLE_NAME);
        when(getter.invoke(entity)).thenReturn(null);
        when(idGenerationStrategy.shouldGenerateId(null)).thenReturn(true);
        when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
        when(idGenerator.generateId(TABLE_NAME, TENANT_ID)).thenThrow(generatorException);

        // When/Then
        String expectedMessage = formatErrorMessage(EntityIdAssigner.ERROR_CANNOT_GENERATE_ID, TEST_ENTITY_NAME);
        assertThatThrownBy(() -> assigner.assignIdIfNeeded(entity, event))
                .isInstanceOf(IdAssignmentException.class)
                .hasMessageContaining(expectedMessage)
                .hasCause(generatorException);

        verifyNoInteractions(setter);
    }

    @Test
    void shouldHandleZeroIdAsNeedingGeneration() throws Exception {
        // Given
        Long zeroId = 0L;
        when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
        when(metadata.isSkip()).thenReturn(false);
        when(metadata.getGetter()).thenReturn(getter);
        when(metadata.getSetter()).thenReturn(setter);
        when(metadata.getTableName()).thenReturn(TABLE_NAME);
        when(metadata.getIdFieldName()).thenReturn(ID_FIELD_NAME);
        when(getter.invoke(entity)).thenReturn(zeroId);
        when(idGenerationStrategy.shouldGenerateId(zeroId)).thenReturn(true);
        when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
        when(idGenerator.generateId(TABLE_NAME, TENANT_ID)).thenReturn(GENERATED_ID);

        // When
        assigner.assignIdIfNeeded(entity, event);

        // Then
        verify(setter).invoke(entity, GENERATED_ID);
        verify(idGenerator).generateId(TABLE_NAME, TENANT_ID);
    }

    @Test
    void shouldHandleEmptyStringIdAsNeedingGeneration() throws Exception {
        // Given
        String emptyId = "";
        when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
        when(metadata.isSkip()).thenReturn(false);
        when(metadata.getGetter()).thenReturn(getter);
        when(metadata.getSetter()).thenReturn(setter);
        when(metadata.getTableName()).thenReturn(TABLE_NAME);
        when(metadata.getIdFieldName()).thenReturn(ID_FIELD_NAME);
        when(getter.invoke(entity)).thenReturn(emptyId);
        when(idGenerationStrategy.shouldGenerateId(emptyId)).thenReturn(true);
        when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
        when(idGenerator.generateId(TABLE_NAME, TENANT_ID)).thenReturn(GENERATED_ID);

        // When
        assigner.assignIdIfNeeded(entity, event);

        // Then
        verify(setter).invoke(entity, GENERATED_ID);
        verify(idGenerator).generateId(TABLE_NAME, TENANT_ID);
    }

    /**
     * Helper method to format error messages the same way EntityIdAssigner does
     * Converts SLF4J placeholder {} to String.format placeholder %s and formats
     */
    private String formatErrorMessage(String messageTemplate, String... args) {
        return String.format(messageTemplate.replace("{}", "%s"), (Object[]) args);
    }

    // Test helper class
    @Setter
    @Getter
    private static class TestEntity {
        private Long userId;
    }
}