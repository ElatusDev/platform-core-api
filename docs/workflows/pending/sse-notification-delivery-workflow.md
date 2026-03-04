# SSE Notification Delivery Workflow — AkademiaPlus

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Module**: `notification-system`
**Prerequisite**: Read `docs/directives/CLAUDE.md`, `docs/directives/AI-CODE-REF.md`, and `docs/design/DESIGN.md` before starting. Also read the existing notification-system code (`NotificationCreationUseCase.java`, `NotificationController.java`, `NotificationDataModel.java`, `NotificationDeliveryDataModel.java`).

---

## 1. Architecture Overview

### 1.1 Transport: Server-Sent Events (SSE)

The WEBAPP delivery channel uses SSE for real-time server-to-client notification push. Each authenticated user subscribes to a persistent HTTP connection via `GET /v1/notification-system/notifications/stream`. The server pushes notification events through the open connection.

### 1.2 Channel Strategy Pattern

A `DeliveryChannelStrategy` interface abstracts the delivery mechanism per channel. Each `DeliveryChannel` enum value (WEBAPP, EMAIL, SMS, IOS_PUSH, ANDROID_PUSH) can have a corresponding strategy implementation. This workflow implements only `WebappDeliveryChannelStrategy` (SSE); future channels plug in as additional `@Service` beans without modifying the dispatch orchestrator.

```
DeliveryChannelStrategy (interface)
├── WebappDeliveryChannelStrategy  ← this workflow
├── EmailDeliveryChannelStrategy   ← future
├── SmsDeliveryChannelStrategy     ← future
└── PushDeliveryChannelStrategy    ← future
```

### 1.3 Delivery Paths

**Instant delivery** — triggered synchronously from the controller:

```
POST /notifications/{id}/dispatch
  → NotificationController
    → NotificationDispatchService.dispatch(notification)
      → resolves DeliveryChannelStrategy for WEBAPP
        → WebappDeliveryChannelStrategy.deliver()
          → SseEmitterRegistry.getEmitter(tenantId, userId)
            → SseEmitter.send(event)
      → creates NotificationDeliveryDataModel (status=SENT|FAILED)
      → returns NotificationDispatchResponseDTO
```

**Scheduled delivery** — triggered by a cron scheduler:

```
@Scheduled (every 60s)
  → ScheduledNotificationDispatcher.dispatchScheduledNotifications()
    → NotificationRepository.findScheduledBefore(now)  (cross-tenant, no filter)
    → group by tenantId
    → for each tenant:
        → set up synthetic request scope (for TenantContextHolder + EntityIdAssigner)
        → for each notification:
            → NotificationDispatchService.dispatch(notification)
```

### 1.4 Data Flow Diagram

```
                         ┌─────────────────────────────┐
                         │     Client (Browser/App)     │
                         └──────┬──────────────▲────────┘
                                │              │
                      subscribe │         SSE  │ event push
                     (GET /stream)             │
                                │              │
                         ┌──────▼──────────────┴────────┐
                         │        SseController          │
                         │  (registers emitter)          │
                         └──────┬───────────────────────┘
                                │
                         ┌──────▼───────────────────────┐
                         │     SseEmitterRegistry        │
                         │  ConcurrentMap<key, emitter>  │
                         │  key = "tenantId:userId"      │
                         └──────▲───────────────────────┘
                                │
          ┌─────────────────────┤
          │                     │
   ┌──────┴──────────┐  ┌──────┴──────────────────────┐
   │ Instant Path    │  │ Scheduled Path               │
   │                 │  │                              │
   │ POST /dispatch  │  │ @Scheduled(fixedDelay=60s)   │
   │ → Controller    │  │ → ScheduledNotification-     │
   │ → DispatchSvc   │  │   Dispatcher                 │
   │                 │  │ → DispatchSvc                │
   └─────────────────┘  └─────────────────────────────┘
          │                     │
          └─────────┬───────────┘
                    │
             ┌──────▼──────────────────────────┐
             │  NotificationDispatchService     │
             │  → resolve strategy by channel   │
             │  → strategy.deliver()            │
             │  → save DeliveryDataModel        │
             └──────┬──────────────────────────┘
                    │
             ┌──────▼──────────────────────────┐
             │  WebappDeliveryChannelStrategy   │
             │  → SseEmitterRegistry.get()     │
             │  → emitter.send(event)          │
             └─────────────────────────────────┘
```

