package com.akademiaplus.customer.tutor.usecases;

import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
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
    @Mock private ModelMapper modelMapper;
    @Mock private HashingService hashingService;
    @Mock private PiiNormalizer piiNormalizer;

    private TutorCreationUseCase useCase;

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
            personPII.setPhone(TEST_PHONE);
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
            verify(applicationContext).getBean(TutorDataModel.class);
            verify(applicationContext).getBean(PersonPIIDataModel.class);
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
            verify(piiNormalizer).normalizeEmail(TEST_EMAIL);
            verify(hashingService).generateHash(NORMALIZED_EMAIL);
            verify(piiNormalizer).normalizePhoneNumber(TEST_PHONE);
            verify(hashingService).generateHash(NORMALIZED_PHONE);
            assertThat(personPII.getEmailHash()).isEqualTo(EMAIL_HASH);
            assertThat(personPII.getPhoneHash()).isEqualTo(PHONE_HASH);
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
            personPII.setPhone(TEST_PHONE);
            existingTutor = new TutorDataModel();
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
            when(tutorRepository.findById(TUTOR_ID)).thenReturn(Optional.of(existingTutor));

            // When
            MinorStudentDataModel result = useCase.transformMinorStudent(minorDto);

            // Then
            assertThat(result.getPersonPII()).isEqualTo(personPII);
            assertThat(result.getEntryDate()).isEqualTo(LocalDate.now());
            assertThat(result.getCustomerAuth()).isEqualTo(customerAuth);
            assertThat(result.getCustomerAuth().getProvider()).isEqualTo("GOOGLE");
            assertThat(result.getCustomerAuth().getToken()).isEqualTo("oauth_token123");
            verify(applicationContext).getBean(MinorStudentDataModel.class);
            verify(applicationContext).getBean(PersonPIIDataModel.class);
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
            when(tutorRepository.findById(TUTOR_ID)).thenReturn(Optional.of(existingTutor));

            // When
            MinorStudentDataModel result = useCase.transformMinorStudent(minorDto);

            // Then
            verify(tutorRepository).findById(TUTOR_ID);
            assertThat(result.getTutor()).isEqualTo(existingTutor);
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
            when(tutorRepository.findById(TUTOR_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.transformMinorStudent(minorDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(TUTOR_ID));
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
            when(tutorRepository.findById(TUTOR_ID)).thenReturn(Optional.of(existingTutor));

            // When
            useCase.transformMinorStudent(minorDto);

            // Then
            verify(piiNormalizer).normalizeEmail(TEST_EMAIL);
            verify(hashingService).generateHash(NORMALIZED_EMAIL);
            verify(piiNormalizer).normalizePhoneNumber(TEST_PHONE);
            verify(hashingService).generateHash(NORMALIZED_PHONE);
            assertThat(personPII.getEmailHash()).isEqualTo(EMAIL_HASH);
            assertThat(personPII.getPhoneHash()).isEqualTo(PHONE_HASH);
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
            personPII.setPhone(TEST_PHONE);
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
            when(tutorRepository.save(tutorModel)).thenReturn(savedTutor);
            when(modelMapper.map(savedTutor, TutorCreationResponseDTO.class)).thenReturn(expectedResponse);

            // When
            TutorCreationResponseDTO result = useCase.create(tutorDto);

            // Then
            verify(tutorRepository).save(tutorModel);
            assertThat(result).isEqualTo(expectedResponse);
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
            personPII.setPhone(TEST_PHONE);
            TutorDataModel existingTutor = new TutorDataModel();
            CustomerAuthDataModel customerAuth = new CustomerAuthDataModel();
            MinorStudentDataModel savedMinor = new MinorStudentDataModel();
            MinorStudentCreationResponseDTO expectedResponse = new MinorStudentCreationResponseDTO();

            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
            doNothing().when(modelMapper).map(minorDto, personPII);
            when(applicationContext.getBean(MinorStudentDataModel.class)).thenReturn(minorModel);
            doNothing().when(modelMapper).map(minorDto, minorModel, TutorCreationUseCase.MINOR_STUDENT_MAP_NAME);
            when(applicationContext.getBean(CustomerAuthDataModel.class)).thenReturn(customerAuth);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
            when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(existingTutor));
            when(minorStudentRepository.save(minorModel)).thenReturn(savedMinor);
            when(modelMapper.map(savedMinor, MinorStudentCreationResponseDTO.class)).thenReturn(expectedResponse);

            // When
            MinorStudentCreationResponseDTO result = useCase.createMinorStudent(minorDto);

            // Then
            verify(minorStudentRepository).save(minorModel);
            assertThat(result).isEqualTo(expectedResponse);
        }
    }
}
