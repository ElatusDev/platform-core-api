# Mobile API Gaps Workflow — platform-core-api

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and `DESIGN.md` before starting.
**Blocks**: `platform/mobile-app` Stage 1 — cannot begin until all gaps are resolved and tested.

---

## 1. Objective

Add six API capabilities required by the upcoming React Native student mobile app.
These are **read-heavy, student-facing** endpoints. The mobile app consumes `core-api`
exclusively — no new modules are needed; changes go into existing modules.

### Motivation

The current API was designed for admin SPAs. Students querying their own data need:
- Self-lookup from JWT (no client-supplied ID)
- Filtered queries scoped to a single student
- Device token management for push notifications
- Read-status tracking for notifications

---

## 2. Scope

### In Scope

| # | Gap | Module | Type |
|---|-----|--------|------|
| 1 | `GET /v1/user-management/me` — resolve student from JWT | `user-management` | New endpoint |
| 2 | `GET /v1/course-management/course-events?attendeeId={id}` | `course-management` | Query param |
| 3 | `GET /v1/billing/paymentAdultStudents?adultStudentId={id}` | `billing` | Query param |
| 4 | `GET /v1/notification-system/notifications?targetUserId={id}` | `notification-system` | Query param |
| 5 | `POST /v1/notification-system/push/devices` — FCM token registration | `notification-system` | New endpoint + entity |
| 6 | `PATCH /v1/notification-system/notifications/{id}/read` — mark as read | `notification-system` | New endpoint + field |

### Out of Scope

- Mobile app code (separate workflow)
- New modules — all changes in existing modules
- Admin-facing endpoints (existing CRUD is unchanged)
- OAuth / social login changes (handled by existing Firebase flow)
- E2E Postman tests (separate follow-up)

---

## 3. Gap Specifications

### Gap 1: Student Self-Lookup (`/me`)

**Module**: `user-management`

**Endpoint**: `GET /v1/user-management/me`
**Auth**: Bearer JWT (required)
**Response**: Same as `GET /v1/user-management/adult-students/{id}` or tutor/minor response

**Logic**:
1. Extract authenticated user ID from `SecurityContextHolder` (or the JWT claims resolver used in the project)
2. Look up the user by their auth-provider ID or internal user mapping
3. Determine user type (adult student, tutor, minor student) and return the appropriate profile
4. Return 404 if no student/tutor profile exists for the authenticated user

**OpenAPI additions**:
- New path `/me` in the user-management module spec
- New response DTO `GetCurrentUserResponseDTO` with a discriminator for user type, OR reuse existing response DTOs with a wrapper

**Implementation approach**:
- New use case: `GetCurrentUserUseCase`
- Reads the principal from Spring Security context
- Queries the appropriate repository based on user type
- Maps to response DTO

**Design decision**: How the JWT maps to a student/tutor record. Options:
1. **Email match** — JWT email claim → find student/tutor by email
2. **Provider + token match** — OAuth provider ID stored on the student entity
3. **Dedicated user-profile table** — maps auth user ID to student/tutor ID

Check which pattern exists in `security` module and the student entity `provider`/`token` fields.

---

### Gap 2: Attendance Filter by Student

**Module**: `course-management`

**Endpoint**: `GET /v1/course-management/course-events?attendeeId={id}`
**Auth**: Bearer JWT (required)

**Logic**:
1. Add optional query parameter `attendeeId` (Long) to the existing `getAllCourseEvents` endpoint
2. When present, filter course events where `adultAttendeeIds` or `minorAttendeeIds` contains the given ID
3. Return standard paginated response

**OpenAPI additions**:
- Add `attendeeId` query parameter to existing `/course-events` GET operation

**Implementation approach**:
- Add repository method: custom JPQL or `@Query` that joins the attendee collections
- Modify existing `GetAllCourseEventsUseCase` to accept optional `attendeeId` parameter
- If `attendeeId` is null, existing behavior is preserved (return all)

**Considerations**:
- `adultAttendeeIds` and `minorAttendeeIds` are stored as collections on `CourseEventDataModel` — check if they are `@ElementCollection`, `@ManyToMany`, or serialized JSON
- The query strategy depends on the storage approach

---

### Gap 3: Payment Filter by Student

**Module**: `billing`

**Endpoint**: `GET /v1/billing/paymentAdultStudents?adultStudentId={id}`
**Auth**: Bearer JWT (required)

