package com.akademiaplus.util.base;

import com.akademiaplus.exceptions.FailToGenerateMockDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DataLoader")
@ExtendWith(MockitoExtension.class)
class DataLoaderTest {

    @Mock private TenantScopedRepository<ModelStub, Long> repository;
    @Mock private Function<DtoStub, ModelStub> transformer;
    @Mock private DataFactory<DtoStub> factory;

    private DataLoader<DtoStub, ModelStub, Long> dataLoader;

    @BeforeEach
    void setUp() {
        dataLoader = new DataLoader<>(repository, transformer, factory);
    }

    @Nested
    @DisplayName("Successful loading")
    class SuccessfulLoading {

        @Test
        void shouldCallFactoryWithExactCount_whenLoadIsCalled() {
            // Given
            int count = 3;
            DtoStub dto1 = new DtoStub("first");
            DtoStub dto2 = new DtoStub("second");
            DtoStub dto3 = new DtoStub("third");
            ModelStub model1 = new ModelStub("first");
            ModelStub model2 = new ModelStub("second");
            ModelStub model3 = new ModelStub("third");

            when(factory.generate(count)).thenReturn(List.of(dto1, dto2, dto3));
            when(transformer.apply(dto1)).thenReturn(model1);
            when(transformer.apply(dto2)).thenReturn(model2);
            when(transformer.apply(dto3)).thenReturn(model3);

            // When
            dataLoader.load(count);

            // Then
            verify(factory).generate(count);
        }

        @Test
        void shouldSaveAllGeneratedRecords_whenLoadIsCalled() {
            // Given
            int count = 2;
            DtoStub dto1 = new DtoStub("alpha");
            DtoStub dto2 = new DtoStub("beta");
            ModelStub model1 = new ModelStub("alpha");
            ModelStub model2 = new ModelStub("beta");

            when(factory.generate(count)).thenReturn(List.of(dto1, dto2));
            when(transformer.apply(dto1)).thenReturn(model1);
            when(transformer.apply(dto2)).thenReturn(model2);

            // When
            dataLoader.load(count);

            // Then
            verify(repository).save(model1);
            verify(repository).save(model2);
            verify(repository, times(2)).flush();
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        void shouldWrapExceptionInFailToGenerateMockDataException_whenSaveFails() {
            // Given
            int count = 1;
            DtoStub dto = new DtoStub("failing");
            ModelStub model = new ModelStub("failing");

            when(factory.generate(count)).thenReturn(List.of(dto));
            when(transformer.apply(dto)).thenReturn(model);
            when(repository.save(model)).thenThrow(new RuntimeException("DB error"));

            // When / Then
            assertThatThrownBy(() -> dataLoader.load(count))
                    .isInstanceOf(FailToGenerateMockDataException.class);
        }
    }

    record DtoStub(String name) {}
    record ModelStub(String name) {}
}
