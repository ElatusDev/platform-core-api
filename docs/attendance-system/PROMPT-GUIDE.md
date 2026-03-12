# Attendance System — AI-Assisted Development Prompt Guide

## Architecture Decisions Rationale

### Why Animated QR codes?
- **Replay-resistant**: Tokens rotate every ~15 seconds, preventing screenshot sharing
- **Offline-capable**: Deterministic HMAC computation — no real-time server round-trip for token generation
- **Low friction**: Students just scan; no hardware installation needed (unlike NFC/BLE)
- **Extensible**: Strategy pattern allows adding NFC/BLE/manual without modifying core logic

### Why a separate module?
- Clean separation of concerns from course-management
- Independent release cycle for attendance features
- Avoids polluting existing M2M junction tables with check-in metadata

### Why HMAC-SHA256 for tokens?
- Deterministic: both server and client compute the same token for the same time window
- No nonce storage needed server-side
- Cryptographically strong — cannot be forged without the session secret
- Compact output suitable for QR encoding

---

## Prompts for Common Extension Tasks

### Adding a New Verification Method (e.g., NFC)

```
I need to add NFC support to the attendance-system module.

1. Add `NFC` to the `VerificationMethod` enum in multi-tenant-data
   (already present — just needs a strategy implementation).

2. Create `NfcVerificationStrategy` in attendance-system following the pattern
   of `AnimatedQrVerificationStrategy`:
   - Implement `AttendanceVerificationStrategy`
   - `getMethod()` returns `VerificationMethod.NFC`
   - `verify(token, session)` validates the NFC payload
     (define the NFC token format)
   - Add `@Service` annotation

3. No changes needed to `CheckInUseCase` — it resolves strategies dynamically
   by `verificationMethod` from the request.

4. Write unit tests following `AnimatedQrVerificationStrategyTest` pattern:
   - shouldDoX_whenY() naming
   - ZERO any() matchers
   - @DisplayName on all tests
   - All string literals as static final constants

Reference files:
- AnimatedQrVerificationStrategy (strategy pattern)
- DeliveryChannelStrategy in notification-system (similar interface design)
- AttendanceVerificationStrategy (the interface to implement)
```

### Adding Token Expiration / Session Auto-Close

```
I need the attendance system to auto-close sessions after a configurable duration.

1. Add a `maxDurationMinutes` column to `attendance_sessions` table
   (and both schema files + test schemas).

2. Add the field to `AttendanceSessionDataModel`.

3. Update `AttendanceSessionScheduler` to also check for sessions where:
   - status = ACTIVE
   - startedAt + maxDurationMinutes < now
   And close them automatically.

4. Update the `AttendanceSessionCreationUseCase` to accept the duration
   from the request or use a default.

5. Add the field to the OpenAPI spec (`AttendanceSessionCreateRequest`).

Reference files:
- AttendanceSessionScheduler (scheduler pattern)
- ScheduledNotificationDispatcher (cross-tenant scheduler reference)
- db_init_dev/00-schema.sql and db_init_qa/00-schema.sql (schema change pattern)
```

### Adding Geolocation Validation

```
I need to validate that students are physically near the classroom
when checking in.

1. Add `latitude` and `longitude` columns to `attendance_sessions`
   (classroom coordinates set when session starts).

2. Add `latitude` and `longitude` to the `CheckInRequest` OpenAPI schema.

3. In `CheckInUseCase`, after token validation, compute the Haversine
   distance between the session location and the student's reported location.
   Reject if distance > configurable threshold (e.g., 100 meters).

4. Create a `GeolocationValidator` utility class for the distance computation.

5. Add a new exception: `StudentTooFarFromClassroomException` (403).

Reference files:
- CheckInUseCase (validation chain to extend)
- AttendanceControllerAdvice (exception handler registration)
- course-event.yaml (OpenAPI schema extension pattern)
```

### Writing Tests for a New Use Case

