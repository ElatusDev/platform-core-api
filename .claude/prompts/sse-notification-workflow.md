# SSE Notification Delivery — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Module**: `notification-system`
**Spec**: `docs/workflows/pending/sse-notification-delivery-workflow.md` — read this first.
**Prerequisites**: Read `docs/directives/CLAUDE.md` and `docs/directives/AI-CODE-REF.md` before writing any code.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (1 → 2 → 3 → 4 → 5). Do NOT skip ahead.
2. Before writing any code, read the existing notification-system source files:
   - `notification-system/src/main/java/com/akademiaplus/notification/usecases/NotificationCreationUseCase.java`
   - `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/NotificationController.java`
   - `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/NotificationRepository.java`
   - `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/NotificationDeliveryRepository.java`
   - `multi-tenant-data/src/main/java/com/akademiaplus/notifications/NotificationDataModel.java`
   - `multi-tenant-data/src/main/java/com/akademiaplus/notifications/NotificationDeliveryDataModel.java`
   - `multi-tenant-data/src/main/java/com/akademiaplus/notifications/DeliveryChannel.java`
   - `multi-tenant-data/src/main/java/com/akademiaplus/notifications/DeliveryStatus.java`
3. **Compile gate**: After each phase, run `mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml`. Fix all errors before proceeding.
4. **Test gate**: After each phase that creates tests, run `mvn test -pl notification-system -am -f platform-core-api/pom.xml`. Fix all failures before proceeding.
5. Follow ALL conventions from `AI-CODE-REF.md`: Given-When-Then, `shouldDoX_whenY()`, ZERO `any()` matchers, `@DisplayName`, prototype beans, constants, copyright header.
6. **Commit** after each phase using the commit message provided.

---

## Phase 1: SSE Infrastructure

### 1.1 — Create SseEmitterRegistry

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/SseEmitterRegistry.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe registry for Server-Sent Event emitters.
 * <p>
 * Manages one {@link SseEmitter} per user per tenant using a composite key
 * of {@code "tenantId:userId"}. Registers lifecycle callbacks to automatically
 * remove emitters on completion, timeout, or error.
 */
@Service
public class SseEmitterRegistry {

    /** Default SSE connection timeout in milliseconds (5 minutes). */
    public static final long DEFAULT_TIMEOUT = 300_000L;

    /** Separator between tenantId and userId in the composite key. */
    public static final String EMITTER_KEY_SEPARATOR = ":";

    private final ConcurrentMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Creates and registers a new {@link SseEmitter} for the given tenant and user.
     * <p>
     * If an emitter already exists for this tenant-user pair, it is replaced.
     * Lifecycle callbacks are registered to auto-remove the emitter on
     * completion, timeout, or error.
     *
     * @param tenantId the tenant identifier
     * @param userId   the user identifier
     * @return the newly created SseEmitter
     */
    public SseEmitter register(Long tenantId, Long userId) {
        final String key = buildKey(tenantId, userId);
        final SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitter.onCompletion(() -> emitters.remove(key));
        emitter.onTimeout(() -> emitters.remove(key));
        emitter.onError(e -> emitters.remove(key));

        emitters.put(key, emitter);
        return emitter;
    }

    /**
     * Retrieves the active emitter for the given tenant and user.
     *
     * @param tenantId the tenant identifier
     * @param userId   the user identifier
     * @return the emitter if the user is connected, empty otherwise
     */
    public Optional<SseEmitter> getEmitter(Long tenantId, Long userId) {
        return Optional.ofNullable(emitters.get(buildKey(tenantId, userId)));
    }

    /**
     * Removes the emitter for the given tenant and user.
     *
     * @param tenantId the tenant identifier
     * @param userId   the user identifier
     */
    public void remove(Long tenantId, Long userId) {
        emitters.remove(buildKey(tenantId, userId));
    }