**Logic**:
1. Add optional query parameter `adultStudentId` (Long) to the existing `getAllPaymentAdultStudents` endpoint
2. When present, filter payments that belong to the given student via the membership association chain: payment → membershipAdultStudent → adultStudentId
3. Return standard response

**OpenAPI additions**:
- Add `adultStudentId` query parameter to existing `/paymentAdultStudents` GET operation

**Implementation approach**:
- Add repository method: `findByMembershipAdultStudent_AdultStudentId(Long adultStudentId)` or equivalent JPQL
- Modify existing `GetAllPaymentAdultStudentsUseCase` to accept optional `adultStudentId`
- Equivalent for tutor payments: add `tutorId` param to `GET /v1/billing/paymentTutors`

---

### Gap 4: Notification Filter by Target User

**Module**: `notification-system`

**Endpoint**: `GET /v1/notification-system/notifications?targetUserId={id}`
**Auth**: Bearer JWT (required)

**Logic**:
1. Add optional query parameter `targetUserId` (Long) to the existing `getAllNotifications` endpoint
2. When present, filter notifications where `targetUserId` matches
3. Return standard response

**OpenAPI additions**:
- Add `targetUserId` query parameter to existing `/notifications` GET operation

**Implementation approach**:
- Add repository method: `findByTargetUserId(Long targetUserId)` or modify existing query
- Modify existing `GetAllNotificationsUseCase` to accept optional `targetUserId`

---

### Gap 5: Device Token Registration

**Module**: `notification-system`

**New entity**: `PushDeviceDataModel`

```
PushDeviceDataModel {
    pushDeviceId: Long (PK, auto-increment)
    userId: Long (NOT NULL)
    deviceToken: String (NOT NULL, unique)
    platform: String (NOT NULL) — "IOS" | "ANDROID"
    appVersion: String (nullable)
    createdAt, updatedAt (from Auditable)
}
```

**Endpoints**:
- `POST /v1/notification-system/push/devices` — register or update device token
- `DELETE /v1/notification-system/push/devices/{deviceToken}` — unregister device

**Logic**:
1. On POST: upsert — if `deviceToken` already exists, update `userId` and `platform`; otherwise insert
2. On DELETE: remove the device registration
3. The existing push delivery system (`POST /push/send`) should use registered devices to resolve targets

**OpenAPI additions**:
- New path `/push/devices` with POST and DELETE operations
- New schemas: `RegisterPushDeviceRequestDTO`, `RegisterPushDeviceResponseDTO`

**Implementation approach**:
- New entity in `multi-tenant-data`: `PushDeviceDataModel`
- New repository: `PushDeviceRepository`
- New use cases: `RegisterPushDeviceUseCase`, `UnregisterPushDeviceUseCase`
- Table DDL in `db_init/00-schema-qa.sql`

**Entity classification**: Platform-level (not tenant-scoped) — device tokens belong to users across tenants.

---

### Gap 6: Notification Read Status

**Module**: `notification-system`

**Endpoint**: `PATCH /v1/notification-system/notifications/{notificationId}/read`
**Auth**: Bearer JWT (required)

**New entity**: `NotificationReadStatusDataModel`

```
NotificationReadStatusDataModel {
    notificationReadStatusId: Long (PK, auto-increment)
    notificationId: Long (NOT NULL, FK)
    userId: Long (NOT NULL)
    readAt: Timestamp (NOT NULL, default CURRENT_TIMESTAMP)
    UNIQUE(notificationId, userId)
}
```

**Logic**:
1. On PATCH: insert a read receipt for the authenticated user + notification ID
2. If already read (unique constraint), return 200 OK (idempotent)
3. Enhance `GET /notifications` response with `isRead` boolean (joined from read-status table for the requesting user)

**OpenAPI additions**:
- New path `/notifications/{notificationId}/read` with PATCH operation
- Add `isRead` field to `GetNotificationResponseDTO` (or equivalent)

**Implementation approach**:
- New entity in `multi-tenant-data`: `NotificationReadStatusDataModel`
- New repository: `NotificationReadStatusRepository`
- New use case: `MarkNotificationAsReadUseCase`
- Modify `GetAllNotificationsUseCase` to join read-status when `targetUserId` is provided
- Table DDL in `db_init/00-schema-qa.sql`

---

## 4. Implementation Sequence

