# Mobile API Gaps — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Spec**: `docs/workflows/pending/mobile-api-gaps-workflow.md`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and `DESIGN.md` before starting.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (Phase 1 → 2 → 3 → 4).
2. Do NOT skip ahead. Each phase must compile before the next begins.
3. After EACH phase, run the specified verification command. Fix failures before proceeding.
4. All new files MUST include the ElatusDev copyright header.
5. All `public` classes and methods MUST have Javadoc.
6. ALL string literals → `public static final` constants, shared between impl and tests.
7. Commit after each phase using the exact commit message provided.
8. **OpenAPI first** — always update the YAML spec and regenerate before writing Java code.
9. **Query filters are optional** — when the param is absent, return all (existing behavior).
10. **No new Maven modules** — all changes go into existing modules.

---

## Phase 1: Student Self-Lookup (`/me` endpoint)

### Read first

```bash
# Understand JWT/auth context resolution
find security/src/main/java -name "*.java" | head -20
grep -rn "SecurityContextHolder\|Principal\|Authentication" security/src/main/java/ | head -20
grep -rn "SecurityContextHolder\|Principal\|Authentication" user-management/src/main/java/ | head -20

# Understand user entity structure
cat user-management/src/main/resources/openapi/adult-student-api.yaml
cat multi-tenant-data/src/main/java/com/akademiaplus/usermanagement/AdultStudentDataModel.java

# Reference: existing controller pattern
cat user-management/src/main/java/com/akademiaplus/*/interfaceadapters/*Controller.java | head -80

# Check how provider/token fields are used on student entities
grep -rn "provider\|token\|oauth\|firebase" multi-tenant-data/src/main/java/com/akademiaplus/usermanagement/ | head -20
```

Determine how the JWT principal maps to a student or tutor record. The mapping strategy
depends on what's already in the codebase.

### Step 1.1: Investigate mapping

Look at the JWT claims. Determine which field (email, provider+token, or a userId) links
the authenticated principal to the student/tutor entity. This determines the query strategy.

### Step 1.2: Update OpenAPI spec

**Edit file:** User-management module OpenAPI spec (find the exact file from Step 1 reads).

Add a new path:

```yaml
  /me:
    get:
      operationId: getCurrentUser
      summary: Get the currently authenticated user's profile
      description: >
        Resolves the authenticated user from the JWT and returns their
        student or tutor profile.
      tags:
        - Current User
      responses:
        '200':
          description: User profile found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/GetCurrentUserResponse'
        '404':
          description: No student/tutor profile found for authenticated user
```

Add the response schema. Design decision: either a wrapper DTO with a discriminator
(`userType`: `ADULT_STUDENT` | `MINOR_STUDENT` | `TUTOR`) or reuse the existing response
DTOs. Check what pattern is cleanest given the existing generated DTOs.

### Step 1.3: Generate DTOs

```bash
mvn clean generate-sources -pl user-management -am -DskipTests
find user-management/target/generated-sources -name "*CurrentUser*" -o -name "*MeApi*" | sort
```

### Step 1.4: Create `GetCurrentUserUseCase`

**Create file:** `user-management/src/main/java/com/akademiaplus/<package>/usecases/GetCurrentUserUseCase.java`

Implementation:
- Extract principal from `SecurityContextHolder.getContext().getAuthentication()`
- Resolve the user's email or provider ID from the JWT claims
- Query the adult-student, tutor, or minor-student repository
- Map to response DTO
- Throw `EntityNotFoundException` if no profile exists

Constants:
- `public static final String ERROR_USER_NOT_FOUND = "No student or tutor profile found for authenticated user"`

### Step 1.5: Wire into controller

Either add a method to the existing user-management controller or create a dedicated
`CurrentUserController` at `/v1/user-management` that implements the generated API interface.

### Step 1.6: Unit tests

**Create file:** `GetCurrentUserUseCaseTest.java`

```
@DisplayName("GetCurrentUserUseCase")
class GetCurrentUserUseCaseTest {

    @Nested @DisplayName("getCurrentUser")
    class GetCurrentUser {
        @Test shouldReturnAdultStudentProfile_whenAuthenticatedAsAdultStudent()
        @Test shouldReturnTutorProfile_whenAuthenticatedAsTutor()
        @Test shouldThrowEntityNotFoundException_whenNoProfileExists()
    }
}
```