    /**
     * Builds the composite map key from tenant and user identifiers.
     *
     * @param tenantId the tenant identifier
     * @param userId   the user identifier
     * @return composite key in format "tenantId:userId"
     */
    String buildKey(Long tenantId, Long userId) {
        return tenantId + EMITTER_KEY_SEPARATOR + userId;
    }
}
```

### 1.2 — Create SseEmitterRegistryTest

**File**: `notification-system/src/test/java/com/akademiaplus/notification/usecases/SseEmitterRegistryTest.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SseEmitterRegistry")
class SseEmitterRegistryTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;
    private static final String EXPECTED_KEY = "1:100";

    private SseEmitterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SseEmitterRegistry();
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("Should return new SseEmitter when registering a user")
        void shouldReturnNewEmitter_whenRegistering() {
            // Given — a tenant and user ID

            // When
            SseEmitter emitter = registry.register(TENANT_ID, USER_ID);

            // Then
            assertThat(emitter).isNotNull();
        }

        @Test
        @DisplayName("Should store emitter retrievable by same tenant and user")
        void shouldStoreEmitter_whenRegistering() {
            // Given
            SseEmitter registered = registry.register(TENANT_ID, USER_ID);

            // When
            Optional<SseEmitter> retrieved = registry.getEmitter(TENANT_ID, USER_ID);

            // Then
            assertThat(retrieved).isPresent().contains(registered);
        }

        @Test
        @DisplayName("Should replace existing emitter when same user registers again")
        void shouldReplaceEmitter_whenSameUserRegistersAgain() {
            // Given
            SseEmitter first = registry.register(TENANT_ID, USER_ID);

            // When
            SseEmitter second = registry.register(TENANT_ID, USER_ID);

            // Then
            assertThat(second).isNotSameAs(first);
            assertThat(registry.getEmitter(TENANT_ID, USER_ID)).contains(second);
        }
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return emitter when user is registered")
        void shouldReturnEmitter_whenUserIsRegistered() {
            // Given
            SseEmitter emitter = registry.register(TENANT_ID, USER_ID);

            // When
            Optional<SseEmitter> result = registry.getEmitter(TENANT_ID, USER_ID);

            // Then
            assertThat(result).isPresent().contains(emitter);
        }

        @Test
        @DisplayName("Should return empty when user is not registered")
        void shouldReturnEmpty_whenUserNotRegistered() {
            // Given — no registration

            // When
            Optional<SseEmitter> result = registry.getEmitter(TENANT_ID, OTHER_USER_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Removal")
    class Removal {

        @Test
        @DisplayName("Should remove emitter when calling remove")
        void shouldRemoveEmitter_whenCallingRemove() {
            // Given
            registry.register(TENANT_ID, USER_ID);

            // When
            registry.remove(TENANT_ID, USER_ID);

            // Then
            assertThat(registry.getEmitter(TENANT_ID, USER_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Key Building")
    class KeyBuilding {

        @Test
        @DisplayName("Should build key with tenant ID and user ID separated by colon")
        void shouldBuildKey_withTenantAndUserId() {
            // Given — TENANT_ID and USER_ID constants

            // When
            String key = registry.buildKey(TENANT_ID, USER_ID);

            // Then
            assertThat(key).isEqualTo(EXPECTED_KEY);
        }
    }
}
```

### 1.3 — Verify Phase 1

```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl notification-system -am -f platform-core-api/pom.xml
```

### Commit Phase 1

```bash
git add notification-system/src/main/java/com/akademiaplus/notification/usecases/SseEmitterRegistry.java \
      notification-system/src/test/java/com/akademiaplus/notification/usecases/SseEmitterRegistryTest.java
git commit -m "feat(notification-system): add SseEmitterRegistry for SSE connection management

Tenant-scoped emitter registry using ConcurrentHashMap with composite
key (tenantId:userId). Auto-removes emitters on timeout, completion,
and error via lifecycle callbacks. Unit tests cover registration,
retrieval, removal, and key building."
```

---

## Phase 2: Channel Strategy Pattern

### 2.1 — Create DeliveryChannelStrategy interface

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/DeliveryChannelStrategy.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.NotificationDataModel;

/**
 * Strategy interface for delivering notifications through a specific channel.
 * <p>
 * Each {@link DeliveryChannel} value (WEBAPP, EMAIL, SMS, etc.) can have a
 * corresponding implementation. The {@link NotificationDispatchService}
 * resolves the appropriate strategy at dispatch time.
 *
 * @see WebappDeliveryChannelStrategy
 */
public interface DeliveryChannelStrategy {

    /**
     * Returns the delivery channel this strategy handles.
     *
     * @return the delivery channel
     */
    DeliveryChannel getChannel();

    /**
     * Delivers a notification to the specified recipient.
     *
     * @param notification        the notification to deliver
     * @param recipientIdentifier the recipient identifier (e.g., userId for WEBAPP)
     * @return the result of the delivery attempt
     */
    DeliveryResult deliver(NotificationDataModel notification, String recipientIdentifier);
}
```

### 2.2 — Create DeliveryResult record

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/DeliveryResult.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notifications.DeliveryStatus;

/**
 * Immutable result of a notification delivery attempt.
 *
 * @param status        the delivery outcome (SENT or FAILED)
 * @param failureReason the reason for failure, or {@code null} if successful
 */
public record DeliveryResult(DeliveryStatus status, String failureReason) {

    /**
     * Creates a successful delivery result.
     *
     * @return result with {@link DeliveryStatus#SENT} and no failure reason
     */
    public static DeliveryResult sent() {
        return new DeliveryResult(DeliveryStatus.SENT, null);
    }

    /**
     * Creates a failed delivery result.
     *
     * @param reason the failure reason
     * @return result with {@link DeliveryStatus#FAILED} and the given reason
     */
    public static DeliveryResult failed(String reason) {
        return new DeliveryResult(DeliveryStatus.FAILED, reason);
    }
}
```

### 2.3 — Create WebappDeliveryChannelStrategy

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/WebappDeliveryChannelStrategy.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.NotificationDataModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Optional;

/**
 * Delivers notifications via Server-Sent Events (SSE) to connected webapp clients.
 * <p>
 * Looks up the target user's active {@link SseEmitter} from the
 * {@link SseEmitterRegistry} and sends the notification as an SSE event.
 * Returns {@link DeliveryResult#failed(String)} if the user is not connected
 * or if the send operation fails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebappDeliveryChannelStrategy implements DeliveryChannelStrategy {

    /** SSE event name used for notification events. */
    public static final String EVENT_NAME = "notification";

    /** Error message when the target user has no active SSE connection. */
    public static final String ERROR_USER_NOT_CONNECTED = "User not connected via SSE";

    private final SseEmitterRegistry sseEmitterRegistry;

    @Override
    public DeliveryChannel getChannel() {
        return DeliveryChannel.WEBAPP;
    }

    @Override
    public DeliveryResult deliver(NotificationDataModel notification, String recipientIdentifier) {
        final Long tenantId = notification.getTenantId();
        final Long userId = Long.parseLong(recipientIdentifier);

        Optional<SseEmitter> emitterOpt = sseEmitterRegistry.getEmitter(tenantId, userId);

        if (emitterOpt.isEmpty()) {
            return DeliveryResult.failed(ERROR_USER_NOT_CONNECTED);
        }

        try {
            emitterOpt.get().send(
                    SseEmitter.event()
                            .name(EVENT_NAME)
                            .data(notification)
            );
            return DeliveryResult.sent();
        } catch (IOException e) {
            log.warn("SSE send failed for tenant {} user {}: {}", tenantId, userId, e.getMessage());
            sseEmitterRegistry.remove(tenantId, userId);
            return DeliveryResult.failed(e.getMessage());
        }
    }
}
```

### 2.4 — Create WebappDeliveryChannelStrategyTest

**File**: `notification-system/src/test/java/com/akademiaplus/notification/usecases/WebappDeliveryChannelStrategyTest.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.DeliveryStatus;
import com.akademiaplus.notifications.NotificationDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("WebappDeliveryChannelStrategy")
@ExtendWith(MockitoExtension.class)
class WebappDeliveryChannelStrategyTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final String RECIPIENT_IDENTIFIER = "100";

    @Mock
    private SseEmitterRegistry sseEmitterRegistry;

    private WebappDeliveryChannelStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new WebappDeliveryChannelStrategy(sseEmitterRegistry);
    }

    private NotificationDataModel buildNotification() {
        NotificationDataModel notification = new NotificationDataModel();
        notification.setTenantId(TENANT_ID);
        notification.setTargetUserId(USER_ID);
        notification.setTitle("Test Notification");
        return notification;
    }

    @Nested
    @DisplayName("Channel Identity")
    class ChannelIdentity {

        @Test
        @DisplayName("Should return WEBAPP as the delivery channel")
        void shouldReturnWebappChannel() {
            // Given — strategy instance

            // When
            DeliveryChannel channel = strategy.getChannel();

            // Then
            assertThat(channel).isEqualTo(DeliveryChannel.WEBAPP);
        }
    }

    @Nested
    @DisplayName("Delivery")
    class Delivery {

        @Test
        @DisplayName("Should return SENT when emitter exists and send succeeds")
        void shouldReturnSent_whenEmitterExistsAndSendSucceeds() throws IOException {
            // Given
            NotificationDataModel notification = buildNotification();
            SseEmitter emitter = mock(SseEmitter.class);
            when(sseEmitterRegistry.getEmitter(TENANT_ID, USER_ID)).thenReturn(Optional.of(emitter));

            // When
            DeliveryResult result = strategy.deliver(notification, RECIPIENT_IDENTIFIER);

            // Then
            assertThat(result.status()).isEqualTo(DeliveryStatus.SENT);
            assertThat(result.failureReason()).isNull();
        }

        @Test
        @DisplayName("Should return FAILED when no emitter is registered for user")
        void shouldReturnFailed_whenNoEmitterRegistered() {
            // Given
            NotificationDataModel notification = buildNotification();
            when(sseEmitterRegistry.getEmitter(TENANT_ID, USER_ID)).thenReturn(Optional.empty());

            // When
            DeliveryResult result = strategy.deliver(notification, RECIPIENT_IDENTIFIER);

            // Then
            assertThat(result.status()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(result.failureReason()).isEqualTo(WebappDeliveryChannelStrategy.ERROR_USER_NOT_CONNECTED);
        }

        @Test
        @DisplayName("Should return FAILED when SSE send throws IOException")
        void shouldReturnFailed_whenSendThrowsIOException() throws IOException {
            // Given
            NotificationDataModel notification = buildNotification();
            SseEmitter emitter = mock(SseEmitter.class);
            when(sseEmitterRegistry.getEmitter(TENANT_ID, USER_ID)).thenReturn(Optional.of(emitter));
            doThrow(new IOException("Connection reset")).when(emitter).send(
                    SseEmitter.event().name(WebappDeliveryChannelStrategy.EVENT_NAME).data(notification)
            );

            // When
            DeliveryResult result = strategy.deliver(notification, RECIPIENT_IDENTIFIER);

            // Then
            assertThat(result.status()).isEqualTo(DeliveryStatus.FAILED);
        }

        @Test
        @DisplayName("Should remove emitter from registry when send fails")
        void shouldRemoveEmitter_whenSendFails() throws IOException {
            // Given
            NotificationDataModel notification = buildNotification();
            SseEmitter emitter = mock(SseEmitter.class);
            when(sseEmitterRegistry.getEmitter(TENANT_ID, USER_ID)).thenReturn(Optional.of(emitter));
            doThrow(new IOException("Connection reset")).when(emitter).send(
                    SseEmitter.event().name(WebappDeliveryChannelStrategy.EVENT_NAME).data(notification)
            );

            // When
            strategy.deliver(notification, RECIPIENT_IDENTIFIER);

            // Then
            verify(sseEmitterRegistry).remove(TENANT_ID, USER_ID);
        }
    }
}
```

> **Note on the IOException tests**: The `SseEmitter.send()` method takes an `Object` parameter. The exact mock stubbing for the `SseEmitter.event().name().data()` builder chain may need adjustment — read the actual `SseEmitter.send()` signature and use `ArgumentCaptor` if the builder creates a non-deterministic object. An alternative approach is to use `doThrow(...).when(emitter).send(org.mockito.ArgumentMatchers.any(SseEmitter.SseEventBuilder.class))` — but ONLY if exact-value matching is genuinely impossible for this specific case. Document the justification with a comment if `any()` is used here.

### 2.5 — Verify Phase 2

```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl notification-system -am -f platform-core-api/pom.xml
```

### Commit Phase 2

```bash
git add notification-system/src/main/java/com/akademiaplus/notification/usecases/DeliveryChannelStrategy.java \
      notification-system/src/main/java/com/akademiaplus/notification/usecases/DeliveryResult.java \
      notification-system/src/main/java/com/akademiaplus/notification/usecases/WebappDeliveryChannelStrategy.java \
      notification-system/src/test/java/com/akademiaplus/notification/usecases/WebappDeliveryChannelStrategyTest.java
git commit -m "feat(notification-system): add channel strategy pattern for notification delivery

Introduce DeliveryChannelStrategy interface and WebappDeliveryChannelStrategy
(SSE) implementation. DeliveryResult record models delivery outcomes.
Strategy uses SseEmitterRegistry to push events to connected users.
Unit tests cover channel identity, successful delivery, user-not-connected,
and IOException handling."
```

---

## Phase 3: Dispatch Service + Repository Query

### 3.1 — Update NotificationRepository

**File**: `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/NotificationRepository.java`

Read the existing file first, then add the JPQL query method:

```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

// Add inside the interface:

/**
 * Finds notifications scheduled for delivery at or before the given time.
 * <p>
 * This query runs cross-tenant when the Hibernate tenant filter is inactive
 * (e.g., in scheduler context). Returns only non-deleted notifications with
 * a non-null {@code scheduledAt} timestamp.
 *
 * @param now the cutoff time
 * @return notifications due for dispatch
 */
@Query("SELECT n FROM NotificationDataModel n WHERE n.scheduledAt <= :now "
     + "AND n.scheduledAt IS NOT NULL AND n.deletedAt IS NULL")
List<NotificationDataModel> findScheduledBefore(@Param("now") LocalDateTime now);
```

### 3.2 — Create NotificationDispatchService

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/NotificationDispatchService.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.NotificationDeliveryRepository;
import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.DeliveryStatus;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.notifications.NotificationDeliveryDataModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates notification delivery through the appropriate
 * {@link DeliveryChannelStrategy}.
 * <p>
 * Resolves the strategy for the target channel, executes delivery,
 * and persists a {@link NotificationDeliveryDataModel} record with
 * the outcome. Currently dispatches via the WEBAPP (SSE) channel.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    /** Error message when no strategy is registered for a channel. */
    public static final String ERROR_NO_STRATEGY = "No delivery strategy registered for channel: %s";

    /** Error message when notification has no target user. */
    public static final String ERROR_TARGET_USER_REQUIRED = "Notification targetUserId is required for dispatch";

    /** Initial retry count for new delivery records. */
    public static final int INITIAL_RETRY_COUNT = 0;

    private final ApplicationContext applicationContext;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final List<DeliveryChannelStrategy> strategies;

    private Map<DeliveryChannel, DeliveryChannelStrategy> strategyMap;

    /**
     * Builds the channel-to-strategy lookup map from all injected strategy beans.
     */
    @PostConstruct
    void initStrategyMap() {
        strategyMap = new EnumMap<>(DeliveryChannel.class);
        for (DeliveryChannelStrategy strategy : strategies) {
            strategyMap.put(strategy.getChannel(), strategy);
        }
    }

    /**
     * Dispatches a notification via the WEBAPP channel.
     * <p>
     * Resolves the {@link WebappDeliveryChannelStrategy}, executes delivery,
     * and persists a {@link NotificationDeliveryDataModel} with the outcome.
     * The delivery entity is created via {@link ApplicationContext#getBean}
     * (prototype scope) and its ID is assigned by {@code EntityIdAssigner}.
     *
     * @param notification the notification to dispatch
     * @return the saved delivery record
     * @throws IllegalArgumentException if the notification has no targetUserId
     * @throws IllegalStateException    if no strategy is registered for WEBAPP
     */
    @Transactional
    public NotificationDeliveryDataModel dispatch(NotificationDataModel notification) {
        if (notification.getTargetUserId() == null) {
            throw new IllegalArgumentException(ERROR_TARGET_USER_REQUIRED);
        }

        final DeliveryChannel channel = DeliveryChannel.WEBAPP;
        final DeliveryChannelStrategy strategy = resolveStrategy(channel);
        final String recipientIdentifier = String.valueOf(notification.getTargetUserId());

        DeliveryResult result = strategy.deliver(notification, recipientIdentifier);

        return persistDeliveryRecord(notification, channel, recipientIdentifier, result);
    }

    /**
     * Resolves the strategy for the given delivery channel.
     *
     * @param channel the target channel
     * @return the strategy implementation
     * @throws IllegalStateException if no strategy is registered
     */
    DeliveryChannelStrategy resolveStrategy(DeliveryChannel channel) {
        DeliveryChannelStrategy strategy = strategyMap.get(channel);
        if (strategy == null) {
            throw new IllegalStateException(String.format(ERROR_NO_STRATEGY, channel));
        }
        return strategy;
    }

    private NotificationDeliveryDataModel persistDeliveryRecord(
            NotificationDataModel notification,
            DeliveryChannel channel,
            String recipientIdentifier,
            DeliveryResult result) {

        final NotificationDeliveryDataModel delivery =
                applicationContext.getBean(NotificationDeliveryDataModel.class);

        delivery.setNotificationId(notification.getNotificationId());
        delivery.setChannel(channel);
        delivery.setRecipientIdentifier(recipientIdentifier);
        delivery.setStatus(result.status());
        delivery.setRetryCount(INITIAL_RETRY_COUNT);

        if (result.status() == DeliveryStatus.SENT) {
            delivery.setSentAt(LocalDateTime.now());
        }
        if (result.failureReason() != null) {
            delivery.setFailureReason(result.failureReason());
        }

        return notificationDeliveryRepository.save(delivery);
    }
}
```

### 3.3 — Create NotificationDispatchServiceTest

**File**: `notification-system/src/test/java/com/akademiaplus/notification/usecases/NotificationDispatchServiceTest.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.NotificationDeliveryRepository;
import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.DeliveryStatus;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.notifications.NotificationDeliveryDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("NotificationDispatchService")
@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long NOTIFICATION_ID = 42L;
    private static final Long TARGET_USER_ID = 100L;
    private static final String RECIPIENT_IDENTIFIER = "100";

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Mock
    private DeliveryChannelStrategy webappStrategy;

    private NotificationDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        when(webappStrategy.getChannel()).thenReturn(DeliveryChannel.WEBAPP);
        dispatchService = new NotificationDispatchService(
                applicationContext,
                notificationDeliveryRepository,
                List.of(webappStrategy)
        );
        dispatchService.initStrategyMap();
    }

    private NotificationDataModel buildNotification() {
        NotificationDataModel notification = new NotificationDataModel();
        notification.setTenantId(TENANT_ID);
        notification.setNotificationId(NOTIFICATION_ID);
        notification.setTargetUserId(TARGET_USER_ID);
        return notification;
    }

    @Nested
    @DisplayName("Strategy Resolution")
    class StrategyResolution {

        @Test
        @DisplayName("Should resolve WEBAPP strategy when registered")
        void shouldResolveStrategy_whenChannelHasRegisteredStrategy() {
            // Given — strategy map initialized in setUp

            // When
            DeliveryChannelStrategy resolved = dispatchService.resolveStrategy(DeliveryChannel.WEBAPP);

            // Then
            assertThat(resolved).isSameAs(webappStrategy);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when no strategy for channel")
        void shouldThrowException_whenNoStrategyForChannel() {
            // Given — no EMAIL strategy registered

            // When / Then
            assertThatThrownBy(() -> dispatchService.resolveStrategy(DeliveryChannel.EMAIL))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(String.format(
                            NotificationDispatchService.ERROR_NO_STRATEGY,
                            DeliveryChannel.EMAIL));
        }
    }

    @Nested
    @DisplayName("Dispatch")
    class Dispatch {

        @Test
        @DisplayName("Should create delivery record via prototype bean when dispatching")
        void shouldCreateDeliveryRecord_whenDispatching() {
            // Given
            NotificationDataModel notification = buildNotification();
            NotificationDeliveryDataModel deliveryModel = new NotificationDeliveryDataModel();
            when(applicationContext.getBean(NotificationDeliveryDataModel.class)).thenReturn(deliveryModel);
            when(webappStrategy.deliver(notification, RECIPIENT_IDENTIFIER)).thenReturn(DeliveryResult.sent());
            when(notificationDeliveryRepository.save(deliveryModel)).thenReturn(deliveryModel);

            // When
            dispatchService.dispatch(notification);

            // Then
            verify(applicationContext).getBean(NotificationDeliveryDataModel.class);
        }

        @Test
        @DisplayName("Should set status to SENT when strategy delivery succeeds")
        void shouldSetStatusToSent_whenStrategySucceeds() {
            // Given
            NotificationDataModel notification = buildNotification();
            NotificationDeliveryDataModel deliveryModel = new NotificationDeliveryDataModel();
            when(applicationContext.getBean(NotificationDeliveryDataModel.class)).thenReturn(deliveryModel);
            when(webappStrategy.deliver(notification, RECIPIENT_IDENTIFIER)).thenReturn(DeliveryResult.sent());
            when(notificationDeliveryRepository.save(deliveryModel)).thenReturn(deliveryModel);

            // When
            dispatchService.dispatch(notification);

            // Then
            assertThat(deliveryModel.getStatus()).isEqualTo(DeliveryStatus.SENT);
            assertThat(deliveryModel.getSentAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set status to FAILED with reason when strategy fails")
        void shouldSetStatusToFailed_whenStrategyFails() {
            // Given
            NotificationDataModel notification = buildNotification();
            NotificationDeliveryDataModel deliveryModel = new NotificationDeliveryDataModel();
            String failureReason = "User not connected via SSE";
            when(applicationContext.getBean(NotificationDeliveryDataModel.class)).thenReturn(deliveryModel);
            when(webappStrategy.deliver(notification, RECIPIENT_IDENTIFIER))
                    .thenReturn(DeliveryResult.failed(failureReason));
            when(notificationDeliveryRepository.save(deliveryModel)).thenReturn(deliveryModel);

            // When
            dispatchService.dispatch(notification);

            // Then
            assertThat(deliveryModel.getStatus()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(deliveryModel.getFailureReason()).isEqualTo(failureReason);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when targetUserId is null")
        void shouldThrowException_whenTargetUserIdIsNull() {
            // Given
            NotificationDataModel notification = buildNotification();
            notification.setTargetUserId(null);

            // When / Then
            assertThatThrownBy(() -> dispatchService.dispatch(notification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(NotificationDispatchService.ERROR_TARGET_USER_REQUIRED);
        }

        @Test
        @DisplayName("Should return saved delivery record")
        void shouldReturnSavedDelivery() {
            // Given
            NotificationDataModel notification = buildNotification();
            NotificationDeliveryDataModel deliveryModel = new NotificationDeliveryDataModel();
            NotificationDeliveryDataModel savedModel = new NotificationDeliveryDataModel();
            when(applicationContext.getBean(NotificationDeliveryDataModel.class)).thenReturn(deliveryModel);
            when(webappStrategy.deliver(notification, RECIPIENT_IDENTIFIER)).thenReturn(DeliveryResult.sent());
            when(notificationDeliveryRepository.save(deliveryModel)).thenReturn(savedModel);

            // When
            NotificationDeliveryDataModel result = dispatchService.dispatch(notification);

            // Then
            assertThat(result).isSameAs(savedModel);
        }

        @Test
        @DisplayName("Should set notification ID and channel on delivery record")
        void shouldSetNotificationIdAndChannel_whenDispatching() {
            // Given
            NotificationDataModel notification = buildNotification();
            NotificationDeliveryDataModel deliveryModel = new NotificationDeliveryDataModel();
            when(applicationContext.getBean(NotificationDeliveryDataModel.class)).thenReturn(deliveryModel);
            when(webappStrategy.deliver(notification, RECIPIENT_IDENTIFIER)).thenReturn(DeliveryResult.sent());
            when(notificationDeliveryRepository.save(deliveryModel)).thenReturn(deliveryModel);

            // When
            dispatchService.dispatch(notification);

            // Then
            assertThat(deliveryModel.getNotificationId()).isEqualTo(NOTIFICATION_ID);
            assertThat(deliveryModel.getChannel()).isEqualTo(DeliveryChannel.WEBAPP);
            assertThat(deliveryModel.getRecipientIdentifier()).isEqualTo(RECIPIENT_IDENTIFIER);
            assertThat(deliveryModel.getRetryCount()).isEqualTo(NotificationDispatchService.INITIAL_RETRY_COUNT);
        }
    }
}
```

### 3.4 — Verify Phase 3

```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl notification-system -am -f platform-core-api/pom.xml
```

### Commit Phase 3

```bash
git add notification-system/src/main/java/com/akademiaplus/notification/usecases/NotificationDispatchService.java \
      notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/NotificationRepository.java \
      notification-system/src/test/java/com/akademiaplus/notification/usecases/NotificationDispatchServiceTest.java
git commit -m "feat(notification-system): add NotificationDispatchService and repository query

Dispatch orchestrator resolves DeliveryChannelStrategy by channel type,
executes delivery, and persists NotificationDeliveryDataModel record.
Repository gains findScheduledBefore() JPQL for the scheduler.
Unit tests cover strategy resolution, delivery record creation,
SENT/FAILED status handling, and input validation."
```

---

## Phase 4: SSE Endpoint + Controller Integration

### 4.1 — Update OpenAPI spec

**File**: `notification-system/src/main/resources/openapi/notification.yaml`

Read the existing file first. Add the following under `components.schemas`:

```yaml
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
```

Add under `paths`:

```yaml
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

**Also update**: `notification-system-module.yaml` — register the new schema and path reference following the existing pattern.

### 4.2 — Regenerate DTOs

```bash
mvn clean generate-sources -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

Verify generated DTOs exist:
```bash
find notification-system/target/generated-sources -name "*NotificationDispatchResponse*DTO.java"
```

### 4.3 — Create SseController

**File**: `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/SseController.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.notification.usecases.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Server-Sent Events endpoint for real-time notification delivery.
 * <p>
 * Clients subscribe via {@code GET /v1/notification-system/notifications/stream}
 * and receive push events when notifications are dispatched to them.
 * This controller is not OpenAPI-generated because SSE streaming
 * responses do not map well to OpenAPI's request-response model.
 */
@RestController
@RequestMapping("/v1/notification-system")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterRegistry sseEmitterRegistry;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Opens an SSE connection for the authenticated user.
     * <p>
     * The emitter is registered in {@link SseEmitterRegistry} keyed by
     * the tenant and user IDs. The connection stays open until the client
     * disconnects, the emitter times out, or an error occurs.
     *
     * @return an SSE emitter that will push notification events
     */
    @GetMapping(value = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        Long tenantId = tenantContextHolder.requireTenantId();
        // TODO: Extract authenticated user ID from SecurityContext JWT claims.
        // Check the security module for how the authenticated principal is structured.
        // Example: Long userId = Long.parseLong(SecurityContextHolder.getContext()
        //              .getAuthentication().getName());
        Long userId = extractAuthenticatedUserId();
        return sseEmitterRegistry.register(tenantId, userId);
    }

    /**
     * Extracts the user ID from the current security context.
     * <p>
     * Implementation depends on the JWT claim structure defined in the
     * security module. Replace this method body with the actual extraction
     * logic after reviewing the project's authentication setup.
     *
     * @return the authenticated user's ID
     */
    private Long extractAuthenticatedUserId() {
        // FIXME: Replace with actual JWT user ID extraction.
        // See security/ module for Authentication principal structure.
        throw new UnsupportedOperationException(
                "Implement extractAuthenticatedUserId() using the security module's JWT claims");
    }
}
```

> **IMPORTANT**: Before compiling, implement `extractAuthenticatedUserId()` by reading the security module to understand how user IDs are stored in JWT claims. Check:
> - `security/src/main/java/` for any `UserDetails` implementation or JWT utilities
> - How `Authentication.getPrincipal()` or `Authentication.getName()` returns the user ID
> - Whether there is a dedicated `AuthenticatedUserProvider` or similar service

### 4.4 — Update NotificationController

Read the existing `NotificationController.java` first. Add:

1. `NotificationDispatchService` as a constructor dependency
2. `ModelMapper` as a constructor dependency (if not already present)
3. Implementation of the new `dispatchNotification` operation:

```java
import openapi.akademiaplus.domain.notification.system.dto.NotificationDispatchResponseDTO;

@Override
public ResponseEntity<NotificationDispatchResponseDTO> dispatchNotification(Long notificationId) {
    NotificationDataModel notification = getNotificationByIdUseCase.getById(notificationId);
    NotificationDeliveryDataModel delivery = notificationDispatchService.dispatch(notification);
    NotificationDispatchResponseDTO response = modelMapper.map(delivery, NotificationDispatchResponseDTO.class);
    return ResponseEntity.ok(response);
}
```

> **Note**: Verify that `getNotificationByIdUseCase.getById()` returns `NotificationDataModel`. If it returns a DTO, you may need to refactor or add a separate method that returns the entity. Read the existing `GetNotificationByIdUseCase.java` to confirm.

### 4.5 — Verify Phase 4

```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

> **No test gate here** — the SseController has a FIXME that must be resolved when the security module integration is addressed. Compile verification is sufficient.

### Commit Phase 4

```bash
git add notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/SseController.java \
      notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/NotificationController.java \
      notification-system/src/main/resources/openapi/notification.yaml \
      notification-system/src/main/resources/openapi/notification-system-module.yaml
git commit -m "feat(notification-system): add SSE subscription endpoint and dispatch controller operation

SseController provides GET /notifications/stream for real-time SSE
connections. NotificationController gains POST /notifications/{id}/dispatch
for instant delivery. OpenAPI spec updated with NotificationDispatchResponse
schema. SseController userId extraction requires security module integration
(marked with FIXME)."
```

---

## Phase 5: Scheduling Infrastructure

### 5.1 — Create NotificationSchedulingConfiguration

**File**: `notification-system/src/main/java/com/akademiaplus/config/NotificationSchedulingConfiguration.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activates Spring's task scheduling infrastructure for the notification module.
 * <p>
 * Enables {@link org.springframework.scheduling.annotation.Scheduled @Scheduled}
 * methods, used by {@link com.akademiaplus.notification.usecases.ScheduledNotificationDispatcher}
 * to periodically dispatch due notifications.
 */
@Configuration
@EnableScheduling
public class NotificationSchedulingConfiguration {
}
```

### 5.2 — Create ScheduledNotificationDispatcher

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/ScheduledNotificationDispatcher.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.NotificationRepository;
import com.akademiaplus.notifications.NotificationDataModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Periodically queries for scheduled notifications and dispatches them.
 * <p>
 * Runs outside HTTP request scope, so it creates a synthetic
 * {@link RequestAttributes} context per tenant to enable
 * {@code TenantContextHolder} and {@code EntityIdAssigner} to function
 * correctly when creating delivery records.
 *
 * @see NotificationDispatchService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledNotificationDispatcher {

    /** Default scheduler interval in milliseconds (60 seconds). */
    public static final long DEFAULT_INTERVAL_MS = 60_000L;

    private final NotificationRepository notificationRepository;
    private final NotificationDispatchService notificationDispatchService;
    private final ApplicationContext applicationContext;

    /**
     * Queries all pending scheduled notifications across tenants and
     * dispatches each one within the appropriate tenant context.
     * <p>
     * Notifications are grouped by tenant to minimize context switches.
     * Individual dispatch failures are caught and logged to prevent one
     * failed notification from blocking the rest.
     */
    @Scheduled(fixedDelayString = "${akademia.notification.scheduler.interval-ms:60000}")
    public void dispatchScheduledNotifications() {
        final LocalDateTime now = LocalDateTime.now();
        final List<NotificationDataModel> pending = notificationRepository.findScheduledBefore(now);

        if (pending.isEmpty()) {
            return;
        }

        log.info("Found {} scheduled notifications due for dispatch", pending.size());

        final Map<Long, List<NotificationDataModel>> byTenant = pending.stream()
                .collect(Collectors.groupingBy(NotificationDataModel::getTenantId));

        for (Map.Entry<Long, List<NotificationDataModel>> entry : byTenant.entrySet()) {
            executeInTenantContext(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Executes notification dispatches within a synthetic request scope
     * for the given tenant.
     * <p>
     * Creates a minimal {@link RequestAttributes} implementation and
     * sets the tenant ID on the request-scoped {@code TenantContextHolder}
     * bean, enabling {@code EntityIdAssigner} to function correctly.
     *
     * @param tenantId      the tenant to set in context
     * @param notifications the notifications to dispatch
     */
    void executeInTenantContext(Long tenantId, List<NotificationDataModel> notifications) {
        RequestContextHolder.setRequestAttributes(new SchedulerRequestAttributes());
        try {
            applicationContext.getBean(
                    com.akademiaplus.infra.persistence.config.TenantContextHolder.class
            ).setTenantId(tenantId);

            for (NotificationDataModel notification : notifications) {
                dispatchSafely(notification);
            }
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    /**
     * Dispatches a single notification, catching and logging any exceptions.
     *
     * @param notification the notification to dispatch
     */
    void dispatchSafely(NotificationDataModel notification) {
        try {
            notificationDispatchService.dispatch(notification);
        } catch (RuntimeException e) {
            log.error("Failed to dispatch notification {} for tenant {}: {}",
                    notification.getNotificationId(),
                    notification.getTenantId(),
                    e.getMessage(), e);
        }
    }

    /**
     * Minimal {@link RequestAttributes} implementation for scheduler threads.
     * <p>
     * Provides a simple attribute store so that request-scoped beans
     * (like {@code TenantContextHolder}) can be created and used outside
     * of an actual HTTP request context.
     */
    static class SchedulerRequestAttributes implements RequestAttributes {

        private final Map<String, Object> attributes = new ConcurrentHashMap<>();

        @Override
        public Object getAttribute(String name, int scope) {
            return attributes.get(name);
        }

        @Override
        public void setAttribute(String name, Object value, int scope) {
            attributes.put(name, value);
        }

        @Override
        public void removeAttribute(String name, int scope) {
            attributes.remove(name);
        }

        @Override
        public String[] getAttributeNames(int scope) {
            return attributes.keySet().toArray(new String[0]);
        }

        @Override
        public void registerDestructionCallback(String name, Runnable callback, int scope) {
            // No-op for scheduler context — no destruction lifecycle
        }

        @Override
        public Object resolveReference(String key) {
            return null;
        }

        @Override
        public String getSessionId() {
            return "scheduler-" + Thread.currentThread().getName();
        }

        @Override
        public Object getSessionMutex() {
            return this;
        }
    }
}
```

### 5.3 — Create ScheduledNotificationDispatcherTest

**File**: `notification-system/src/test/java/com/akademiaplus/notification/usecases/ScheduledNotificationDispatcherTest.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.notification.interfaceadapters.NotificationRepository;
import com.akademiaplus.notifications.NotificationDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@DisplayName("ScheduledNotificationDispatcher")
@ExtendWith(MockitoExtension.class)
class ScheduledNotificationDispatcherTest {

    private static final Long TENANT_ID_1 = 1L;
    private static final Long TENANT_ID_2 = 2L;
    private static final Long NOTIFICATION_ID_1 = 10L;
    private static final Long NOTIFICATION_ID_2 = 20L;
    private static final Long TARGET_USER_ID = 100L;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private ScheduledNotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new ScheduledNotificationDispatcher(
                notificationRepository,
                notificationDispatchService,
                applicationContext
        );
    }

    private NotificationDataModel buildNotification(Long tenantId, Long notificationId) {
        NotificationDataModel notification = new NotificationDataModel();
        notification.setTenantId(tenantId);
        notification.setNotificationId(notificationId);
        notification.setTargetUserId(TARGET_USER_ID);
        return notification;
    }

    @Nested
    @DisplayName("Query")
    class Query {

        @Test
        @DisplayName("Should query pending notifications with current time")
        void shouldQueryPendingNotifications_whenDispatching() {
            // Given
            when(notificationRepository.findScheduledBefore(org.mockito.ArgumentMatchers.isA(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            dispatcher.dispatchScheduledNotifications();

            // Then
            verify(notificationRepository).findScheduledBefore(org.mockito.ArgumentMatchers.isA(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Dispatch")
    class Dispatch {

        @Test
        @DisplayName("Should dispatch each pending notification")
        void shouldDispatchEachNotification_whenPendingExist() {
            // Given
            NotificationDataModel notification1 = buildNotification(TENANT_ID_1, NOTIFICATION_ID_1);
            NotificationDataModel notification2 = buildNotification(TENANT_ID_1, NOTIFICATION_ID_2);
            when(notificationRepository.findScheduledBefore(org.mockito.ArgumentMatchers.isA(LocalDateTime.class)))
                    .thenReturn(List.of(notification1, notification2));
            when(applicationContext.getBean(TenantContextHolder.class)).thenReturn(tenantContextHolder);

            // When
            dispatcher.dispatchScheduledNotifications();

            // Then
            verify(notificationDispatchService).dispatch(notification1);
            verify(notificationDispatchService).dispatch(notification2);
        }

        @Test
        @DisplayName("Should not dispatch when no pending notifications")
        void shouldNotDispatch_whenNoPendingNotifications() {
            // Given
            when(notificationRepository.findScheduledBefore(org.mockito.ArgumentMatchers.isA(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            dispatcher.dispatchScheduledNotifications();

            // Then
            verifyNoInteractions(notificationDispatchService);
        }

        @Test
        @DisplayName("Should continue dispatching when one notification fails")
        void shouldContinueDispatching_whenOneNotificationFails() {
            // Given
            NotificationDataModel notification1 = buildNotification(TENANT_ID_1, NOTIFICATION_ID_1);
            NotificationDataModel notification2 = buildNotification(TENANT_ID_1, NOTIFICATION_ID_2);
            when(notificationRepository.findScheduledBefore(org.mockito.ArgumentMatchers.isA(LocalDateTime.class)))
                    .thenReturn(List.of(notification1, notification2));
            when(applicationContext.getBean(TenantContextHolder.class)).thenReturn(tenantContextHolder);
            doThrow(new RuntimeException("Dispatch failed")).when(notificationDispatchService).dispatch(notification1);

            // When
            dispatcher.dispatchScheduledNotifications();

            // Then — second notification still dispatched despite first failure
            verify(notificationDispatchService).dispatch(notification2);
        }
    }

    @Nested
    @DisplayName("Tenant Context")
    class TenantContext {

        @Test
        @DisplayName("Should set tenant context for each tenant group")
        void shouldSetTenantContext_forEachTenantGroup() {
            // Given
            NotificationDataModel notification1 = buildNotification(TENANT_ID_1, NOTIFICATION_ID_1);
            NotificationDataModel notification2 = buildNotification(TENANT_ID_2, NOTIFICATION_ID_2);
            when(notificationRepository.findScheduledBefore(org.mockito.ArgumentMatchers.isA(LocalDateTime.class)))
                    .thenReturn(List.of(notification1, notification2));
            when(applicationContext.getBean(TenantContextHolder.class)).thenReturn(tenantContextHolder);

            // When
            dispatcher.dispatchScheduledNotifications();

            // Then
            verify(tenantContextHolder).setTenantId(TENANT_ID_1);
            verify(tenantContextHolder).setTenantId(TENANT_ID_2);
        }
    }
}
```

> **Note on `any()` matchers**: The `LocalDateTime.now()` call in the production code produces a non-deterministic value that changes between test setup and execution. Using `isA(LocalDateTime.class)` here is the correct approach per the project's strict stubbing conventions — it verifies the parameter type without using the forbidden `any()` matcher. If a more precise assertion is needed, use `ArgumentCaptor<LocalDateTime>` and assert the captured value is within a reasonable time window.

### 5.4 — Verify Phase 5

```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl notification-system -am -f platform-core-api/pom.xml

# Full project build (no cross-module breakage)
mvn clean install -DskipTests -DskipITs -f platform-core-api/pom.xml
```

### Commit Phase 5

```bash
git add notification-system/src/main/java/com/akademiaplus/config/NotificationSchedulingConfiguration.java \
      notification-system/src/main/java/com/akademiaplus/notification/usecases/ScheduledNotificationDispatcher.java \
      notification-system/src/test/java/com/akademiaplus/notification/usecases/ScheduledNotificationDispatcherTest.java
git commit -m "feat(notification-system): add scheduled notification dispatcher with tenant context support

ScheduledNotificationDispatcher runs on configurable interval (default 60s),
queries pending scheduled notifications across all tenants, groups by tenant,
and dispatches within a synthetic request scope. SchedulerRequestAttributes
inner class enables TenantContextHolder and EntityIdAssigner in non-HTTP
context. NotificationSchedulingConfiguration enables @EnableScheduling.
Unit tests cover query execution, dispatch delegation, error isolation,
and per-tenant context setup."
```

---

## Final Verification

After all five phases are committed:

```bash
# Full build with all tests
mvn clean install -DskipITs -f platform-core-api/pom.xml

# Convention compliance checks
grep -rn "any()" notification-system/src/test/ | grep -v "import" | grep -v "isA" | wc -l  # must be 0
grep -rn "// Arrange" notification-system/src/test/ | wc -l                                 # must be 0
grep -rn "catch (Exception " notification-system/src/main/ | wc -l                         # must be 0

# Copyright header on all new files
for f in $(cd platform-core-api && git diff --name-only --diff-filter=A HEAD~5 -- notification-system/); do
  head -1 "platform-core-api/$f" | grep -q "Copyright" || echo "MISSING: $f"
done
```

---

## File Path Summary

All paths relative to `platform-core-api/`:

| Phase | File | Action |
|-------|------|--------|
| 1 | `notification-system/src/main/java/com/akademiaplus/notification/usecases/SseEmitterRegistry.java` | Create |
| 1 | `notification-system/src/test/java/com/akademiaplus/notification/usecases/SseEmitterRegistryTest.java` | Create |
| 2 | `notification-system/src/main/java/com/akademiaplus/notification/usecases/DeliveryChannelStrategy.java` | Create |
| 2 | `notification-system/src/main/java/com/akademiaplus/notification/usecases/DeliveryResult.java` | Create |
| 2 | `notification-system/src/main/java/com/akademiaplus/notification/usecases/WebappDeliveryChannelStrategy.java` | Create |
| 2 | `notification-system/src/test/java/com/akademiaplus/notification/usecases/WebappDeliveryChannelStrategyTest.java` | Create |
| 3 | `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/NotificationRepository.java` | Modify |
| 3 | `notification-system/src/main/java/com/akademiaplus/notification/usecases/NotificationDispatchService.java` | Create |
| 3 | `notification-system/src/test/java/com/akademiaplus/notification/usecases/NotificationDispatchServiceTest.java` | Create |
| 4 | `notification-system/src/main/resources/openapi/notification.yaml` | Modify |
| 4 | `notification-system/src/main/resources/openapi/notification-system-module.yaml` | Modify |
| 4 | `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/SseController.java` | Create |
| 4 | `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/NotificationController.java` | Modify |
| 5 | `notification-system/src/main/java/com/akademiaplus/config/NotificationSchedulingConfiguration.java` | Create |
| 5 | `notification-system/src/main/java/com/akademiaplus/notification/usecases/ScheduledNotificationDispatcher.java` | Create |
| 5 | `notification-system/src/test/java/com/akademiaplus/notification/usecases/ScheduledNotificationDispatcherTest.java` | Create |