```
Phase 1: Gap 1 — Student self-lookup (/me)
  ├── 1.1  Investigate JWT → student mapping pattern
  ├── 1.2  Add OpenAPI spec for /me endpoint
  ├── 1.3  Generate DTOs
  ├── 1.4  Create GetCurrentUserUseCase
  ├── 1.5  Wire into UserManagementController (or create MeController)
  ├── 1.6  Unit tests
  └── 1.7  Compile gate + commit

Phase 2: Gaps 2-4 — Query parameter filters (parallel-safe)
  ├── 2.1  Gap 2: attendeeId filter on course-events
  │   ├── Update OpenAPI spec
  │   ├── Add repository method
  │   ├── Modify GetAllCourseEventsUseCase
  │   └── Unit tests
  ├── 2.2  Gap 3: adultStudentId filter on payments
  │   ├── Update OpenAPI spec
  │   ├── Add repository method
  │   ├── Modify GetAllPaymentAdultStudentsUseCase
  │   └── Unit tests
  ├── 2.3  Gap 4: targetUserId filter on notifications
  │   ├── Update OpenAPI spec
  │   ├── Add repository method
  │   ├── Modify GetAllNotificationsUseCase
  │   └── Unit tests
  └── 2.4  Compile gate + commit

Phase 3: Gap 5 — Device token registration
  ├── 3.1  Create PushDeviceDataModel entity
  ├── 3.2  Add push_devices table DDL
  ├── 3.3  Add OpenAPI spec for /push/devices
  ├── 3.4  Generate DTOs
  ├── 3.5  Create PushDeviceRepository
  ├── 3.6  Create RegisterPushDeviceUseCase (upsert)
  ├── 3.7  Create UnregisterPushDeviceUseCase
  ├── 3.8  Wire into controller
  ├── 3.9  Unit tests
  └── 3.10 Compile gate + commit

Phase 4: Gap 6 — Notification read status
  ├── 4.1  Create NotificationReadStatusDataModel entity
  ├── 4.2  Add notification_read_statuses table DDL
  ├── 4.3  Add OpenAPI spec for /notifications/{id}/read
  ├── 4.4  Add isRead field to notification response DTO
  ├── 4.5  Generate DTOs
  ├── 4.6  Create NotificationReadStatusRepository
  ├── 4.7  Create MarkNotificationAsReadUseCase
  ├── 4.8  Modify GetAllNotificationsUseCase (join read-status)
  ├── 4.9  Wire into controller
  ├── 4.10 Unit tests
  └── 4.11 Compile gate + commit
```

---

## 5. File Inventory

### New Files

| # | File | Module | Responsibility | Phase |
|---|------|--------|----------------|-------|
| 1 | `GetCurrentUserUseCase.java` | `user-management` | Resolve student from JWT | 1 |
| 2 | `GetCurrentUserUseCaseTest.java` | `user-management` | Test | 1 |
| 3 | `PushDeviceDataModel.java` | `multi-tenant-data` | Device token entity | 3 |
| 4 | `PushDeviceRepository.java` | `notification-system` | Device CRUD | 3 |
| 5 | `RegisterPushDeviceUseCase.java` | `notification-system` | Upsert device token | 3 |
| 6 | `UnregisterPushDeviceUseCase.java` | `notification-system` | Remove device token | 3 |
| 7 | `RegisterPushDeviceUseCaseTest.java` | `notification-system` | Test | 3 |
| 8 | `UnregisterPushDeviceUseCaseTest.java` | `notification-system` | Test | 3 |
| 9 | `NotificationReadStatusDataModel.java` | `multi-tenant-data` | Read receipt entity | 4 |
| 10 | `NotificationReadStatusRepository.java` | `notification-system` | Read receipt CRUD | 4 |
| 11 | `MarkNotificationAsReadUseCase.java` | `notification-system` | Mark notification read | 4 |
| 12 | `MarkNotificationAsReadUseCaseTest.java` | `notification-system` | Test | 4 |

### Modified Files