```
I need to write unit tests for [NewUseCaseName] in the attendance-system module.

Follow these conventions exactly:
- File: attendance-system/src/test/java/com/akademiaplus/attendance/usecases/[NewUseCaseName]Test.java
- @DisplayName("[NewUseCaseName]") on the class
- @ExtendWith(MockitoExtension.class)
- All dependencies as @Mock fields
- Use case instantiated in @BeforeEach via constructor (not Spring)
- ALL test data as `private static final` constants
- Method naming: shouldDoX_whenY()
- @DisplayName on EVERY @Test and @Nested
- ZERO any() matchers — use exact argument values
- Void ModelMapper: doNothing().when(modelMapper).map(dto, model, MAP_NAME)
- @Nested classes to group: Transformation, Persistence, Validation
- Given/When/Then comments in each test
- InOrder to verify operation sequence where relevant
- lenient() for stubs not always called

Reference files:
- CourseEventCreationUseCaseTest (creation use case test pattern)
- AttendanceSessionCreationUseCaseTest (same module reference)
- CheckInUseCaseTest (validation chain testing)
```

### Writing Controller Tests

```
I need to write controller tests for [ControllerName] in the attendance-system module.

Follow these conventions:
- Standalone MockMvc setup (no full Spring context)
- Controller + AttendanceControllerAdvice wired in @BeforeEach
- MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(advice).build()
- BASE_PATH constant matching the controller's @RequestMapping
- @Nested classes grouping by HTTP method + path
- Verify both HTTP status AND that the correct use case was called
- verifyNoMoreInteractions() after assertions
- shouldDoX_whenY() naming + @DisplayName

Reference files:
- CourseEventControllerTest (standalone MockMvc pattern)
- AttendanceSessionControllerTest (same module reference)
```

### Writing Integration Tests

```
I need to write an integration test for the attendance check-in flow.

Follow these conventions:
- Extend module's AbstractIntegrationTest
- @AutoConfigureMockMvc, @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
- Static flags for one-time data setup (dataCreated pattern)
- @BeforeEach creates test tenant + tenant_sequences entries
- @Order annotations to enforce test execution order
- Full HTTP requests via MockMvc
- Verify response status codes AND JSON structure
- Use @DisplayName on class and all @Test methods

Critical setup:
- Insert tenant_sequences rows for EVERY entity type used:
  - attendance_session, attendance_record, course_event, course, schedule, etc.
- Set TenantContextHolder before each test method
- Create prerequisite data (tenant, course, schedule, course_event)
  before testing attendance operations

Reference files:
- CourseComponentTest (component test lifecycle)
- infra-common AbstractIntegrationTest (base class)
```

### Adding the Module to CI/CD

```
The attendance-system module needs to be included in CI/CD.

1. Verify it's listed in parent pom.xml <modules> section
2. Verify it's listed in the `platform-core-api` profile <modules>
3. Verify application/pom.xml has the dependency
4. Run full build: mvn clean install -f platform-core-api/pom.xml -DskipITs
5. Check SonarCloud picks up the new module (sonar.exclusions
   already covers generated DTOs via wildcard pattern)
```

---

## Module File Structure Reference