### Verify Phase 1

```bash
mvn clean compile -pl user-management -am -DskipTests
mvn test -pl user-management -am
```

### Commit Phase 1

```bash
git add -A
git commit -m "feat(user-management): add GET /me endpoint for student self-lookup

Resolve authenticated user from JWT and return their student or tutor
profile. Enables mobile app to load profile without client-supplied ID.

Closes: mobile-api-gap-1"
```

---

## Phase 2: Query Parameter Filters (Gaps 2, 3, 4)

These three gaps follow the same pattern. Execute them sequentially within this phase.

### Read first

```bash
# Course events
cat course-management/src/main/resources/openapi/*.yaml | head -100
cat course-management/src/main/java/com/akademiaplus/*/usecases/Get*CourseEvent*.java
cat course-management/src/main/java/com/akademiaplus/*/interfaceadapters/*Repository.java
grep -rn "attendeeIds\|adultAttendee\|minorAttendee" multi-tenant-data/src/main/java/ | head -10

# Payments
cat billing/src/main/resources/openapi/*.yaml | head -100
cat billing/src/main/java/com/akademiaplus/*/usecases/Get*Payment*.java
cat billing/src/main/java/com/akademiaplus/*/interfaceadapters/*Repository.java

# Notifications
cat notification-system/src/main/resources/openapi/*.yaml | head -100
cat notification-system/src/main/java/com/akademiaplus/*/usecases/Get*Notification*.java
cat notification-system/src/main/java/com/akademiaplus/*/interfaceadapters/*Repository.java
```

### Gap 2: attendeeId filter on course-events

#### Step 2.1: Update OpenAPI spec

**Edit file:** Course-management OpenAPI spec.

Add optional query parameter `attendeeId` (integer, format: int64) to the existing
`GET /course-events` operation.

#### Step 2.2: Regenerate DTOs

```bash
mvn clean generate-sources -pl course-management -am -DskipTests
```

#### Step 2.3: Add repository method

**Edit file:** Course event repository.

Add a query method that filters events by attendee ID. The exact approach depends on
how attendee IDs are stored:

- If `@ElementCollection`: use `@Query` with JOIN on the collection table
- If `@ManyToMany`: use repository derived query
- If JSON column: use MariaDB `JSON_CONTAINS`

#### Step 2.4: Modify use case

**Edit file:** `GetAllCourseEventsUseCase.java`

Add optional `Long attendeeId` parameter. When non-null, call the filtered repository
method. When null, call the existing `findAll()`.

Update the controller to pass the query parameter through.

#### Step 2.5: Unit tests

Add test methods to the existing `GetAllCourseEventsUseCaseTest`:

```
@Test shouldFilterByAttendeeId_whenAttendeeIdProvided()
@Test shouldReturnAll_whenAttendeeIdIsNull()
```

---

### Gap 3: adultStudentId filter on payments

Same pattern as Gap 2. Apply to `billing` module:

1. Update OpenAPI spec — add `adultStudentId` query param to `GET /paymentAdultStudents`
2. Regenerate DTOs
3. Add repository method: filter by the association chain (payment → membershipAdultStudent → adultStudentId)
4. Modify use case to accept optional filter
5. Add tests

Also add `tutorId` param to `GET /paymentTutors` following the same pattern.

---

### Gap 4: targetUserId filter on notifications

Same pattern as Gap 2. Apply to `notification-system` module:

1. Update OpenAPI spec — add `targetUserId` query param to `GET /notifications`
2. Regenerate DTOs
3. Add repository method: `findByTargetUserId(Long targetUserId)`
4. Modify use case to accept optional filter
5. Add tests

---

### Verify Phase 2

```bash
mvn clean compile -pl course-management,billing,notification-system -am -DskipTests
mvn test -pl course-management -am
mvn test -pl billing -am
mvn test -pl notification-system -am
```

### Commit Phase 2

