package com.akademiaplus.infra.persistence.idassigner;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.infra.persistence.exceptions.IdAssignmentException;
import com.akademiaplus.utilities.exceptions.InvalidTenantException;
import com.akademiaplus.utilities.idgeneration.IDGenerator;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.event.spi.PreInsertEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EntityIdAssigner
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EntityIdAssigner Tests")
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

    @Nested
    @DisplayName("Happy path ID assignment")
    class HappyPath {

        @Test
        @DisplayName("Should assign generated ID when entity has no ID")
        void shouldAssignGeneratedId_whenEntityHasNoId() throws Exception {
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
            InOrder inOrder = inOrder(metadataResolver, metadata, getter, idGenerationStrategy,
                    tenantContextHolder, idGenerator, setter, hibernateStateUpdater);
            inOrder.verify(metadataResolver, times(1)).resolve(TestEntity.class);
            inOrder.verify(metadata, times(1)).isSkip();
            inOrder.verify(metadata, times(1)).getGetter();
            inOrder.verify(getter, times(1)).invoke(entity);
            inOrder.verify(idGenerationStrategy, times(1)).shouldGenerateId(null);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(metadata, times(1)).getTableName();
            inOrder.verify(idGenerator, times(1)).generateId(TABLE_NAME, TENANT_ID);
            inOrder.verify(metadata, times(1)).getSetter();
            inOrder.verify(setter, times(1)).invoke(entity, GENERATED_ID);
            inOrder.verify(metadata, times(1)).getIdFieldName();
            inOrder.verify(hibernateStateUpdater, times(1)).updatePropertyInState(event, ID_FIELD_NAME, GENERATED_ID);
            verifyNoMoreInteractions(idGenerator, tenantContextHolder, metadataResolver,
                    idGenerationStrategy, hibernateStateUpdater, event, metadata, getter, setter);
        }

        @Test
        @DisplayName("Should handle zero ID as needing generation")
        void shouldAssignGeneratedId_whenEntityHasZeroId() throws Exception {
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
            InOrder inOrder = inOrder(metadataResolver, metadata, getter, idGenerationStrategy,
                    tenantContextHolder, idGenerator, setter, hibernateStateUpdater);
            inOrder.verify(metadataResolver, times(1)).resolve(TestEntity.class);
            inOrder.verify(metadata, times(1)).isSkip();
            inOrder.verify(metadata, times(1)).getGetter();
            inOrder.verify(getter, times(1)).invoke(entity);
            inOrder.verify(idGenerationStrategy, times(1)).shouldGenerateId(zeroId);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(metadata, times(1)).getTableName();
            inOrder.verify(idGenerator, times(1)).generateId(TABLE_NAME, TENANT_ID);
            inOrder.verify(metadata, times(1)).getSetter();
            inOrder.verify(setter, times(1)).invoke(entity, GENERATED_ID);
            inOrder.verify(metadata, times(1)).getIdFieldName();
            inOrder.verify(hibernateStateUpdater, times(1)).updatePropertyInState(event, ID_FIELD_NAME, GENERATED_ID);
            verifyNoMoreInteractions(idGenerator, tenantContextHolder, metadataResolver,
                    idGenerationStrategy, hibernateStateUpdater, event, metadata, getter, setter);
        }

        @Test
        @DisplayName("Should handle empty string ID as needing generation")
        void shouldAssignGeneratedId_whenEntityHasEmptyStringId() throws Exception {
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
            InOrder inOrder = inOrder(metadataResolver, metadata, getter, idGenerationStrategy,
                    tenantContextHolder, idGenerator, setter, hibernateStateUpdater);
            inOrder.verify(metadataResolver, times(1)).resolve(TestEntity.class);
            inOrder.verify(metadata, times(1)).isSkip();
            inOrder.verify(metadata, times(1)).getGetter();
            inOrder.verify(getter, times(1)).invoke(entity);
            inOrder.verify(idGenerationStrategy, times(1)).shouldGenerateId(emptyId);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(metadata, times(1)).getTableName();
            inOrder.verify(idGenerator, times(1)).generateId(TABLE_NAME, TENANT_ID);
            inOrder.verify(metadata, times(1)).getSetter();
            inOrder.verify(setter, times(1)).invoke(entity, GENERATED_ID);
            inOrder.verify(metadata, times(1)).getIdFieldName();
            inOrder.verify(hibernateStateUpdater, times(1)).updatePropertyInState(event, ID_FIELD_NAME, GENERATED_ID);
            verifyNoMoreInteractions(idGenerator, tenantContextHolder, metadataResolver,
                    idGenerationStrategy, hibernateStateUpdater, event, metadata, getter, setter);
        }

        @Test
        @DisplayName("Should override pre-set ID and log warning")
        void shouldOverridePreSetId_whenEntityHasExistingId() throws Exception {
            // Given
            Long preSetId = 999L;
            when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
            when(metadata.isSkip()).thenReturn(false);
            when(metadata.getGetter()).thenReturn(getter);
            when(metadata.getSetter()).thenReturn(setter);
            when(metadata.getTableName()).thenReturn(TABLE_NAME);
            when(metadata.getIdFieldName()).thenReturn(ID_FIELD_NAME);
            when(getter.invoke(entity)).thenReturn(preSetId);
            when(idGenerationStrategy.shouldGenerateId(preSetId)).thenReturn(false);
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(idGenerator.generateId(TABLE_NAME, TENANT_ID)).thenReturn(GENERATED_ID);

            // When
            assigner.assignIdIfNeeded(entity, event);

            // Then
            InOrder inOrder = inOrder(metadataResolver, metadata, getter, idGenerationStrategy,
                    tenantContextHolder, idGenerator, setter, hibernateStateUpdater);
            inOrder.verify(metadataResolver, times(1)).resolve(TestEntity.class);
            inOrder.verify(metadata, times(1)).isSkip();
            inOrder.verify(metadata, times(1)).getGetter();
            inOrder.verify(getter, times(1)).invoke(entity);
            inOrder.verify(idGenerationStrategy, times(1)).shouldGenerateId(preSetId);
            inOrder.verify(tenantContextHolder, times(2)).getTenantId();
            inOrder.verify(metadata, times(1)).getTableName();
            inOrder.verify(idGenerator, times(1)).generateId(TABLE_NAME, TENANT_ID);
            inOrder.verify(metadata, times(1)).getSetter();
            inOrder.verify(setter, times(1)).invoke(entity, GENERATED_ID);
            inOrder.verify(metadata, times(1)).getIdFieldName();
            inOrder.verify(hibernateStateUpdater, times(1)).updatePropertyInState(event, ID_FIELD_NAME, GENERATED_ID);
            verifyNoMoreInteractions(idGenerator, tenantContextHolder, metadataResolver,
                    idGenerationStrategy, hibernateStateUpdater, event, metadata, getter, setter);
        }
    }

    @Nested
    @DisplayName("Skip metadata path")
    class SkipMetadata {

        @Test
        @DisplayName("Should skip ID assignment when metadata indicates skip")
        void shouldSkipIdAssignment_whenMetadataIndicatesSkip() {
            // Given
            when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
            when(metadata.isSkip()).thenReturn(true);

            // When
            assigner.assignIdIfNeeded(entity, event);

            // Then
            InOrder inOrder = inOrder(metadataResolver, metadata);
            inOrder.verify(metadataResolver, times(1)).resolve(TestEntity.class);
            inOrder.verify(metadata, times(1)).isSkip();
            verifyNoInteractions(idGenerator, tenantContextHolder, idGenerationStrategy,
                    hibernateStateUpdater, getter, setter);
            verifyNoMoreInteractions(metadataResolver, event, metadata);
        }
    }

    @Nested
    @DisplayName("Exception paths")
    class ExceptionPaths {

        @Test
        @DisplayName("Should throw IdAssignmentException when no tenant context")
        void shouldThrowIdAssignmentException_whenNoTenantContext() throws Exception {
            // Given
            when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
            when(metadata.isSkip()).thenReturn(false);
            when(metadata.getGetter()).thenReturn(getter);
            when(metadata.getTableName()).thenReturn(TABLE_NAME);
            when(getter.invoke(entity)).thenReturn(null);
            when(idGenerationStrategy.shouldGenerateId(null)).thenReturn(true);
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.empty());

            // When / Then
            String expectedMessage = String.format(
                    EntityIdAssigner.ERROR_CANNOT_GENERATE_ID.replace("{}", "%s"), TEST_ENTITY_NAME);
            assertThatThrownBy(() -> assigner.assignIdIfNeeded(entity, event))
                    .isInstanceOf(IdAssignmentException.class)
                    .hasMessage(expectedMessage)
                    .hasCauseInstanceOf(InvalidTenantException.class);

            // Verify cutoff: idGenerator, setter, hibernateStateUpdater never called
            InOrder inOrder = inOrder(metadataResolver, metadata, getter, idGenerationStrategy, tenantContextHolder);
            inOrder.verify(metadataResolver, times(1)).resolve(TestEntity.class);
            inOrder.verify(metadata, times(1)).isSkip();
            inOrder.verify(metadata, times(1)).getGetter();
            inOrder.verify(getter, times(1)).invoke(entity);
            inOrder.verify(idGenerationStrategy, times(1)).shouldGenerateId(null);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(metadata, times(1)).getTableName();
            verifyNoInteractions(idGenerator, setter, hibernateStateUpdater);
            verifyNoMoreInteractions(metadataResolver, tenantContextHolder,
                    idGenerationStrategy, event, metadata, getter);
        }

        @Test
        @DisplayName("Should throw IdAssignmentException when getter invocation fails")
        void shouldThrowIdAssignmentException_whenGetterFails() throws Exception {
            // Given
            InvocationTargetException getterException = new InvocationTargetException(
                    new IllegalAccessException("Cannot access getter"));

            when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
            when(metadata.isSkip()).thenReturn(false);
            when(metadata.getGetter()).thenReturn(getter);
            when(getter.invoke(entity)).thenThrow(getterException);

            // When / Then
            String expectedMessage = String.format(
                    EntityIdAssigner.ERROR_CANNOT_GENERATE_ID.replace("{}", "%s"), TEST_ENTITY_NAME);
            assertThatThrownBy(() -> assigner.assignIdIfNeeded(entity, event))
                    .isInstanceOf(IdAssignmentException.class)
                    .hasMessage(expectedMessage)
                    .hasCause(getterException);

            // Verify cutoff: everything after getter not called
            InOrder inOrder = inOrder(metadataResolver, metadata, getter);
            inOrder.verify(metadataResolver, times(1)).resolve(TestEntity.class);
            inOrder.verify(metadata, times(1)).isSkip();
            inOrder.verify(metadata, times(1)).getGetter();
            inOrder.verify(getter, times(1)).invoke(entity);
            verifyNoInteractions(idGenerator, tenantContextHolder, idGenerationStrategy,
                    hibernateStateUpdater, setter);
            verifyNoMoreInteractions(metadataResolver, event, metadata, getter);
        }

        @Test
        @DisplayName("Should throw IdAssignmentException when setter invocation fails")
        void shouldThrowIdAssignmentException_whenSetterFails() throws Exception {
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

            // When / Then
            String expectedMessage = String.format(
                    EntityIdAssigner.ERROR_CANNOT_GENERATE_ID.replace("{}", "%s"), TEST_ENTITY_NAME);
            assertThatThrownBy(() -> assigner.assignIdIfNeeded(entity, event))
                    .isInstanceOf(IdAssignmentException.class)
                    .hasMessage(expectedMessage)
                    .hasCause(setterException);

            // Verify cutoff: hibernateStateUpdater never called
            InOrder inOrder = inOrder(metadataResolver, metadata, getter, idGenerationStrategy,
                    tenantContextHolder, idGenerator, setter);
            inOrder.verify(metadataResolver, times(1)).resolve(TestEntity.class);
            inOrder.verify(metadata, times(1)).isSkip();
            inOrder.verify(metadata, times(1)).getGetter();
            inOrder.verify(getter, times(1)).invoke(entity);
            inOrder.verify(idGenerationStrategy, times(1)).shouldGenerateId(null);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(metadata, times(1)).getTableName();
            inOrder.verify(idGenerator, times(1)).generateId(TABLE_NAME, TENANT_ID);
            inOrder.verify(metadata, times(1)).getSetter();
            inOrder.verify(setter, times(1)).invoke(entity, GENERATED_ID);
            verifyNoInteractions(hibernateStateUpdater);
            verifyNoMoreInteractions(idGenerator, tenantContextHolder, metadataResolver,
                    idGenerationStrategy, hibernateStateUpdater, event, metadata, getter, setter);
        }
    }

    @Nested
    @DisplayName("Collaborator exception propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should throw IdAssignmentException when ID generator fails")
        void shouldThrowIdAssignmentException_whenIdGeneratorFails() throws Exception {
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

            // When / Then
            String expectedMessage = String.format(
                    EntityIdAssigner.ERROR_CANNOT_GENERATE_ID.replace("{}", "%s"), TEST_ENTITY_NAME);
            assertThatThrownBy(() -> assigner.assignIdIfNeeded(entity, event))
                    .isInstanceOf(IdAssignmentException.class)
                    .hasMessage(expectedMessage)
                    .hasCause(generatorException);

            // Verify cutoff: setter and hibernateStateUpdater never called
            InOrder inOrder = inOrder(metadataResolver, metadata, getter, idGenerationStrategy,
                    tenantContextHolder, idGenerator);
            inOrder.verify(metadataResolver, times(1)).resolve(TestEntity.class);
            inOrder.verify(metadata, times(1)).isSkip();
            inOrder.verify(metadata, times(1)).getGetter();
            inOrder.verify(getter, times(1)).invoke(entity);
            inOrder.verify(idGenerationStrategy, times(1)).shouldGenerateId(null);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(metadata, times(1)).getTableName();
            inOrder.verify(idGenerator, times(1)).generateId(TABLE_NAME, TENANT_ID);
            verifyNoInteractions(setter, hibernateStateUpdater);
            verifyNoMoreInteractions(idGenerator, tenantContextHolder, metadataResolver,
                    idGenerationStrategy, event, metadata, getter);
        }

        @Test
        @DisplayName("Should throw IdAssignmentException when metadata resolver fails")
        void shouldThrowIdAssignmentException_whenMetadataResolverFails() {
            // Given
            RuntimeException resolverException = new RuntimeException("Resolver failed");
            when(metadataResolver.resolve(TestEntity.class)).thenThrow(resolverException);

            // When / Then
            String expectedMessage = String.format(
                    EntityIdAssigner.ERROR_CANNOT_GENERATE_ID.replace("{}", "%s"), TEST_ENTITY_NAME);
            assertThatThrownBy(() -> assigner.assignIdIfNeeded(entity, event))
                    .isInstanceOf(IdAssignmentException.class)
                    .hasMessage(expectedMessage)
                    .hasCause(resolverException);

            // Verify cutoff: everything after resolver not called
            verify(metadataResolver, times(1)).resolve(TestEntity.class);
            verifyNoInteractions(idGenerator, tenantContextHolder, idGenerationStrategy,
                    hibernateStateUpdater, getter, setter);
            verifyNoMoreInteractions(metadataResolver, event, metadata);
        }

        @Test
        @DisplayName("Should throw IdAssignmentException when ID generation strategy fails")
        void shouldThrowIdAssignmentException_whenIdGenerationStrategyFails() throws Exception {
            // Given
            RuntimeException strategyException = new RuntimeException("Strategy failed");

            when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
            when(metadata.isSkip()).thenReturn(false);
            when(metadata.getGetter()).thenReturn(getter);
            when(getter.invoke(entity)).thenReturn(null);
            when(idGenerationStrategy.shouldGenerateId(null)).thenThrow(strategyException);

            // When / Then
            String expectedMessage = String.format(
                    EntityIdAssigner.ERROR_CANNOT_GENERATE_ID.replace("{}", "%s"), TEST_ENTITY_NAME);
            assertThatThrownBy(() -> assigner.assignIdIfNeeded(entity, event))
                    .isInstanceOf(IdAssignmentException.class)
                    .hasMessage(expectedMessage)
                    .hasCause(strategyException);

            // Verify cutoff: everything after strategy not called
            InOrder inOrder = inOrder(metadataResolver, metadata, getter, idGenerationStrategy);
            inOrder.verify(metadataResolver, times(1)).resolve(TestEntity.class);
            inOrder.verify(metadata, times(1)).isSkip();
            inOrder.verify(metadata, times(1)).getGetter();
            inOrder.verify(getter, times(1)).invoke(entity);
            inOrder.verify(idGenerationStrategy, times(1)).shouldGenerateId(null);
            verifyNoInteractions(idGenerator, tenantContextHolder, setter, hibernateStateUpdater);
            verifyNoMoreInteractions(metadataResolver, idGenerationStrategy, event, metadata, getter);
        }

        @Test
        @DisplayName("Should throw IdAssignmentException when tenant context holder fails")
        void shouldThrowIdAssignmentException_whenTenantContextHolderFails() throws Exception {
            // Given
            RuntimeException tenantException = new RuntimeException("Tenant context failed");

            when(metadataResolver.resolve(TestEntity.class)).thenReturn(metadata);
            when(metadata.isSkip()).thenReturn(false);
            when(metadata.getGetter()).thenReturn(getter);
            when(getter.invoke(entity)).thenReturn(null);
            when(idGenerationStrategy.shouldGenerateId(null)).thenReturn(true);
            when(tenantContextHolder.getTenantId()).thenThrow(tenantException);

            // When / Then
            String expectedMessage = String.format(
                    EntityIdAssigner.ERROR_CANNOT_GENERATE_ID.replace("{}", "%s"), TEST_ENTITY_NAME);
            assertThatThrownBy(() -> assigner.assignIdIfNeeded(entity, event))
                    .isInstanceOf(IdAssignmentException.class)
                    .hasMessage(expectedMessage)
                    .hasCause(tenantException);

            // Verify cutoff: everything after tenant context not called
            InOrder inOrder = inOrder(metadataResolver, metadata, getter, idGenerationStrategy, tenantContextHolder);
            inOrder.verify(metadataResolver, times(1)).resolve(TestEntity.class);
            inOrder.verify(metadata, times(1)).isSkip();
            inOrder.verify(metadata, times(1)).getGetter();
            inOrder.verify(getter, times(1)).invoke(entity);
            inOrder.verify(idGenerationStrategy, times(1)).shouldGenerateId(null);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoInteractions(idGenerator, setter, hibernateStateUpdater);
            verifyNoMoreInteractions(metadataResolver, tenantContextHolder,
                    idGenerationStrategy, event, metadata, getter);
        }

        @Test
        @DisplayName("Should throw IdAssignmentException when hibernate state updater fails")
        void shouldThrowIdAssignmentException_whenHibernateStateUpdaterFails() throws Exception {
            // Given
            RuntimeException updaterException = new RuntimeException("State update failed");

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
            doThrow(updaterException).when(hibernateStateUpdater)
                    .updatePropertyInState(event, ID_FIELD_NAME, GENERATED_ID);

            // When / Then
            String expectedMessage = String.format(
                    EntityIdAssigner.ERROR_CANNOT_GENERATE_ID.replace("{}", "%s"), TEST_ENTITY_NAME);
            assertThatThrownBy(() -> assigner.assignIdIfNeeded(entity, event))
                    .isInstanceOf(IdAssignmentException.class)
                    .hasMessage(expectedMessage)
                    .hasCause(updaterException);

            // Verify all collaborators were called up to the failure point
            InOrder inOrder = inOrder(metadataResolver, metadata, getter, idGenerationStrategy,
                    tenantContextHolder, idGenerator, setter, hibernateStateUpdater);
            inOrder.verify(metadataResolver, times(1)).resolve(TestEntity.class);
            inOrder.verify(metadata, times(1)).isSkip();
            inOrder.verify(metadata, times(1)).getGetter();
            inOrder.verify(getter, times(1)).invoke(entity);
            inOrder.verify(idGenerationStrategy, times(1)).shouldGenerateId(null);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(metadata, times(1)).getTableName();
            inOrder.verify(idGenerator, times(1)).generateId(TABLE_NAME, TENANT_ID);
            inOrder.verify(metadata, times(1)).getSetter();
            inOrder.verify(setter, times(1)).invoke(entity, GENERATED_ID);
            inOrder.verify(metadata, times(1)).getIdFieldName();
            inOrder.verify(hibernateStateUpdater, times(1)).updatePropertyInState(event, ID_FIELD_NAME, GENERATED_ID);
            verifyNoMoreInteractions(idGenerator, tenantContextHolder, metadataResolver,
                    idGenerationStrategy, hibernateStateUpdater, event, metadata, getter, setter);
        }
    }

    // Test helper class
    @Setter
    @Getter
    private static class TestEntity {
        private Long userId;
    }
}
