package com.akademiaplus.customer.tutor.usecases;

import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.PiiField;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import openapi.akademiaplus.domain.user.management.dto.MinorStudentCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.MinorStudentCreationResponseDTO;
import openapi.akademiaplus.domain.user.management.dto.TutorCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.TutorCreationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.context.ApplicationContext;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("TutorCreationUseCase")
@ExtendWith(MockitoExtension.class)
class TutorCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private TutorRepository tutorRepository;
    @Mock private MinorStudentRepository minorStudentRepository;
    @Mock private PersonPIIRepository personPIIRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;
    @Mock private HashingService hashingService;
    @Mock private PiiNormalizer piiNormalizer;

    private TutorCreationUseCase useCase;

    private static final Long TENANT_ID = 1L;
    private static final String TEST_EMAIL = "jdoe@example.com";
    private static final String TEST_PHONE = "5551234567";
    private static final String NORMALIZED_EMAIL = "jdoe@example.com";
    private static final String NORMALIZED_PHONE = "+525551234567";
    private static final String EMAIL_HASH = "hashed_email";
    private static final String PHONE_HASH = "hashed_phone";

    @BeforeEach
    void setUp() {
        useCase = new TutorCreationUseCase(
                applicationContext,
                tutorRepository,
                minorStudentRepository,
                personPIIRepository,
                tenantContextHolder,
                modelMapper,
                hashingService,
                piiNormalizer
        );
    }

    @Nested
    @DisplayName("Tutor transformation")
    class TutorTransformation {

        private TutorCreationRequestDTO tutorDto;
        private TutorDataModel tutorModel;
        private PersonPIIDataModel personPII;

        @BeforeEach
        void setUpTutor() {
            tutorDto = new TutorCreationRequestDTO(
                    LocalDate.of(1990, 1, 15),
                    "John", "Doe",
                    TEST_EMAIL, TEST_PHONE,
                    "123 Main St", "12345"
            );

            tutorModel = new TutorDataModel();
            personPII = new PersonPIIDataModel();
            personPII.setEmail(TEST_EMAIL);
            personPII.setPhoneNumber(TEST_PHONE);
        }

        @Test
        @DisplayName("Should map DTO to TutorDataModel when given valid DTO")
        void shouldMapDtoToTutorDataModel_whenGivenValidDto() {
            // Given
            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(tutorDto, personPII);
            when(applicationContext.getBean(TutorDataModel.class)).thenReturn(tutorModel);
            doNothing().when(modelMapper).map(tutorDto, tutorModel, TutorCreationUseCase.TUTOR_MAP_NAME);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);

            // When
            TutorDataModel result = useCase.transformTutor(tutorDto);

            // Then
            assertThat(result.getPersonPII()).isEqualTo(personPII);
            assertThat(result.getEntryDate()).isEqualTo(LocalDate.now());
            verify(applicationContext, times(1)).getBean(TutorDataModel.class);
            verify(applicationContext, times(1)).getBean(PersonPIIDataModel.class);
            verifyNoMoreInteractions(applicationContext, modelMapper, piiNormalizer, hashingService);
            verifyNoInteractions(tutorRepository, minorStudentRepository, personPIIRepository, tenantContextHolder);
        }

        @Test
        @DisplayName("Should create CustomerAuth when provider is present")
        void shouldCreateCustomerAuth_whenProviderIsPresent() {
            // Given
            String provider = "GOOGLE";
            String token = "oauth_abc123";
            tutorDto.setProvider(JsonNullable.of(provider));
            tutorDto.setToken(JsonNullable.of(token));
            CustomerAuthDataModel customerAuth = new CustomerAuthDataModel();

            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(tutorDto, personPII);
            when(applicationContext.getBean(TutorDataModel.class)).thenReturn(tutorModel);
            doNothing().when(modelMapper).map(tutorDto, tutorModel, TutorCreationUseCase.TUTOR_MAP_NAME);
            when(applicationContext.getBean(CustomerAuthDataModel.class)).thenReturn(customerAuth);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);

            // When
            TutorDataModel result = useCase.transformTutor(tutorDto);

            // Then
            assertThat(result.getCustomerAuth()).isNotNull();
            assertThat(result.getCustomerAuth().getProvider()).isEqualTo(provider);
            assertThat(result.getCustomerAuth().getToken()).isEqualTo(token);
            verifyNoMoreInteractions(applicationContext, modelMapper, piiNormalizer, hashingService);
            verifyNoInteractions(tutorRepository, minorStudentRepository, personPIIRepository, tenantContextHolder);
        }

        @Test
        @DisplayName("Should not create CustomerAuth when provider is absent")
        void shouldNotCreateCustomerAuth_whenProviderIsAbsent() {
            // Given — provider not set (remains null by default)
            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(tutorDto, personPII);
            when(applicationContext.getBean(TutorDataModel.class)).thenReturn(tutorModel);
            doNothing().when(modelMapper).map(tutorDto, tutorModel, TutorCreationUseCase.TUTOR_MAP_NAME);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);

            // When
            TutorDataModel result = useCase.transformTutor(tutorDto);

            // Then
            assertThat(result.getCustomerAuth()).isNull();
            verifyNoMoreInteractions(applicationContext, modelMapper, piiNormalizer, hashingService);
            verifyNoInteractions(tutorRepository, minorStudentRepository, personPIIRepository, tenantContextHolder);
        }

        @Test
        @DisplayName("Should hash email and phone when transforming tutor")
        void shouldHashEmailAndPhone_whenTransformingTutor() {
            // Given
            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(tutorDto, personPII);
            when(applicationContext.getBean(TutorDataModel.class)).thenReturn(tutorModel);
            doNothing().when(modelMapper).map(tutorDto, tutorModel, TutorCreationUseCase.TUTOR_MAP_NAME);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);

            // When
            useCase.transformTutor(tutorDto);

            // Then
            verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            verify(hashingService, times(1)).generateHash(NORMALIZED_EMAIL);
            verify(piiNormalizer, times(1)).normalizePhoneNumber(TEST_PHONE);
            verify(hashingService, times(1)).generateHash(NORMALIZED_PHONE);
            assertThat(personPII.getEmailHash()).isEqualTo(EMAIL_HASH);
            assertThat(personPII.getPhoneHash()).isEqualTo(PHONE_HASH);
            verifyNoMoreInteractions(applicationContext, modelMapper, piiNormalizer, hashingService);
            verifyNoInteractions(tutorRepository, minorStudentRepository, personPIIRepository, tenantContextHolder);
        }
    }

    @Nested
    @DisplayName("Minor student transformation")
    class MinorStudentTransformation {

        private MinorStudentCreationRequestDTO minorDto;
        private MinorStudentDataModel minorModel;
        private PersonPIIDataModel personPII;
        private TutorDataModel existingTutor;
        private static final Long TUTOR_ID = 42L;

        @BeforeEach
        void setUpMinorStudent() {
            minorDto = new MinorStudentCreationRequestDTO(
                    LocalDate.of(2012, 6, 1),
                    TUTOR_ID,
                    "Jane", "Doe",
                    TEST_EMAIL, TEST_PHONE,
                    "456 Oak Ave", "67890",
                    "GOOGLE", "oauth_token123"
            );

            minorModel = new MinorStudentDataModel();
            personPII = new PersonPIIDataModel();
            personPII.setEmail(TEST_EMAIL);
            personPII.setPhoneNumber(TEST_PHONE);
            existingTutor = new TutorDataModel();

            lenient().when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
        }

        @Test
        @DisplayName("Should map DTO to MinorStudentDataModel when given valid DTO")
        void shouldMapDtoToMinorStudentDataModel_whenGivenValidDto() {
            // Given
            CustomerAuthDataModel customerAuth = new CustomerAuthDataModel();
            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(minorDto, personPII);
            when(applicationContext.getBean(MinorStudentDataModel.class)).thenReturn(minorModel);
            doNothing().when(modelMapper).map(minorDto, minorModel, TutorCreationUseCase.MINOR_STUDENT_MAP_NAME);
            when(applicationContext.getBean(CustomerAuthDataModel.class)).thenReturn(customerAuth);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
            when(tutorRepository.findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID))).thenReturn(Optional.of(existingTutor));

            // When
            MinorStudentDataModel result = useCase.transformMinorStudent(minorDto);

            // Then
            assertThat(result.getPersonPII()).isEqualTo(personPII);
            assertThat(result.getEntryDate()).isEqualTo(LocalDate.now());
            assertThat(result.getCustomerAuth()).isEqualTo(customerAuth);
            assertThat(result.getCustomerAuth().getProvider()).isEqualTo("GOOGLE");
            assertThat(result.getCustomerAuth().getToken()).isEqualTo("oauth_token123");
            verify(applicationContext, times(1)).getBean(MinorStudentDataModel.class);
            verify(applicationContext, times(1)).getBean(PersonPIIDataModel.class);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoMoreInteractions(applicationContext, modelMapper, piiNormalizer, hashingService, tutorRepository, tenantContextHolder);
            verifyNoInteractions(minorStudentRepository, personPIIRepository);
        }


        @Test
        @DisplayName("Should look up tutor when transforming minor student")
        void shouldLookUpTutor_whenTransformingMinorStudent() {
            // Given
            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(minorDto, personPII);
            when(applicationContext.getBean(MinorStudentDataModel.class)).thenReturn(minorModel);
            doNothing().when(modelMapper).map(minorDto, minorModel, TutorCreationUseCase.MINOR_STUDENT_MAP_NAME);
            when(applicationContext.getBean(CustomerAuthDataModel.class)).thenReturn(new CustomerAuthDataModel());
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
            when(tutorRepository.findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID))).thenReturn(Optional.of(existingTutor));

            // When
            MinorStudentDataModel result = useCase.transformMinorStudent(minorDto);

            // Then
            verify(tutorRepository, times(1)).findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID));
            assertThat(result.getTutor()).isEqualTo(existingTutor);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoMoreInteractions(applicationContext, modelMapper, piiNormalizer, hashingService, tutorRepository, tenantContextHolder);
            verifyNoInteractions(minorStudentRepository, personPIIRepository);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when tenant context is missing")
        void shouldThrowIllegalArgumentException_whenTenantContextMissing() {
            // Given
            CustomerAuthDataModel customerAuth = new CustomerAuthDataModel();
            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(minorDto, personPII);
            when(applicationContext.getBean(MinorStudentDataModel.class)).thenReturn(minorModel);
            doNothing().when(modelMapper).map(minorDto, minorModel, TutorCreationUseCase.MINOR_STUDENT_MAP_NAME);
            when(applicationContext.getBean(CustomerAuthDataModel.class)).thenReturn(customerAuth);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.transformMinorStudent(minorDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(TutorCreationUseCase.ERROR_TENANT_CONTEXT_REQUIRED);

            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoInteractions(tutorRepository, minorStudentRepository, personPIIRepository);
        }

        @Test
        @DisplayName("Should throw exception when tutor not found")
        void shouldThrowException_whenTutorNotFound() {
            // Given
            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(minorDto, personPII);
            when(applicationContext.getBean(MinorStudentDataModel.class)).thenReturn(minorModel);
            doNothing().when(modelMapper).map(minorDto, minorModel, TutorCreationUseCase.MINOR_STUDENT_MAP_NAME);
            when(applicationContext.getBean(CustomerAuthDataModel.class)).thenReturn(new CustomerAuthDataModel());
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
            when(tutorRepository.findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.transformMinorStudent(minorDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(TutorCreationUseCase.ERROR_TUTOR_NOT_FOUND + TUTOR_ID);

            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoMoreInteractions(applicationContext, modelMapper, piiNormalizer, hashingService, tutorRepository, tenantContextHolder);
            verifyNoInteractions(minorStudentRepository, personPIIRepository);
        }

        @Test
        @DisplayName("Should hash email and phone when transforming minor student")
        void shouldHashEmailAndPhone_whenTransformingMinorStudent() {
            // Given
            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(minorDto, personPII);
            when(applicationContext.getBean(MinorStudentDataModel.class)).thenReturn(minorModel);
            doNothing().when(modelMapper).map(minorDto, minorModel, TutorCreationUseCase.MINOR_STUDENT_MAP_NAME);
            when(applicationContext.getBean(CustomerAuthDataModel.class)).thenReturn(new CustomerAuthDataModel());
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
            when(tutorRepository.findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID))).thenReturn(Optional.of(existingTutor));

            // When
            useCase.transformMinorStudent(minorDto);

            // Then
            verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            verify(hashingService, times(1)).generateHash(NORMALIZED_EMAIL);
            verify(piiNormalizer, times(1)).normalizePhoneNumber(TEST_PHONE);
            verify(hashingService, times(1)).generateHash(NORMALIZED_PHONE);
            assertThat(personPII.getEmailHash()).isEqualTo(EMAIL_HASH);
            assertThat(personPII.getPhoneHash()).isEqualTo(PHONE_HASH);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoMoreInteractions(applicationContext, modelMapper, piiNormalizer, hashingService, tutorRepository, tenantContextHolder);
            verifyNoInteractions(minorStudentRepository, personPIIRepository);
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save and return response when creating tutor")
        void shouldSaveAndReturnResponse_whenCreatingTutor() {
            // Given
            TutorCreationRequestDTO tutorDto = new TutorCreationRequestDTO(
                    LocalDate.of(1990, 1, 15),
                    "John", "Doe",
                    TEST_EMAIL, TEST_PHONE,
                    "123 Main St", "12345"
            );
            TutorDataModel tutorModel = new TutorDataModel();
            PersonPIIDataModel personPII = new PersonPIIDataModel();
            personPII.setEmail(TEST_EMAIL);
            personPII.setPhoneNumber(TEST_PHONE);
            TutorDataModel savedTutor = new TutorDataModel();
            TutorCreationResponseDTO expectedResponse = new TutorCreationResponseDTO();

            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(tutorDto, personPII);
            when(applicationContext.getBean(TutorDataModel.class)).thenReturn(tutorModel);
            doNothing().when(modelMapper).map(tutorDto, tutorModel, TutorCreationUseCase.TUTOR_MAP_NAME);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
            when(personPIIRepository.existsByEmailHash(EMAIL_HASH)).thenReturn(false);
            when(personPIIRepository.existsByPhoneHash(PHONE_HASH)).thenReturn(false);
            when(tutorRepository.saveAndFlush(tutorModel)).thenReturn(savedTutor);
            when(modelMapper.map(savedTutor, TutorCreationResponseDTO.class)).thenReturn(expectedResponse);

            // When
            TutorCreationResponseDTO result = useCase.create(tutorDto);

            // Then
            assertThat(result).isEqualTo(expectedResponse);

            verify(tutorRepository, times(1)).saveAndFlush(tutorModel);
            verify(personPIIRepository, times(1)).existsByEmailHash(EMAIL_HASH);
            verify(personPIIRepository, times(1)).existsByPhoneHash(PHONE_HASH);
            verifyNoInteractions(minorStudentRepository, tenantContextHolder);
        }

        @Test
        @DisplayName("Should save and return response when creating minor student")
        void shouldSaveAndReturnResponse_whenCreatingMinorStudent() {
            // Given
            Long tutorId = 42L;
            MinorStudentCreationRequestDTO minorDto = new MinorStudentCreationRequestDTO(
                    LocalDate.of(2012, 6, 1),
                    tutorId,
                    "Jane", "Doe",
                    TEST_EMAIL, TEST_PHONE,
                    "456 Oak Ave", "67890",
                    "GOOGLE", "oauth_token123"
            );
            MinorStudentDataModel minorModel = new MinorStudentDataModel();
            PersonPIIDataModel personPII = new PersonPIIDataModel();
            personPII.setEmail(TEST_EMAIL);
            personPII.setPhoneNumber(TEST_PHONE);
            TutorDataModel existingTutor = new TutorDataModel();
            CustomerAuthDataModel customerAuth = new CustomerAuthDataModel();
            MinorStudentDataModel savedMinor = new MinorStudentDataModel();
            MinorStudentCreationResponseDTO expectedResponse = new MinorStudentCreationResponseDTO();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(minorDto, personPII);
            when(applicationContext.getBean(MinorStudentDataModel.class)).thenReturn(minorModel);
            doNothing().when(modelMapper).map(minorDto, minorModel, TutorCreationUseCase.MINOR_STUDENT_MAP_NAME);
            when(applicationContext.getBean(CustomerAuthDataModel.class)).thenReturn(customerAuth);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
            when(tutorRepository.findById(new TutorDataModel.TutorCompositeId(TENANT_ID, tutorId))).thenReturn(Optional.of(existingTutor));
            when(personPIIRepository.existsByEmailHash(EMAIL_HASH)).thenReturn(false);
            when(personPIIRepository.existsByPhoneHash(PHONE_HASH)).thenReturn(false);
            when(minorStudentRepository.saveAndFlush(minorModel)).thenReturn(savedMinor);
            when(modelMapper.map(savedMinor, MinorStudentCreationResponseDTO.class)).thenReturn(expectedResponse);

            // When
            MinorStudentCreationResponseDTO result = useCase.createMinorStudent(minorDto);

            // Then
            assertThat(result).isEqualTo(expectedResponse);

            verify(minorStudentRepository, times(1)).saveAndFlush(minorModel);
            verify(personPIIRepository, times(1)).existsByEmailHash(EMAIL_HASH);
            verify(personPIIRepository, times(1)).existsByPhoneHash(PHONE_HASH);
        }
    }

    @Nested
    @DisplayName("Duplicate validation")
    class DuplicateValidation {

        @Test
        @DisplayName("Should throw DuplicateEntityException when tutor email already exists")
        void shouldThrowDuplicateEntityException_whenTutorEmailAlreadyExists() {
            // Given
            TutorCreationRequestDTO tutorDto = new TutorCreationRequestDTO(
                    LocalDate.of(1990, 1, 15),
                    "John", "Doe",
                    TEST_EMAIL, TEST_PHONE,
                    "123 Main St", "12345"
            );
            TutorDataModel tutorModel = new TutorDataModel();
            PersonPIIDataModel personPII = new PersonPIIDataModel();
            personPII.setEmail(TEST_EMAIL);
            personPII.setPhoneNumber(TEST_PHONE);

            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(tutorDto, personPII);
            when(applicationContext.getBean(TutorDataModel.class)).thenReturn(tutorModel);
            doNothing().when(modelMapper).map(tutorDto, tutorModel, TutorCreationUseCase.TUTOR_MAP_NAME);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
            when(personPIIRepository.existsByEmailHash(EMAIL_HASH)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.create(tutorDto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessage("Duplicate " + PiiField.EMAIL + " for " + EntityType.TUTOR)
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.TUTOR);
                        assertThat(dee.getField()).isEqualTo(PiiField.EMAIL);
                    });

            verify(personPIIRepository, times(1)).existsByEmailHash(EMAIL_HASH);
            verifyNoInteractions(tutorRepository, minorStudentRepository);
        }

        @Test
        @DisplayName("Should throw DuplicateEntityException when tutor phone already exists")
        void shouldThrowDuplicateEntityException_whenTutorPhoneAlreadyExists() {
            // Given
            TutorCreationRequestDTO tutorDto = new TutorCreationRequestDTO(
                    LocalDate.of(1990, 1, 15),
                    "John", "Doe",
                    TEST_EMAIL, TEST_PHONE,
                    "123 Main St", "12345"
            );
            TutorDataModel tutorModel = new TutorDataModel();
            PersonPIIDataModel personPII = new PersonPIIDataModel();
            personPII.setEmail(TEST_EMAIL);
            personPII.setPhoneNumber(TEST_PHONE);

            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(tutorDto, personPII);
            when(applicationContext.getBean(TutorDataModel.class)).thenReturn(tutorModel);
            doNothing().when(modelMapper).map(tutorDto, tutorModel, TutorCreationUseCase.TUTOR_MAP_NAME);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
            when(personPIIRepository.existsByEmailHash(EMAIL_HASH)).thenReturn(false);
            when(personPIIRepository.existsByPhoneHash(PHONE_HASH)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.create(tutorDto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessage("Duplicate " + PiiField.PHONE_NUMBER + " for " + EntityType.TUTOR)
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.TUTOR);
                        assertThat(dee.getField()).isEqualTo(PiiField.PHONE_NUMBER);
                    });

            verify(personPIIRepository, times(1)).existsByEmailHash(EMAIL_HASH);
            verify(personPIIRepository, times(1)).existsByPhoneHash(PHONE_HASH);
            verifyNoInteractions(tutorRepository, minorStudentRepository);
        }

        @Test
        @DisplayName("Should throw DuplicateEntityException when minor student email already exists")
        void shouldThrowDuplicateEntityException_whenMinorStudentEmailAlreadyExists() {
            // Given
            Long tutorId = 42L;
            MinorStudentCreationRequestDTO minorDto = new MinorStudentCreationRequestDTO(
                    LocalDate.of(2012, 6, 1),
                    tutorId,
                    "Jane", "Doe",
                    TEST_EMAIL, TEST_PHONE,
                    "456 Oak Ave", "67890",
                    "GOOGLE", "oauth_token123"
            );
            MinorStudentDataModel minorModel = new MinorStudentDataModel();
            PersonPIIDataModel personPII = new PersonPIIDataModel();
            personPII.setEmail(TEST_EMAIL);
            personPII.setPhoneNumber(TEST_PHONE);
            TutorDataModel existingTutor = new TutorDataModel();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(minorDto, personPII);
            when(applicationContext.getBean(MinorStudentDataModel.class)).thenReturn(minorModel);
            doNothing().when(modelMapper).map(minorDto, minorModel, TutorCreationUseCase.MINOR_STUDENT_MAP_NAME);
            when(applicationContext.getBean(CustomerAuthDataModel.class)).thenReturn(new CustomerAuthDataModel());
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
            when(tutorRepository.findById(new TutorDataModel.TutorCompositeId(TENANT_ID, tutorId))).thenReturn(Optional.of(existingTutor));
            when(personPIIRepository.existsByEmailHash(EMAIL_HASH)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.createMinorStudent(minorDto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessage("Duplicate " + PiiField.EMAIL + " for " + EntityType.MINOR_STUDENT)
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.MINOR_STUDENT);
                        assertThat(dee.getField()).isEqualTo(PiiField.EMAIL);
                    });

            verify(personPIIRepository, times(1)).existsByEmailHash(EMAIL_HASH);
            verifyNoInteractions(minorStudentRepository);
        }

        @Test
        @DisplayName("Should throw DuplicateEntityException when minor student phone already exists")
        void shouldThrowDuplicateEntityException_whenMinorStudentPhoneAlreadyExists() {
            // Given
            Long tutorId = 42L;
            MinorStudentCreationRequestDTO minorDto = new MinorStudentCreationRequestDTO(
                    LocalDate.of(2012, 6, 1),
                    tutorId,
                    "Jane", "Doe",
                    TEST_EMAIL, TEST_PHONE,
                    "456 Oak Ave", "67890",
                    "GOOGLE", "oauth_token123"
            );
            MinorStudentDataModel minorModel = new MinorStudentDataModel();
            PersonPIIDataModel personPII = new PersonPIIDataModel();
            personPII.setEmail(TEST_EMAIL);
            personPII.setPhoneNumber(TEST_PHONE);
            TutorDataModel existingTutor = new TutorDataModel();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(minorDto, personPII);
            when(applicationContext.getBean(MinorStudentDataModel.class)).thenReturn(minorModel);
            doNothing().when(modelMapper).map(minorDto, minorModel, TutorCreationUseCase.MINOR_STUDENT_MAP_NAME);
            when(applicationContext.getBean(CustomerAuthDataModel.class)).thenReturn(new CustomerAuthDataModel());
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
            when(tutorRepository.findById(new TutorDataModel.TutorCompositeId(TENANT_ID, tutorId))).thenReturn(Optional.of(existingTutor));
            when(personPIIRepository.existsByEmailHash(EMAIL_HASH)).thenReturn(false);
            when(personPIIRepository.existsByPhoneHash(PHONE_HASH)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.createMinorStudent(minorDto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessage("Duplicate " + PiiField.PHONE_NUMBER + " for " + EntityType.MINOR_STUDENT)
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.MINOR_STUDENT);
                        assertThat(dee.getField()).isEqualTo(PiiField.PHONE_NUMBER);
                    });

            verify(personPIIRepository, times(1)).existsByEmailHash(EMAIL_HASH);
            verify(personPIIRepository, times(1)).existsByPhoneHash(PHONE_HASH);
            verifyNoInteractions(minorStudentRepository);
        }
    }
}
