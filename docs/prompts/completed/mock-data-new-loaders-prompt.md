# Mock-Data New Loaders — Claude Code Execution Prompt

> **Workflow**: [`mock-data-new-loaders-workflow.md`](../../workflows/pending/mock-data-new-loaders-workflow.md)
> **Project**: core-api (mock-data-system module)

---

## EXECUTION RULES

1. **Read every file before modifying it**
2. Follow existing patterns exactly — read a working loader (e.g., CourseEvent) as reference
3. Copyright header: standard ElatusDev header on all new files
4. Verify: `mvn compile -pl mock-data-system -am` after each step

---

## Step 1 — Read existing patterns

### Read these files first

```
mock-data-system/src/main/java/com/akademiaplus/config/MockEntityType.java
mock-data-system/src/main/java/com/akademiaplus/config/MockDataRegistry.java
mock-data-system/src/main/java/com/akademiaplus/config/MockDataOrchestrator.java
mock-data-system/src/main/java/com/akademiaplus/config/MockDataExecutionPlan.java
mock-data-system/src/main/java/com/akademiaplus/usecases/course/LoadCourseEventMockDataUseCase.java
mock-data-system/src/main/java/com/akademiaplus/util/mock/course/CourseEventFactory.java
mock-data-system/src/main/java/com/akademiaplus/config/CourseDataLoaderConfiguration.java
mock-data-system/src/main/java/com/akademiaplus/util/base/DataLoader.java
mock-data-system/src/main/java/com/akademiaplus/util/base/DataCleanUp.java
```

### Read the target entity models + repositories

```
multi-tenant-data/src/main/java/com/akademiaplus/attendance/AttendanceSessionDataModel.java
multi-tenant-data/src/main/java/com/akademiaplus/attendance/AttendanceRecordDataModel.java
multi-tenant-data/src/main/java/com/akademiaplus/leadmanagement/DemoRequestDataModel.java
multi-tenant-data/src/main/java/com/akademiaplus/notifications/NotificationReadStatusDataModel.java
multi-tenant-data/src/main/java/com/akademiaplus/notifications/PushDeviceDataModel.java
```

Search for repositories: `AttendanceSessionRepository`, `AttendanceRecordRepository`,
`DemoRequestRepository`, `NotificationReadStatusRepository`, `PushDeviceRepository`.

Also check if there are existing create DTOs for these entities (e.g., `AttendanceSessionCreateRequestDTO`).
If DTOs don't exist, the factory can produce the DataModel directly (like the Course loader does).

---

## Step 2 — Add MockEntityType entries

**File**: `MockEntityType.java`

Add these entries in the correct dependency level position:

```java
// Level 0 — no dependencies (platform-level)
DEMO_REQUEST(true, true),
PUSH_DEVICE(true, true),

// Level 4 — depends on COURSE_EVENT
ATTENDANCE_SESSION(true, true, COURSE_EVENT),

// Level 4 — depends on NOTIFICATION
NOTIFICATION_READ_STATUS(true, true, NOTIFICATION),

// Level 5 — depends on ATTENDANCE_SESSION
ATTENDANCE_RECORD(true, true, ATTENDANCE_SESSION),
```

### Verify
```bash
mvn compile -pl mock-data-system -am -q
```

---

## Step 3 — Create factories

Create 5 factory classes implementing `DataFactory<DTO>`. If no create DTO exists for
an entity, use the DataModel class as the generic type and build entities directly.

**For tenant-scoped entities** (AttendanceSession, AttendanceRecord):
- Use `@Setter` for injected FK ID lists (same pattern as CourseEventFactory)
- Validate lists are non-empty before generating

**For platform-level entities** (DemoRequest, NotificationReadStatus, PushDevice):
- DemoRequest: generate sequential unique emails
- PushDevice: generate sequential unique device tokens
- NotificationReadStatus: use `@Setter` for notificationIds and userIds

---

## Step 4 — Create use cases

Create 5 use case classes extending `AbstractMockDataUseCase<D, M, I>`.
Each injects `DataLoader` and `DataCleanUp` beans.

---

## Step 5 — Create DataLoader configuration

Create `@Configuration` class(es) with `@Bean` methods for:
- `DataLoader` per entity (repository + transformer function + factory)
- `DataCleanUp` per entity (repository + DataModel class)

**Critical**: For platform-level entities using `JpaRepository` (not `TenantScopedRepository`),
verify `DataLoader` and `DataCleanUp` generics are compatible. If not, create separate
platform-level loader/cleanup utilities.

---

## Step 6 — Wire in MockDataRegistry

**File**: `MockDataRegistry.java`

1. Inject the 5 new use cases
2. Add entries to `mockDataLoaders` map
3. Add entries to `mockDataCleaners` map
4. Add post-load hooks to wire FK IDs into downstream factories:
   - After COURSE_EVENT → inject courseEventIds into AttendanceSessionFactory
   - After ATTENDANCE_SESSION → inject sessionIds into AttendanceRecordFactory
   - After ADULT_STUDENT → inject studentIds into AttendanceRecordFactory, PushDeviceFactory
   - After NOTIFICATION → inject notificationIds into NotificationReadStatusFactory

---

## Step 7 — Handle platform-level entities in orchestrator

Check `MockDataOrchestrator.generateForTenant()` to confirm platform-level entities
(DEMO_REQUEST, PUSH_DEVICE, NOTIFICATION_READ_STATUS) are handled correctly.

If the orchestrator wraps everything in `TenantContextHolder.setCurrentTenant()`,
platform-level entities will fail because their repositories don't filter by tenant.

**Options:**
- Load platform-level entities outside the tenant loop
- Or check if `@FilterDef` simply doesn't apply to non-tenant entities (likely fine)

Verify by running the full generation endpoint.

---

## Step 8 — Compile and test

```bash
# Compile
mvn compile -pl mock-data-system -am -q

# Start the full dev stack
cd ../
docker compose -f docker-compose.dev.yml up --build -d

# Wait for health, then seed
curl -s http://localhost:8080/api/actuator/health
curl -s -X POST "http://localhost:8180/infra/v1/infra/mock-data/generate/tenant/1?count=5"

# Verify new entities were seeded (check DB or API)
```

### Commit

```
feat(mock-data): add loaders for attendance, demo request, push device, and notification read status

Add MockEntityType entries, factories, use cases, and DataLoader
configurations for 5 previously unseeded entities. Wire post-load
hooks to inject FK IDs into downstream factories.
```