| # | File | Module | Change | Phase |
|---|------|--------|--------|-------|
| 1 | OpenAPI spec for user-management | `user-management` | Add `/me` path | 1 |
| 2 | `UserManagementController.java` (or new controller) | `user-management` | Wire `/me` endpoint | 1 |
| 3 | OpenAPI spec for course-events | `course-management` | Add `attendeeId` param | 2 |
| 4 | `GetAllCourseEventsUseCase.java` | `course-management` | Accept optional filter | 2 |
| 5 | Course event repository | `course-management` | Add filtered query method | 2 |
| 6 | `GetAllCourseEventsUseCaseTest.java` | `course-management` | Test filtered path | 2 |
| 7 | OpenAPI spec for payments | `billing` | Add `adultStudentId` param | 2 |
| 8 | `GetAllPaymentAdultStudentsUseCase.java` | `billing` | Accept optional filter | 2 |
| 9 | Payment repository | `billing` | Add filtered query method | 2 |
| 10 | `GetAllPaymentAdultStudentsUseCaseTest.java` | `billing` | Test filtered path | 2 |
| 11 | OpenAPI spec for notifications | `notification-system` | Add `targetUserId` param | 2 |
| 12 | `GetAllNotificationsUseCase.java` | `notification-system` | Accept optional filter | 2 |
| 13 | Notification repository | `notification-system` | Add filtered query method | 2 |
| 14 | `GetAllNotificationsUseCaseTest.java` | `notification-system` | Test filtered path | 2 |
| 15 | `db_init/00-schema-qa.sql` | `db_init` | Add push_devices + notification_read_statuses tables | 3, 4 |
| 16 | OpenAPI spec for push notifications | `notification-system` | Add `/push/devices` paths | 3 |
| 17 | Push notification controller | `notification-system` | Wire device endpoints | 3 |
| 18 | OpenAPI spec for notifications | `notification-system` | Add `/read` path + `isRead` field | 4 |
| 19 | Notification controller | `notification-system` | Wire read endpoint | 4 |
| 20 | `GetAllNotificationsUseCase.java` | `notification-system` | Join read-status | 4 |

---

## 6. SQL Schemas

### push_devices

```sql
-- ============================================================
-- NOTIFICATION SYSTEM — PUSH DEVICES
-- ============================================================

CREATE TABLE push_devices (
    push_device_id  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT        NOT NULL,
    device_token    VARCHAR(512)  NOT NULL UNIQUE,
    platform        VARCHAR(10)   NOT NULL,
    app_version     VARCHAR(20),
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_push_device_user (user_id),
    INDEX idx_push_device_token (device_token)
);
```

### notification_read_statuses

```sql
-- ============================================================
-- NOTIFICATION SYSTEM — READ STATUSES
-- ============================================================

CREATE TABLE notification_read_statuses (
    notification_read_status_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_id             BIGINT    NOT NULL,
    user_id                     BIGINT    NOT NULL,
    read_at                     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_notification_user (notification_id, user_id),
    INDEX idx_read_status_user (user_id),
    INDEX idx_read_status_notification (notification_id)
);
```

---

## 7. Verification Checklist

After all phases complete:

```bash
# Full project build
mvn clean install -DskipTests

# Run affected module tests
mvn test -pl user-management -am
mvn test -pl course-management -am
mvn test -pl billing -am
mvn test -pl notification-system -am

# Full test suite
mvn clean install
```

### Convention compliance

```bash
# No any() matchers in new/modified tests
grep -rn "any()" user-management/src/test/ course-management/src/test/ billing/src/test/ notification-system/src/test/ | grep -v "/target/" && echo "FAIL" || echo "PASS"

# No inline string literals in new use cases (check for hardcoded error messages)
grep -rn '".*not found.*\|.*already.*"' user-management/src/main/ course-management/src/main/ billing/src/main/ notification-system/src/main/ | grep -v "static final" | grep -v "/target/" && echo "FAIL" || echo "PASS"
```

---

## 8. Critical Reminders

1. **Do NOT create new modules** — all gaps are implemented within existing modules.
2. **All IDs are `Long`** — no UUID, no String IDs.
3. **OpenAPI first** — update the YAML spec, regenerate DTOs, then code against generated interfaces.
4. **Query param filters must be optional** — when absent, existing behavior is preserved.
5. **New entities go in `multi-tenant-data`** — both `PushDeviceDataModel` and `NotificationReadStatusDataModel`.
6. **Platform-level entities** — device tokens and read-statuses are NOT tenant-scoped. No composite keys.
7. **Upsert for device registration** — don't throw on duplicate token, update instead.
8. **Read-status is idempotent** — marking an already-read notification returns 200, not an error.
9. **Compile gate after every phase** — `mvn clean compile -pl <module> -am -DskipTests`.
10. **Tests follow Given-When-Then** — zero `any()` matchers, `shouldDoX_whenY()` naming.