---

## 2. File Inventory

### 2.1 New Files (7)

| # | File | Package | Responsibility |
|---|------|---------|----------------|
| 1 | `SseEmitterRegistry.java` | `notification.usecases` | Manages per-user SSE connections. Thread-safe ConcurrentHashMap with composite key `tenantId:userId`. Handles emitter lifecycle (timeout, completion, error callbacks). |
| 2 | `SseController.java` | `notification.interfaceadapters` | SSE subscription endpoint. `GET /v1/notification-system/notifications/stream` → returns `SseEmitter`. Extracts userId from JWT, tenantId from `TenantContextHolder`. Not OpenAPI-generated (SSE doesn't map well to OpenAPI). |
| 3 | `DeliveryChannelStrategy.java` | `notification.usecases` | Strategy interface: `DeliveryChannel getChannel()` + `DeliveryResult deliver(NotificationDataModel, String recipientId)`. Extensible for future channels. |
| 4 | `WebappDeliveryChannelStrategy.java` | `notification.usecases` | WEBAPP channel implementation. Uses `SseEmitterRegistry` to find and send events. Returns SENT/FAILED status. |
| 5 | `NotificationDispatchService.java` | `notification.usecases` | Dispatch orchestrator. Resolves strategy by channel, calls `deliver()`, creates `NotificationDeliveryDataModel` record via prototype bean, saves to repository. |
| 6 | `ScheduledNotificationDispatcher.java` | `notification.usecases` | `@Scheduled` dispatcher. Queries pending notifications across all tenants, groups by tenant, sets up synthetic request scope per tenant (for `TenantContextHolder` / `EntityIdAssigner`), delegates to `NotificationDispatchService`. |
| 7 | `NotificationSchedulingConfiguration.java` | `config` | `@Configuration` + `@EnableScheduling`. Activates Spring's task scheduling infrastructure for the notification module. |

### 2.2 Modified Files (2)

| # | File | Change |
|---|------|--------|
| 1 | `NotificationRepository.java` | Add JPQL query `findScheduledBefore(LocalDateTime now)` — selects notifications where `scheduledAt <= now`, `scheduledAt IS NOT NULL`, and `deletedAt IS NULL`. Used by the scheduler to find due notifications. |
| 2 | `NotificationController.java` | Add `POST /notifications/{notificationId}/dispatch` operation (from updated OpenAPI spec). Delegates to `NotificationDispatchService`. Returns `NotificationDispatchResponseDTO`. |

### 2.3 OpenAPI Changes

Update `notification-system/src/main/resources/openapi/notification.yaml`:

- Add `NotificationDispatchResponse` schema (`notificationDeliveryId: int64`, `channel: string`, `status: string`)
- Add `POST /notifications/{notificationId}/dispatch` path with `operationId: dispatchNotification`
- Register new schemas and path in `notification-system-module.yaml`

### 2.4 Test Files (5)

| # | File | Scope |
|---|------|-------|
| 1 | `SseEmitterRegistryTest.java` | Registration, retrieval, removal, lifecycle callbacks |
| 2 | `WebappDeliveryChannelStrategyTest.java` | Channel identity, send success/failure |
| 3 | `NotificationDispatchServiceTest.java` | Strategy resolution, delivery record creation, status handling |
| 4 | `ScheduledNotificationDispatcherTest.java` | Query execution, tenant grouping, context setup, delegation |
| 5 | `SseControllerTest.java` | Endpoint returns SseEmitter, registration in registry |

---

## 3. Implementation Sequence

Execute phases in strict order. Each phase ends with a compile/test verification gate. Do NOT proceed to the next phase until the current one passes.

```
Phase 1: SSE Infrastructure       → SseEmitterRegistry + test
Phase 2: Channel Strategy Pattern  → interface + WEBAPP impl + test
Phase 3: Dispatch Service          → service + repository query + test
Phase 4: Endpoints                 → OpenAPI + SseController + controller update
Phase 5: Scheduling                → config + dispatcher + test
```

### Phase 1: SSE Infrastructure

**Creates**: `SseEmitterRegistry.java`, `SseEmitterRegistryTest.java`

`SseEmitterRegistry` is a singleton `@Service` managing `ConcurrentHashMap<String, SseEmitter>`. Key format: `"tenantId:userId"`.

**Public API**:

| Method | Description |
|--------|-------------|
| `SseEmitter register(Long tenantId, Long userId)` | Creates new emitter, registers lifecycle callbacks, stores in map. Returns emitter. |
| `Optional<SseEmitter> getEmitter(Long tenantId, Long userId)` | Looks up emitter by composite key. |
| `void remove(Long tenantId, Long userId)` | Removes emitter from map. |

**Constants**:
- `public static final long DEFAULT_TIMEOUT` — emitter timeout in ms (300,000 = 5 min)
- `public static final String EMITTER_KEY_SEPARATOR` — `":"`

**Lifecycle callbacks** (registered on each emitter):
- `onCompletion()` → `remove(tenantId, userId)`
- `onTimeout()` → `remove(tenantId, userId)`
- `onError(e)` → `remove(tenantId, userId)`

**Test plan** — `SseEmitterRegistryTest`:

| @Nested | Test Method | Verifies |
|---------|-------------|----------|
| Registration | `shouldReturnNewEmitter_whenRegistering` | register() returns non-null SseEmitter |
| Registration | `shouldStoreEmitter_whenRegistering` | getEmitter() returns the registered emitter |
| Registration | `shouldReplaceEmitter_whenSameUserRegistersAgain` | latest emitter wins |
| Retrieval | `shouldReturnEmitter_whenUserIsRegistered` | getEmitter() returns Optional with emitter |
| Retrieval | `shouldReturnEmpty_whenUserNotRegistered` | getEmitter() returns Optional.empty() |
| Removal | `shouldRemoveEmitter_whenCallingRemove` | getEmitter() returns empty after remove |
| KeyBuilding | `shouldBuildKey_withTenantAndUserId` | buildKey returns "tenantId:userId" |

**Verification gate**:
```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl notification-system -am -f platform-core-api/pom.xml
```

---

### Phase 2: Channel Strategy Pattern

**Creates**: `DeliveryChannelStrategy.java`, `WebappDeliveryChannelStrategy.java`, `WebappDeliveryChannelStrategyTest.java`

**`DeliveryChannelStrategy` interface**:

```java
public interface DeliveryChannelStrategy {
    DeliveryChannel getChannel();
    DeliveryResult deliver(NotificationDataModel notification, String recipientIdentifier);
}
```

**`DeliveryResult` record** (inner type or separate file):

```java
public record DeliveryResult(DeliveryStatus status, String failureReason) {
    public static DeliveryResult sent() { return new DeliveryResult(DeliveryStatus.SENT, null); }
    public static DeliveryResult failed(String reason) { return new DeliveryResult(DeliveryStatus.FAILED, reason); }
}
```

**`WebappDeliveryChannelStrategy`** — `@Service` implementing `DeliveryChannelStrategy`:

- `getChannel()` → `DeliveryChannel.WEBAPP`
- `deliver()`:
  1. Look up emitter via `sseEmitterRegistry.getEmitter(tenantId, userId)`
  2. If absent → `DeliveryResult.failed("User not connected")`
  3. If present → `emitter.send(SseEmitter.event().name("notification").data(notification))` wrapped in try/catch
  4. On success → `DeliveryResult.sent()`
  5. On IOException → `DeliveryResult.failed(e.getMessage())`, remove emitter

**Test plan** — `WebappDeliveryChannelStrategyTest`:

| @Nested | Test Method | Verifies |
|---------|-------------|----------|
| ChannelIdentity | `shouldReturnWebappChannel` | getChannel() == WEBAPP |
| Delivery | `shouldReturnSent_whenEmitterExistsAndSendSucceeds` | DeliveryResult status = SENT |
| Delivery | `shouldReturnFailed_whenNoEmitterRegistered` | DeliveryResult status = FAILED |
| Delivery | `shouldReturnFailed_whenSendThrowsIOException` | Catches error, returns FAILED |
| Delivery | `shouldRemoveEmitter_whenSendFails` | Calls registry.remove() on error |

**Verification gate**:
```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl notification-system -am -f platform-core-api/pom.xml
```

---

### Phase 3: Dispatch Service + Repository Query

**Creates**: `NotificationDispatchService.java`, `NotificationDispatchServiceTest.java`
**Modifies**: `NotificationRepository.java`

**`NotificationRepository` — add JPQL query**:

```java
@Query("SELECT n FROM NotificationDataModel n WHERE n.scheduledAt <= :now "
     + "AND n.scheduledAt IS NOT NULL AND n.deletedAt IS NULL")
List<NotificationDataModel> findScheduledBefore(@Param("now") LocalDateTime now);
```

This query runs cross-tenant when the Hibernate tenant filter is inactive (scheduler context).

**`NotificationDispatchService`** — `@Service`:

Dependencies:
- `ApplicationContext` — prototype bean for `NotificationDeliveryDataModel`
- `NotificationDeliveryRepository` — save delivery records
- `List<DeliveryChannelStrategy>` — auto-injected by Spring (all strategy beans)

**Public API**:

| Method | Description |
|--------|-------------|
| `NotificationDeliveryDataModel dispatch(NotificationDataModel notification)` | Resolves WEBAPP strategy, calls deliver(), creates + saves delivery record. |

**Dispatch flow**:

1. Resolve strategy for `DeliveryChannel.WEBAPP`
2. Call `strategy.deliver(notification, String.valueOf(notification.getTargetUserId()))`
3. Create `NotificationDeliveryDataModel` via `applicationContext.getBean()`
4. Set fields: `notificationId`, `channel=WEBAPP`, `recipientIdentifier`, `status`, `sentAt` (if SENT), `failureReason` (if FAILED), `retryCount=0`
5. Save via `notificationDeliveryRepository.save(delivery)`
6. Return saved delivery

**Strategy resolution**: Build a `Map<DeliveryChannel, DeliveryChannelStrategy>` in `@PostConstruct` from the injected list.

**Constants**:
- `public static final String ERROR_NO_STRATEGY` — "No delivery strategy registered for channel: %s"
- `public static final String ERROR_TARGET_USER_REQUIRED` — "Notification targetUserId is required for dispatch"
- `public static final int INITIAL_RETRY_COUNT` — 0

**Test plan** — `NotificationDispatchServiceTest`:

| @Nested | Test Method | Verifies |
|---------|-------------|----------|
| StrategyResolution | `shouldResolveStrategy_whenChannelHasRegisteredStrategy` | Finds WEBAPP strategy |
| StrategyResolution | `shouldThrowException_whenNoStrategyForChannel` | IllegalStateException |
| Dispatch | `shouldCreateDeliveryRecord_whenDispatching` | Creates prototype bean, sets fields, saves |
| Dispatch | `shouldSetStatusToSent_whenStrategySucceeds` | DeliveryStatus.SENT on record |
| Dispatch | `shouldSetStatusToFailed_whenStrategyFails` | DeliveryStatus.FAILED + failureReason |
| Dispatch | `shouldSetSentAt_whenDeliverySucceeds` | sentAt is non-null for SENT |
| Dispatch | `shouldThrowException_whenTargetUserIdIsNull` | IllegalArgumentException |
| Dispatch | `shouldReturnSavedDelivery` | Returns the repository save result |

**Verification gate**:
```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl notification-system -am -f platform-core-api/pom.xml
```

---

### Phase 4: Endpoints

**Creates**: `SseController.java`
**Modifies**: `notification.yaml`, `notification-system-module.yaml`, `NotificationController.java`

**Step 4.1 — Update OpenAPI spec**:

Add to `notification.yaml`:

```yaml
# Under components.schemas:
NotificationDispatchResponse:
  type: object
  properties:
    notificationDeliveryId:
      type: integer
      format: int64
      readOnly: true
    channel:
      type: string
      maxLength: 30
    status:
      type: string
      maxLength: 20
  required:
    - notificationDeliveryId
    - channel
    - status

# Under paths:
'/notifications/{notificationId}/dispatch':
  post:
    operationId: dispatchNotification
    summary: Dispatch a notification immediately via WEBAPP channel
    parameters:
      - name: notificationId
        in: path
        required: true
        schema:
          type: integer
          format: int64
    responses:
      '200':
        description: Notification dispatched
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/NotificationDispatchResponse'
      '404':
        description: Notification not found
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ErrorResponse'
```

Register in `notification-system-module.yaml` under schemas + paths.

**Step 4.2 — Regenerate DTOs**:
```bash
mvn clean generate-sources -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

**Step 4.3 — SseController**:

Manual controller (not OpenAPI-generated — SSE doesn't map well to OpenAPI specs):

```java
@RestController
@RequestMapping("/v1/notification-system")
@RequiredArgsConstructor
public class SseController {
    private final SseEmitterRegistry sseEmitterRegistry;
    private final TenantContextHolder tenantContextHolder;

    @GetMapping(value = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        Long tenantId = tenantContextHolder.requireTenantId();
        Long userId = extractAuthenticatedUserId();  // from SecurityContext JWT claims
        return sseEmitterRegistry.register(tenantId, userId);
    }
}
```

> **Note**: `extractAuthenticatedUserId()` implementation depends on the security module's JWT claim structure. Check `security/` for how the authenticated principal is structured. The user ID may be available via `SecurityContextHolder.getContext().getAuthentication().getName()` or a custom claim.

**Step 4.4 — Update NotificationController**:

Add `NotificationDispatchService` as a constructor dependency. Implement the new `dispatchNotification` operation from the regenerated `NotificationsApi` interface:

```java
@Override
public ResponseEntity<NotificationDispatchResponseDTO> dispatchNotification(Long notificationId) {
    NotificationDataModel notification = getNotificationByIdUseCase.getById(notificationId);
    NotificationDeliveryDataModel delivery = notificationDispatchService.dispatch(notification);
    NotificationDispatchResponseDTO response = modelMapper.map(delivery, NotificationDispatchResponseDTO.class);
    return ResponseEntity.ok(response);
}
```

**Verification gate**:
```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

---

### Phase 5: Scheduling Infrastructure

**Creates**: `NotificationSchedulingConfiguration.java`, `ScheduledNotificationDispatcher.java`, `ScheduledNotificationDispatcherTest.java`

**`NotificationSchedulingConfiguration`** — simple `@Configuration` + `@EnableScheduling`.

**`ScheduledNotificationDispatcher`** — `@Service`:

Dependencies:
- `NotificationRepository` — query pending notifications
- `NotificationDispatchService` — dispatch each notification
- `ApplicationContext` — create request-scoped `TenantContextHolder`

**Core method**:
```java
@Scheduled(fixedDelayString = "${akademia.notification.scheduler.interval-ms:60000}")
public void dispatchScheduledNotifications() {
    List<NotificationDataModel> pending = notificationRepository.findScheduledBefore(LocalDateTime.now());
    Map<Long, List<NotificationDataModel>> byTenant = pending.stream()
            .collect(Collectors.groupingBy(NotificationDataModel::getTenantId));
    for (Map.Entry<Long, List<NotificationDataModel>> entry : byTenant.entrySet()) {
        executeInTenantContext(entry.getKey(), entry.getValue());
    }
}
```

**Tenant context challenge**: The scheduler thread runs outside HTTP request scope. `TenantContextHolder` is `@RequestScope` and `EntityIdAssigner` reads from it. Without a request scope, saving entities will fail with a scope-not-active error.

**Solution**: Create a synthetic request scope using Spring's `RequestContextHolder` with a minimal `RequestAttributes` implementation:

```java
private void executeInTenantContext(Long tenantId, List<NotificationDataModel> notifications) {
    RequestContextHolder.setRequestAttributes(new SchedulerRequestAttributes());
    try {
        TenantContextHolder holder = applicationContext.getBean(TenantContextHolder.class);
        holder.setTenantId(tenantId);
        for (NotificationDataModel notification : notifications) {
            dispatchSafely(notification);
        }
    } finally {
        RequestContextHolder.resetRequestAttributes();
    }
}
```

The `SchedulerRequestAttributes` is a package-private inner class implementing `RequestAttributes` with a simple `ConcurrentHashMap` backing store. See the prompt file for the full implementation.

**Error isolation**: `dispatchSafely()` wraps each dispatch in a try-catch to prevent one failed notification from blocking the rest. Logs errors but continues.

**Test plan** — `ScheduledNotificationDispatcherTest`:

| @Nested | Test Method | Verifies |
|---------|-------------|----------|
| Query | `shouldQueryPendingNotifications_whenDispatching` | Calls findScheduledBefore with current time |
| Dispatch | `shouldDispatchEachNotification_whenPendingExist` | Calls dispatch for every result |
| Dispatch | `shouldContinueDispatching_whenOneNotificationFails` | Error isolation works |
| Dispatch | `shouldNotDispatch_whenNoPendingNotifications` | Empty list → no dispatch calls |
| TenantContext | `shouldGroupNotificationsByTenant` | Verifies per-tenant context setup |

**Verification gate**:
```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl notification-system -am -f platform-core-api/pom.xml
# Full build:
mvn clean install -DskipITs -f platform-core-api/pom.xml
```

---

## 4. Key Design Decisions

### 4.1 SSE over WebSocket

| Factor | SSE | WebSocket |
|--------|-----|-----------|
| Direction | Server → Client (unidirectional) | Bidirectional |
| Protocol | Standard HTTP | Upgrade to WS protocol |
| Reconnection | Built-in (browser auto-reconnects) | Manual implementation |
| Complexity | Low (Spring's `SseEmitter`) | Higher (STOMP, message brokers) |
| Use case fit | Notification push = unidirectional | Overkill for this use case |
| Infrastructure | No extra dependencies | Requires `spring-boot-starter-websocket` |

**Decision**: SSE. Notification delivery is server-to-client only. SSE provides automatic reconnection, works through HTTP proxies, and requires no additional dependencies.

### 4.2 `@Scheduled` over Quartz

| Factor | @Scheduled | Quartz |
|--------|------------|--------|
| Setup | Zero config (`@EnableScheduling`) | Database tables + config |
| Clustering | Single instance only | Multi-instance via DB locks |
| Persistence | None (in-memory) | Job store in DB |
| Complexity | Minimal | Significant |
| Current project state | No scheduling exists yet | Would be first use |

**Decision**: `@Scheduled` for now. The project has no scheduling infrastructure. Start simple, upgrade to Quartz/ShedLock when horizontal scaling is needed (see Future Extensibility).

### 4.3 Manual SseController (not OpenAPI-generated)

SSE endpoints return `SseEmitter` (a streaming response) which doesn't fit OpenAPI's request-response model. The `SseController` is implemented manually as a standalone `@RestController`, separate from the OpenAPI-generated `NotificationsApi` interface.

### 4.4 Single emitter per user

The registry stores one `SseEmitter` per `tenantId:userId` pair. If the same user opens multiple browser tabs, the latest connection replaces the previous one. This is a deliberate simplification for v1 — multi-connection support can be added later by changing the value type to `CopyOnWriteArrayList<SseEmitter>`.

### 4.5 Idempotent dispatch

The dispatch service does NOT check for existing delivery records before creating a new one. Each call to `dispatch()` creates a new `NotificationDeliveryDataModel`. This is intentional — retry semantics are handled at the scheduler level (by checking delivery status), not at the service level.

---

## 5. Multi-Tenancy Considerations

### 5.1 Tenant-scoped emitter registry

The `SseEmitterRegistry` uses composite keys (`tenantId:userId`) to ensure complete tenant isolation. A user in tenant 1 cannot receive notifications intended for tenant 2, even if both tenants have a user with the same ID.

### 5.2 Scheduler cross-tenant query

The `findScheduledBefore()` JPQL query runs outside HTTP request scope, so the Hibernate tenant filter is inactive. This is intentional — the scheduler must query across ALL tenants to find due notifications.

The scheduler groups results by `tenantId` and sets up a synthetic request scope per tenant before dispatching. This ensures `TenantContextHolder` and `EntityIdAssigner` work correctly when creating delivery records.

### 5.3 Entity ID assignment

`NotificationDeliveryDataModel` follows the standard composite key pattern (`tenantId` + `notificationDeliveryId`). The `EntityIdAssigner` assigns both IDs via `PreInsertEvent`. The dispatch service creates delivery models via `applicationContext.getBean()` (prototype scope) and lets `EntityIdAssigner` handle ID assignment — never sets IDs manually.

---

## 6. Future Extensibility

### 6.1 ShedLock for cluster-safe scheduling

When the platform runs multiple instances, `@Scheduled` will fire on every node. Add [ShedLock](https://github.com/lukas-krecan/ShedLock) to ensure only one instance processes scheduled notifications:

```java
@Scheduled(fixedDelay = 60000)
@SchedulerLock(name = "dispatchScheduledNotifications", lockAtLeastFor = "30s", lockAtMostFor = "5m")
public void dispatchScheduledNotifications() { ... }
```

Requires: `shedlock-spring` + `shedlock-provider-jdbc-template` dependencies, a `shedlock` database table.

### 6.2 Targeted delivery (audience selection)

Currently dispatches to a single `targetUserId`. Future enhancement: support audience selectors (all users in a tenant, users with a specific role, users enrolled in a course). This would require an `AudienceResolver` service that expands a selector into a list of user IDs.

### 6.3 Redis pub/sub for horizontal scaling

In a multi-instance deployment, SSE emitters exist only on the instance where the user connected. Use Redis pub/sub to broadcast dispatch events across instances:

```
Instance A: user connected → emitter in registry
Instance B: scheduler dispatches → publishes to Redis channel
Instance A: receives Redis message → sends via local emitter
```

### 6.4 Additional channel strategies

Each new channel requires:
1. A `@Service` implementing `DeliveryChannelStrategy`
2. The corresponding delivery configuration (API keys, credentials)
3. No changes to `NotificationDispatchService` — Spring auto-injects all strategy beans

Example channels:
- `EmailDeliveryChannelStrategy` → SendGrid/AWS SES
- `SmsDeliveryChannelStrategy` → Twilio
- `PushDeliveryChannelStrategy` → Firebase FCM / APNs

### 6.5 Delivery retry mechanism

Add exponential backoff retry for failed deliveries:
1. `ScheduledNotificationDispatcher` also queries for FAILED deliveries with `retryCount < maxRetries`
2. Increment `retryCount` on each attempt
3. Use `nextRetryAt = sentAt + (2^retryCount * baseDelay)` for backoff

---

## 7. Verification Checklist

Run after all phases complete:

```bash
# 1. Full module compile
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml

# 2. All unit tests pass
mvn test -pl notification-system -am -f platform-core-api/pom.xml

# 3. Full project build (no cross-module breakage)
mvn clean install -DskipTests -f platform-core-api/pom.xml

# 4. Convention compliance
grep -rn "any()" notification-system/src/test/ | grep -v "import" | wc -l  # must be 0
grep -rn "// Arrange" notification-system/src/test/ | wc -l               # must be 0
grep -rn "catch (Exception " notification-system/src/main/ | wc -l       # must be 0

# 5. Copyright headers on all new files
for f in $(git diff --name-only --diff-filter=A HEAD); do
  head -1 "$f" | grep -q "Copyright" || echo "MISSING: $f"
done
```

---

## 8. Critical Reminders

1. **Prototype beans**: ALL entity instantiation via `applicationContext.getBean()` — never `new NotificationDeliveryDataModel()`
2. **ID assignment**: Never set `notificationDeliveryId` or `tenantId` manually — `EntityIdAssigner` handles both
3. **Named TypeMaps**: If any DTO→entity mapping is added, use named TypeMaps with skip rules
4. **Constants**: ALL string literals → `public static final`, shared between impl and tests
5. **Testing**: Given-When-Then, `shouldDoX_whenY()`, ZERO `any()` matchers, `@DisplayName` on all tests
6. **Copyright header**: Required on ALL new `.java` files (ElatusDev 2025)
7. **Commits**: Conventional Commits (`feat(notification-system): ...`), NO `Co-Authored-By` or AI attribution
8. **Long IDs**: Always `Long`, never `Integer`
9. **`@Transactional`**: Only on methods that write to the database (dispatch + save)
10. **Scheduler tenant context**: Always wrap scheduler dispatch in synthetic request scope