```bash
git add -A
git commit -m "feat(api): add student-scoped query filters for mobile app

Add optional query parameters:
- attendeeId on GET /course-events (course-management)
- adultStudentId on GET /paymentAdultStudents (billing)
- tutorId on GET /paymentTutors (billing)
- targetUserId on GET /notifications (notification-system)

All filters are optional — existing behavior preserved when absent.

Closes: mobile-api-gap-2, mobile-api-gap-3, mobile-api-gap-4"
```

---

## Phase 3: Device Token Registration (Gap 5)

### Read first

```bash
# Existing push notification structure
find notification-system/src/main/java -name "*.java" | sort
cat notification-system/src/main/resources/openapi/*.yaml | head -150
cat notification-system/src/main/java/com/akademiaplus/*/interfaceadapters/*Controller.java

# Entity patterns for platform-level entities
cat multi-tenant-data/src/main/java/com/akademiaplus/tenancy/TenantDataModel.java | head -40
cat infra-common/src/main/java/com/akademiaplus/infra/persistence/model/Auditable.java

# Current schema
tail -30 db_init/00-schema-qa.sql
```

### Step 3.1: Create `PushDeviceDataModel`

**Create file:** `multi-tenant-data/src/main/java/com/akademiaplus/notificationsystem/PushDeviceDataModel.java`

Platform-level entity (extends `Auditable`, NOT `TenantScoped`):
- `pushDeviceId`: Long PK, auto-increment
- `userId`: Long, NOT NULL
- `deviceToken`: String(512), NOT NULL, unique
- `platform`: String(10), NOT NULL — "IOS" or "ANDROID"
- `appVersion`: String(20), nullable

### Step 3.2: Add table DDL

**Edit file:** `db_init/00-schema-qa.sql`

Add the `push_devices` table (see workflow spec for exact DDL).

### Step 3.3: Add OpenAPI spec

**Edit file:** Push notification OpenAPI spec (or create a new spec file for device management).

Add paths:
- `POST /push/devices` — register device
- `DELETE /push/devices/{deviceToken}` — unregister device

Add schemas:
- `RegisterPushDeviceRequest`: userId, deviceToken, platform, appVersion
- `RegisterPushDeviceResponse`: pushDeviceId

### Step 3.4: Generate DTOs

```bash
mvn clean generate-sources -pl notification-system -am -DskipTests
```

### Step 3.5: Create repository

**Create file:** `PushDeviceRepository.java`

```java
@Repository
public interface PushDeviceRepository extends JpaRepository<PushDeviceDataModel, Long> {
    Optional<PushDeviceDataModel> findByDeviceToken(String deviceToken);
    List<PushDeviceDataModel> findByUserId(Long userId);
    void deleteByDeviceToken(String deviceToken);
}
```

### Step 3.6: Create `RegisterPushDeviceUseCase`

Upsert logic:
1. `findByDeviceToken(token)` — if exists, update userId + platform
2. If not exists, create new entity
3. Save and return response

### Step 3.7: Create `UnregisterPushDeviceUseCase`

1. `deleteByDeviceToken(token)`
2. No error if token doesn't exist (idempotent)

### Step 3.8: Wire into controller

Add endpoints to the existing push notification controller or create a dedicated device controller.

### Step 3.9: Unit tests

```
RegisterPushDeviceUseCaseTest:
  shouldRegisterNewDevice_whenTokenNotExists()
  shouldUpdateExistingDevice_whenTokenAlreadyExists()

UnregisterPushDeviceUseCaseTest:
  shouldDeleteDevice_whenTokenExists()
  shouldNotThrow_whenTokenNotExists()
```

### Verify Phase 3

```bash
mvn clean compile -pl notification-system -am -DskipTests
mvn test -pl notification-system -am
```

### Commit Phase 3

```bash
git add -A
git commit -m "feat(notification-system): add push device token registration

Add PushDeviceDataModel for FCM token storage. POST /push/devices
registers or updates a device token (upsert). DELETE /push/devices/{token}
removes registration. Platform-level entity (not tenant-scoped).

Closes: mobile-api-gap-5"
```

---

## Phase 4: Notification Read Status (Gap 6)

### Read first

```bash
# Notification entity structure
cat multi-tenant-data/src/main/java/com/akademiaplus/notificationsystem/NotificationDataModel.java
cat notification-system/src/main/java/com/akademiaplus/*/usecases/Get*Notification*.java
```

