package com.akademiaplus.infra.persistence.idassigner;

import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HibernateStateUpdater
 */
@ExtendWith(MockitoExtension.class)
class HibernateStateUpdaterTest {

    private static final String ID_PROPERTY_NAME = "userId";
    private static final String NAME_PROPERTY_NAME = "name";
    private static final String EMAIL_PROPERTY_NAME = "email";
    private static final String NON_EXISTENT_PROPERTY = "nonExistent";
    private static final Long GENERATED_ID = 12345L;
    private static final Long EXISTING_ID = 123L;
    private static final String USER_NAME = "John Doe";
    private static final String USER_EMAIL = "test@example.com";
    private static final String STRING_ID = "custom-id-123";
    private static final int FIRST_INDEX = 0;
    private static final int SECOND_INDEX = 1;
    private static final int THIRD_INDEX = 2;

    @Mock
    private PreInsertEvent event;

    @Mock
    private EntityPersister persister;

    @Mock
    private Object entity;

    private HibernateStateUpdater updater;

    @BeforeEach
    void setUp() {
        updater = new HibernateStateUpdater();
    }

    @Test
    void shouldUpdatePropertyInStateWhenPropertyExists() {
        // Given
        String[] propertyNames = {ID_PROPERTY_NAME, NAME_PROPERTY_NAME};
        Object[] state = {null, USER_NAME};

        when(event.getPersister()).thenReturn(persister);
        when(persister.getPropertyNames()).thenReturn(propertyNames);
        when(event.getState()).thenReturn(state);
        when(event.getEntity()).thenReturn(entity);

        // When
        updater.updatePropertyInState(event, ID_PROPERTY_NAME, GENERATED_ID);

        // Then
        assertThat(state[FIRST_INDEX]).isEqualTo(GENERATED_ID);
        assertThat(state[SECOND_INDEX]).isEqualTo(USER_NAME); // Other properties unchanged

        InOrder inOrder = inOrder(event, persister);
        inOrder.verify(event, times(1)).getPersister();
        inOrder.verify(persister, times(1)).getPropertyNames();
        inOrder.verify(event, times(1)).getState();
        inOrder.verify(event, times(1)).getEntity();
        verifyNoMoreInteractions(event, persister, entity);
    }

    @Test
    void shouldUpdatePropertyInStateAtCorrectIndex() {
        // Given
        String[] propertyNames = {NAME_PROPERTY_NAME, ID_PROPERTY_NAME, EMAIL_PROPERTY_NAME};
        Object[] state = {USER_NAME, null, USER_EMAIL};

        when(event.getPersister()).thenReturn(persister);
        when(persister.getPropertyNames()).thenReturn(propertyNames);
        when(event.getState()).thenReturn(state);
        when(event.getEntity()).thenReturn(entity);

        // When
        updater.updatePropertyInState(event, ID_PROPERTY_NAME, GENERATED_ID);

        // Then
        assertThat(state[FIRST_INDEX]).isEqualTo(USER_NAME); // First property unchanged
        assertThat(state[SECOND_INDEX]).isEqualTo(GENERATED_ID); // ID updated
        assertThat(state[THIRD_INDEX]).isEqualTo(USER_EMAIL); // Third property unchanged

        InOrder inOrder = inOrder(event, persister);
        inOrder.verify(event, times(1)).getPersister();
        inOrder.verify(persister, times(1)).getPropertyNames();
        inOrder.verify(event, times(1)).getState();
        inOrder.verify(event, times(1)).getEntity();
        verifyNoMoreInteractions(event, persister, entity);
    }

    @Test
    void shouldHandlePropertyNotFoundGracefully() {
        // Given
        String[] propertyNames = {NAME_PROPERTY_NAME, ID_PROPERTY_NAME};
        Object[] state = {USER_NAME, null};

        when(event.getPersister()).thenReturn(persister);
        when(persister.getPropertyNames()).thenReturn(propertyNames);
        when(event.getState()).thenReturn(state);
        when(event.getId()).thenReturn(null);
        when(event.getEntity()).thenReturn(entity);

        // When
        updater.updatePropertyInState(event, NON_EXISTENT_PROPERTY, GENERATED_ID);

        // Then - state should remain unchanged
        assertThat(state[FIRST_INDEX]).isEqualTo(USER_NAME);
        assertThat(state[SECOND_INDEX]).isNull();

        InOrder inOrder = inOrder(event, persister);
        inOrder.verify(event, times(1)).getPersister();
        inOrder.verify(persister, times(1)).getPropertyNames();
        inOrder.verify(event, times(1)).getState();
        inOrder.verify(event, times(1)).getId();
        inOrder.verify(event, times(1)).getEntity();
        verifyNoMoreInteractions(event, persister, entity);
    }

    @Test
    void shouldUpdatePropertyWithNullValue() {
        // Given
        String[] propertyNames = {ID_PROPERTY_NAME};
        Object[] state = {EXISTING_ID}; // Existing value
        Object nullValue = null;

        when(event.getPersister()).thenReturn(persister);
        when(persister.getPropertyNames()).thenReturn(propertyNames);
        when(event.getState()).thenReturn(state);
        when(event.getEntity()).thenReturn(entity);

        // When
        updater.updatePropertyInState(event, ID_PROPERTY_NAME, nullValue);

        // Then
        assertThat(state[FIRST_INDEX]).isNull();

        InOrder inOrder = inOrder(event, persister);
        inOrder.verify(event, times(1)).getPersister();
        inOrder.verify(persister, times(1)).getPropertyNames();
        inOrder.verify(event, times(1)).getState();
        inOrder.verify(event, times(1)).getEntity();
        verifyNoMoreInteractions(event, persister, entity);
    }

    @Test
    void shouldUpdatePropertyWithDifferentTypes() {
        // Given
        String[] propertyNames = {ID_PROPERTY_NAME};
        Object[] state = {null};

        when(event.getPersister()).thenReturn(persister);
        when(persister.getPropertyNames()).thenReturn(propertyNames);
        when(event.getState()).thenReturn(state);
        when(event.getEntity()).thenReturn(entity);

        // When
        updater.updatePropertyInState(event, ID_PROPERTY_NAME, STRING_ID);

        // Then
        assertThat(state[FIRST_INDEX]).isEqualTo(STRING_ID);

        InOrder inOrder = inOrder(event, persister);
        inOrder.verify(event, times(1)).getPersister();
        inOrder.verify(persister, times(1)).getPropertyNames();
        inOrder.verify(event, times(1)).getState();
        inOrder.verify(event, times(1)).getEntity();
        verifyNoMoreInteractions(event, persister, entity);
    }
}