```
attendance-system/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/akademiaplus/
    │   │   ├── attendance/
    │   │   │   ├── interfaceadapters/
    │   │   │   │   ├── AttendanceSessionRepository.java
    │   │   │   │   ├── AttendanceRecordRepository.java
    │   │   │   │   ├── AttendanceSessionController.java
    │   │   │   │   └── AttendanceRecordController.java
    │   │   │   └── usecases/
    │   │   │       ├── AttendanceSessionCreationUseCase.java
    │   │   │       ├── CloseAttendanceSessionUseCase.java
    │   │   │       ├── GetAttendanceSessionUseCase.java
    │   │   │       ├── QrTokenGenerationUseCase.java
    │   │   │       ├── CheckInUseCase.java
    │   │   │       ├── GetAttendanceRecordsUseCase.java
    │   │   │       ├── AttendanceSessionScheduler.java
    │   │   │       ├── AttendanceVerificationStrategy.java
    │   │   │       └── AnimatedQrVerificationStrategy.java
    │   │   ├── config/
    │   │   │   ├── AttendanceModelMapperConfiguration.java
    │   │   │   ├── AttendanceModuleSecurityConfiguration.java
    │   │   │   ├── AttendanceControllerAdvice.java
    │   │   │   └── AttendanceSchedulingConfiguration.java
    │   │   └── exception/
    │   │       ├── AttendanceSessionNotFoundException.java
    │   │       ├── AttendanceSessionClosedException.java
    │   │       ├── StudentAlreadyCheckedInException.java
    │   │       ├── InvalidQrTokenException.java
    │   │       └── StudentNotEnrolledException.java
    │   └── resources/
    │       ├── openapi/
    │       │   ├── attendance-system-module.yaml
    │       │   ├── attendance-session.yaml
    │       │   └── attendance-record.yaml
    │       └── attendance.properties
    └── test/
        ├── java/com/akademiaplus/attendance/
        │   ├── usecases/
        │   │   ├── AttendanceSessionCreationUseCaseTest.java
        │   │   ├── CloseAttendanceSessionUseCaseTest.java
        │   │   ├── GetAttendanceSessionUseCaseTest.java
        │   │   ├── QrTokenGenerationUseCaseTest.java
        │   │   ├── CheckInUseCaseTest.java
        │   │   ├── GetAttendanceRecordsUseCaseTest.java
        │   │   ├── AnimatedQrVerificationStrategyTest.java
        │   │   └── AttendanceSessionSchedulerTest.java
        │   ├── interfaceadapters/
        │   │   ├── AttendanceSessionControllerTest.java
        │   │   └── AttendanceRecordControllerTest.java
        │   └── integration/
        │       ├── AbstractIntegrationTest.java
        │       ├── AttendanceSystemTestApp.java
        │       ├── AttendanceSessionComponentTest.java
        │       └── CheckInComponentTest.java
        └── resources/
            └── 00-schema-dev.sql

Entities (in multi-tenant-data module):
  multi-tenant-data/src/main/java/com/akademiaplus/attendance/
  ├── AttendanceSessionDataModel.java
  ├── AttendanceRecordDataModel.java
  ├── AttendanceSessionStatus.java
  ├── StudentType.java
  └── VerificationMethod.java
```

---

## Key Constants and Naming Conventions

| Item | Convention | Example |
|------|-----------|---------|
| Maven module name | `attendance-system` | `<artifactId>attendance-system</artifactId>` |
| Module property | `attendance.system` | `<module.name>attendance.system</module.name>` |
| Generated DTO package | `openapi.akademiaplus.domain.attendance.system.dto` | `AttendanceSessionCreateRequestDTO` |
| Generated API package | `openapi.akademiaplus.domain.attendance.system.api` | `SessionsApi` |
| Base URL | `/v1/attendance-system` | `@RequestMapping("/v1/attendance-system")` |
| Properties file | `attendance.properties` | `api.attendance.session.base-url=/v1/attendance-system/sessions` |
| TypeMap names | `public static final String MAP_NAME` | `"attendanceSessionMap"` |
| SQL table prefix | `attendance_` | `attendance_sessions`, `attendance_records` |
| Entity composite IDs | Inner static class | `AttendanceSessionCompositeId` |

---

## Dependencies Between Modules

```
                    ┌──────────────────┐
                    │  attendance-system │
                    └────────┬─────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
   ┌────────▼──────┐ ┌──────▼───────┐ ┌──────▼──────┐
   │multi-tenant-  │ │   course-    │ │  security   │
   │    data       │ │ management   │ │             │
   └───────────────┘ └──────────────┘ └─────────────┘
```

- **multi-tenant-data**: Entity classes (`AttendanceSessionDataModel`, `AttendanceRecordDataModel`, `CourseEventDataModel`)
- **course-management**: `ScheduleRepository.findActiveByDayAndTime()` (new query for scheduler)
- **security**: `ModuleSecurityConfigurator` interface, `Role` enum
- **utilities**: `TenantScopedRepository`, `BaseControllerAdvice`, `MessageService`
- **infra-common**: `TenantContextHolder`, `TenantScoped` base class