### Step 4.1: Create `NotificationReadStatusDataModel`

**Create file:** `multi-tenant-data/src/main/java/com/akademiaplus/notificationsystem/NotificationReadStatusDataModel.java`

Platform-level entity:
- `notificationReadStatusId`: Long PK, auto-increment
- `notificationId`: Long, NOT NULL
- `userId`: Long, NOT NULL
- `readAt`: Timestamp, default CURRENT_TIMESTAMP
- Unique constraint: (notificationId, userId)

### Step 4.2: Add table DDL

**Edit file:** `db_init/00-schema-qa.sql`

Add the `notification_read_statuses` table (see workflow spec for exact DDL).

### Step 4.3: Add OpenAPI spec

**Edit file:** Notification OpenAPI spec.

Add path:
- `PATCH /notifications/{notificationId}/read` — mark as read (idempotent)

Add `isRead` boolean field to the notification response schema.

### Step 4.4: Generate DTOs

```bash
mvn clean generate-sources -pl notification-system -am -DskipTests
```

### Step 4.5: Create repository

**Create file:** `NotificationReadStatusRepository.java`

```java
@Repository
public interface NotificationReadStatusRepository
        extends JpaRepository<NotificationReadStatusDataModel, Long> {
    boolean existsByNotificationIdAndUserId(Long notificationId, Long userId);
    List<NotificationReadStatusDataModel> findByUserIdAndNotificationIdIn(
            Long userId, List<Long> notificationIds);
}
```

### Step 4.6: Create `MarkNotificationAsReadUseCase`

1. Check if read-status already exists for (notificationId, userId)
2. If not exists, create and save
3. If exists, return success (idempotent)

### Step 4.7: Modify `GetAllNotificationsUseCase`

When `targetUserId` is provided (from Phase 2 Gap 4), also query read-statuses and
populate the `isRead` flag on each notification response DTO.

### Step 4.8: Wire into controller

Add the PATCH endpoint to the existing notification controller.

### Step 4.9: Unit tests

```
MarkNotificationAsReadUseCaseTest:
  shouldCreateReadStatus_whenNotAlreadyRead()
  shouldNotThrow_whenAlreadyRead()

GetAllNotificationsUseCaseTest (additional tests):
  shouldPopulateIsRead_whenTargetUserIdProvided()
  shouldLeaveIsReadNull_whenTargetUserIdAbsent()
```

### Verify Phase 4

```bash
mvn clean compile -pl notification-system -am -DskipTests
mvn test -pl notification-system -am
```

### Commit Phase 4

```bash
git add -A
git commit -m "feat(notification-system): add notification read status tracking

Add NotificationReadStatusDataModel for per-user read receipts.
PATCH /notifications/{id}/read marks a notification as read (idempotent).
GET /notifications now includes isRead flag when targetUserId is provided.

Closes: mobile-api-gap-6"
```

---

## Final Verification

```bash
# Full project build with all tests
mvn clean install

# Convention compliance
grep -rn "any()" user-management/src/test/ course-management/src/test/ billing/src/test/ notification-system/src/test/ | grep -v "/target/" && echo "FAIL" || echo "PASS"
grep -rn '".*not found.*"' user-management/src/main/ notification-system/src/main/ | grep -v "static final" | grep -v "/target/" && echo "FAIL" || echo "PASS"
```

---

## Post-Execution Checklist

- [ ] `mvn clean install` passes (all modules)
- [ ] `GET /v1/user-management/me` returns authenticated user's profile
- [ ] `GET /course-events?attendeeId=X` filters correctly
- [ ] `GET /paymentAdultStudents?adultStudentId=X` filters correctly
- [ ] `GET /notifications?targetUserId=X` filters correctly and includes `isRead`
- [ ] `POST /push/devices` upserts device token
- [ ] `DELETE /push/devices/{token}` removes device token
- [ ] `PATCH /notifications/{id}/read` is idempotent
- [ ] All query filters return full results when parameter is absent
- [ ] All constants extracted — no inline string literals
- [ ] Tests use Given-When-Then, zero `any()` matchers
- [ ] Commits follow Conventional Commits format
