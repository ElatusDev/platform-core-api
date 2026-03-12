# Mock-Data New Loaders Workflow

> **Scope**: Add mock-data loaders for 5 unseeded entities
> **Trigger**: 9 entities missing from mock-data-service; 5 need seeding for E2E test coverage
> **Project**: core-api (mock-data-service module)
> **Depends on**: None (existing mock-data infrastructure is solid)

---

## 1. Problem Statement

The mock-data-service covers 30/39 JPA entities (77%). Five entities added in recent
features have no loader, factory, or MockEntityType entry. Without seed data for
these entities, E2E tests cannot cover attendance, lead management, push device
registration, or notification read status flows.

**Entities needing loaders:**

| Entity | Module | Scoping | FK Dependencies |
|--------|--------|---------|-----------------|
| AttendanceSession | course-management | Tenant-scoped | COURSE_EVENT |
| AttendanceRecord | course-management | Tenant-scoped | ATTENDANCE_SESSION (+ studentId ref) |
| DemoRequest | lead-management | Platform-level | None |
| NotificationReadStatus | notification-system | Platform-level | NOTIFICATION (+ userId ref) |
| PushDevice | notification-system | Platform-level | None (userId ref) |

**Entities intentionally skipped (runtime-generated, not seed data):**
- MagicLinkToken, PasskeyCredential, RefreshToken — created by auth flows
- EmailTemplate, EmailTemplateVariable — infrastructure config

---

## 2. Changes

### 2.1 Register in MockEntityType enum

**File**: `mock-data-service/src/main/java/com/akademiaplus/config/MockEntityType.java`

Add 5 new entries with correct dependency declarations:

```
ATTENDANCE_SESSION(true, true, COURSE_EVENT)       // Level 4
ATTENDANCE_RECORD(true, true, ATTENDANCE_SESSION)   // Level 5
DEMO_REQUEST(true, true)                            // Level 0 (no FK, platform-level)
NOTIFICATION_READ_STATUS(true, true, NOTIFICATION)  // Level 4 (needs notificationId + userId)
PUSH_DEVICE(true, true)                             // Level 0 (no FK, platform-level)
```

### 2.2 Create factories (5 files)

**Directory**: `mock-data-service/src/main/java/com/akademiaplus/util/mock/`

| Factory | Location | Injected FK Lists |
|---------|----------|-------------------|
| AttendanceSessionFactory | `mock/attendance/` | courseEventIds |
| AttendanceRecordFactory | `mock/attendance/` | attendanceSessionIds, adultStudentIds |
| DemoRequestFactory | `mock/leadmanagement/` | None |
| NotificationReadStatusFactory | `mock/notification/` | notificationIds, userIds |
| PushDeviceFactory | `mock/notification/` | userIds |

Each factory implements `DataFactory<DTO>` and generates realistic test data.

**AttendanceSessionFactory** fields:
- status: cycle through `OPEN`, `CLOSED`
- qrSecret: random UUID
- tokenIntervalSeconds: 30
- courseEventId: from injected list

**AttendanceRecordFactory** fields:
- attendanceSessionId: from injected list
- studentId: from injected adultStudentIds
- studentType: `ADULT`
- verificationMethod: cycle through `QR_SCAN`, `MANUAL`
- checkedInAt: current timestamp

**DemoRequestFactory** fields:
- firstName, lastName: faker-style sequential (`Demo-1`, `Request-1`)
- email: `demo-{i}@e2e-test.com` (unique per count)
- companyName: `Test Company {i}`
- message: `Demo request message {i}`
- status: `PENDING`

**NotificationReadStatusFactory** fields:
- notificationId: from injected list
- userId: from injected list

**PushDeviceFactory** fields:
- userId: from injected list
- deviceToken: `mock-device-token-{i}` (unique)
- platform: cycle through `IOS`, `ANDROID`
- appVersion: `1.0.0`

### 2.3 Create use cases (5 files)

**Directory**: `mock-data-service/src/main/java/com/akademiaplus/usecases/`

Each extends `AbstractMockDataUseCase<DTO, DataModel, IdClass>`:
- `LoadAttendanceSessionMockDataUseCase` in `usecases/attendance/`
- `LoadAttendanceRecordMockDataUseCase` in `usecases/attendance/`
- `LoadDemoRequestMockDataUseCase` in `usecases/leadmanagement/`
- `LoadNotificationReadStatusMockDataUseCase` in `usecases/notification/`
- `LoadPushDeviceMockDataUseCase` in `usecases/notification/`

### 2.4 Create DataLoader configuration

**File**: New `@Configuration` class(es) or extend existing ones.

For tenant-scoped entities (AttendanceSession, AttendanceRecord):
- `DataLoader` bean with repository + `Function<DTO, DataModel>` transformer
- `DataCleanUp` bean with repository + DataModel class

For platform-level entities (DemoRequest, NotificationReadStatus, PushDevice):
- Use `JpaRepository` (not `TenantScopedRepository`)
- DataCleanUp may need adjustment — platform-level entities don't use tenant filtering

### 2.5 Register in MockDataRegistry

**File**: `mock-data-service/src/main/java/com/akademiaplus/config/MockDataRegistry.java`

- Add 5 entries to `mockDataLoaders` map
- Add 5 entries to `mockDataCleaners` map
- Add post-load hooks:
  - After `COURSE_EVENT` → inject courseEventIds into `AttendanceSessionFactory`
  - After `ATTENDANCE_SESSION` → inject attendanceSessionIds into `AttendanceRecordFactory`
  - After `NOTIFICATION` → inject notificationIds into `NotificationReadStatusFactory`
  - After `ADULT_STUDENT` → inject userIds into `PushDeviceFactory` and `AttendanceRecordFactory`

### 2.6 Handle platform-level entities in orchestrator

**File**: `mock-data-service/src/main/java/com/akademiaplus/config/MockDataOrchestrator.java`

DemoRequest, NotificationReadStatus, and PushDevice are NOT tenant-scoped.
Verify the orchestrator handles them correctly:
- They should NOT be wrapped in `TenantContextHolder.setCurrentTenant()`
- If the current `generateForTenant()` flow sets tenant context globally, these entities
  might need to be loaded separately (before or after the tenant loop)
- Check if `DataLoader` + `JpaRepository` works without tenant context

---

## 3. Acceptance Criteria

- [ ] MockEntityType has 5 new entries with correct dependencies
- [ ] 5 factories generate valid DTOs with realistic test data
- [ ] 5 use cases extend AbstractMockDataUseCase correctly
- [ ] MockDataRegistry wires all loaders, cleaners, and post-load hooks
- [ ] `mvn compile -pl mock-data-service -am` succeeds
- [ ] Full mock-data generation works: `POST /v1/infra/mock-data/generate/tenant/{id}?count=5` seeds all entities including new ones
- [ ] Platform-level entities (DemoRequest, PushDevice, NotificationReadStatus) seed without tenant context issues

---

## 4. Risk Assessment

| Change | Risk | Mitigation |
|--------|------|------------|
| New MockEntityType entries | Low — enum addition, no existing changes | Dependencies match entity FK structure |
| Platform-level entities in tenant orchestrator | Medium — may need special handling | Test with and without tenant context |
| Post-load hook wiring | Low — follows existing pattern | Copy from CourseEvent/Notification hooks |
| DataCleanUp for platform entities | Medium — `deleteAllInBatch` may wipe across tenants | Platform entities have no tenantId — verify this is acceptable for mock data |
