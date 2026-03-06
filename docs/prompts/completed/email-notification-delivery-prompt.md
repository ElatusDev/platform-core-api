# Email Notification Delivery — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Spec**: `docs/workflows/pending/email-notification-delivery-workflow.md` — read this first.
**Prerequisites**: Read `docs/directives/CLAUDE.md` and `docs/directives/AI-CODE-REF.md` before writing any code.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (1 → 2 → ... → 10). Do NOT skip ahead.
2. Before writing any code, read the existing files listed in each phase's "Read first" section.
3. **Compile gate**: After each phase that produces code, run the specified verification command. Fix all errors before proceeding.
4. **Test gate**: After each phase that creates tests, run the specified test command. Fix all failures before proceeding.
5. All new files MUST include the ElatusDev copyright header.
6. All `public` classes and methods MUST have Javadoc.
7. Test methods: `shouldDoX_whenGivenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.
8. All string literals → `public static final` constants, shared between impl and tests.
9. Use `applicationContext.getBean()` for all entity instantiation — never `new EntityDataModel()`.
10. Read existing files BEFORE modifying — field names, import paths, and CompositeId class names vary.
11. Commit after each phase using the commit message provided.

---

## Phase 1: DeliveryStatus Enum Expansion

### Read first

```bash
cat multi-tenant-data/src/main/java/com/akademiaplus/notifications/DeliveryStatus.java
cat notification-system/src/main/java/com/akademiaplus/notification/usecases/DeliveryResult.java
```

Verify no exhaustive `switch` on `DeliveryStatus`:
```bash
grep -rn "switch.*DeliveryStatus\|case PENDING\|case SENT\|case DELIVERED\|case ACKNOWLEDGED\|case FAILED\|case EXPIRED" notification-system/src/main/java/ multi-tenant-data/src/main/java/
```

### Step 1.1: Add enum values

**File**: `multi-tenant-data/src/main/java/com/akademiaplus/notifications/DeliveryStatus.java`

Add 3 values after `EXPIRED`:

```java
public enum DeliveryStatus {
    PENDING,
    SENT,
    DELIVERED,
    ACKNOWLEDGED,
    FAILED,
    EXPIRED,
    OPENED,
    CLICKED,
    BOUNCED
}
```

### Verify Phase 1

```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests -f platform-core-api/pom.xml
```

### Commit Phase 1

```bash
git add multi-tenant-data/
git commit -m "feat(multi-tenant-data): add email-specific DeliveryStatus values

Add OPENED, CLICKED, BOUNCED to DeliveryStatus enum for email
delivery tracking. Aligns with email-notification.yaml spec."
```

---

## Phase 2: Template Entities

### Read first

```bash
cat multi-tenant-data/src/main/java/com/akademiaplus/notifications/email/EmailDataModel.java
cat multi-tenant-data/src/main/java/com/akademiaplus/notifications/email/EmailRecipientDataModel.java
cat multi-tenant-data/src/main/java/com/akademiaplus/infra/persistence/model/TenantScoped.java
```

Find the DB schema files:
```bash
find . -name "00-schema*.sql" | head -10
```

### Step 2.1: Create EmailTemplateDataModel

**File**: `multi-tenant-data/src/main/java/com/akademiaplus/notifications/email/EmailTemplateDataModel.java`

Follow the exact pattern of `EmailDataModel`:
- `@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Scope("prototype") @Component`
- `@Entity @Table(name = "email_templates")`
- `@SQLDelete(sql = "UPDATE email_templates SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND template_id = ?")`
- `@IdClass(EmailTemplateDataModel.EmailTemplateCompositeId.class)`
- Extends `TenantScoped`
- Inner `@Data` static class `EmailTemplateCompositeId` with `tenantId` (Long) and `templateId` (Long)

Fields:
```java
@Id
@Column(name = "template_id")
private Long templateId;

@Column(name = "name", nullable = false, length = 100)
private String name;

@Column(name = "description", length = 500)
private String description;

@Column(name = "category", length = 50)
private String category;

@Column(name = "subject_template", nullable = false, length = 255)
private String subjectTemplate;

@Column(name = "body_html", nullable = false, columnDefinition = "TEXT")
private String bodyHtml;

@Column(name = "body_text", columnDefinition = "TEXT")
private String bodyText;

@Column(name = "is_active", nullable = false)
private boolean isActive;

@OneToMany(mappedBy = "template", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
private List<EmailTemplateVariableDataModel> variables;
```

### Step 2.2: Create EmailTemplateVariableDataModel

**File**: `multi-tenant-data/src/main/java/com/akademiaplus/notifications/email/EmailTemplateVariableDataModel.java`

Follow the exact pattern of `EmailRecipientDataModel`:
- Same annotations, extends `TenantScoped`
- `@SQLDelete(sql = "UPDATE email_template_variables SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND template_variable_id = ?")`
- `@IdClass(EmailTemplateVariableDataModel.EmailTemplateVariableCompositeId.class)`
- Inner `@Data` static class `EmailTemplateVariableCompositeId` with `tenantId` (Long) and `templateVariableId` (Long)

Fields:
```java
@Id
@Column(name = "template_variable_id")
private Long templateVariableId;

@Column(name = "template_id", nullable = false)
private Long templateId;

@Column(name = "name", nullable = false, length = 50)
private String name;

@Column(name = "variable_type", nullable = false, length = 20)
private String variableType;

@Column(name = "description", length = 200)
private String description;

@Column(name = "is_required", nullable = false)
private boolean required;

@Column(name = "default_value", length = 255)
private String defaultValue;

@ManyToOne(optional = false, fetch = FetchType.LAZY)
@JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable = false, updatable = false)
@JoinColumn(name = "template_id", referencedColumnName = "template_id", insertable = false, updatable = false)
private EmailTemplateDataModel template;
```

### Step 2.3: Update DB Schema

Read the schema file first, then add the `email_templates` and `email_template_variables` table definitions after the existing `email_attachments` table. Add to both `db_init/00-schema-qa.sql` and the test schema file.

```sql
CREATE TABLE IF NOT EXISTS email_templates (
    tenant_id    BIGINT      NOT NULL,
    template_id  BIGINT      NOT NULL,
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    category     VARCHAR(50),
    subject_template VARCHAR(255) NOT NULL,
    body_html    TEXT        NOT NULL,
    body_text    TEXT,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP,
    deleted_at   TIMESTAMP,
    PRIMARY KEY (tenant_id, template_id),
    INDEX idx_template_tenant_category (tenant_id, category),
    INDEX idx_template_tenant_active (tenant_id, is_active),
    INDEX idx_template_name (tenant_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS email_template_variables (
    tenant_id            BIGINT      NOT NULL,
    template_variable_id BIGINT      NOT NULL,
    template_id          BIGINT      NOT NULL,
    name                 VARCHAR(50) NOT NULL,
    variable_type        VARCHAR(20) NOT NULL,
    description          VARCHAR(200),
    is_required          BOOLEAN     NOT NULL DEFAULT FALSE,
    default_value        VARCHAR(255),
    created_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP,
    deleted_at           TIMESTAMP,
    PRIMARY KEY (tenant_id, template_variable_id),
    INDEX idx_template_var_template (tenant_id, template_id),
    CONSTRAINT fk_template_var_template
        FOREIGN KEY (tenant_id, template_id)
        REFERENCES email_templates (tenant_id, template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Verify Phase 2

```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests -f platform-core-api/pom.xml
```

### Commit Phase 2

```bash
git add multi-tenant-data/ db_init/
git commit -m "feat(multi-tenant-data): add email template entities and schema

Add EmailTemplateDataModel (composite key tenantId+templateId) and
EmailTemplateVariableDataModel with @OneToMany relationship.
Add email_templates and email_template_variables DB tables."
```

---

## Phase 3: OpenAPI Spec Wiring + DTO Generation

### Read first

```bash
cat notification-system/src/main/resources/openapi/notification-system-module.yaml
cat notification-system/src/main/resources/openapi/email-notification.yaml
```

Note the exact path reference format used for notification.yaml paths (tilde escaping).

### Step 3.1: Expand email path references

**File**: `notification-system/src/main/resources/openapi/notification-system-module.yaml`

Replace the single `/email` entry:

```yaml
  # ===== EMAIL NOTIFICATION ENDPOINTS =====
  /email:
    $ref: './email-notification.yaml#/paths'
    description: |
      ...
```

With individual path entries:

```yaml
  # ===== EMAIL NOTIFICATION ENDPOINTS =====
  '/email/notifications/{notificationId}/deliveries':
    $ref: './email-notification.yaml#/paths/~1notifications~1{notificationId}~1deliveries'
  '/email/deliveries/{deliveryId}':
    $ref: './email-notification.yaml#/paths/~1deliveries~1{deliveryId}'
  '/email/deliveries/{deliveryId}/retry':
    $ref: './email-notification.yaml#/paths/~1deliveries~1{deliveryId}~1retry'
  '/email/send':
    $ref: './email-notification.yaml#/paths/~1send'
  '/email/templates':
    $ref: './email-notification.yaml#/paths/~1templates'
  '/email/templates/{templateId}':
    $ref: './email-notification.yaml#/paths/~1templates~1{templateId}'
  '/email/templates/{templateId}/preview':
    $ref: './email-notification.yaml#/paths/~1templates~1{templateId}~1preview'
```

### Step 3.2: Add email schema references

Under `components.schemas`, add after existing schema entries:

```yaml
    # Email delivery schemas
    CreateEmailDeliveryRequest:
      $ref: './email-notification.yaml#/components/schemas/CreateEmailDeliveryRequest'
    EmailDeliveryResponse:
      $ref: './email-notification.yaml#/components/schemas/EmailDeliveryResponse'
    EmailDeliveryDetailResponse:
      $ref: './email-notification.yaml#/components/schemas/EmailDeliveryDetailResponse'
    EmailDeliveryListResponse:
      $ref: './email-notification.yaml#/components/schemas/EmailDeliveryListResponse'
    UpdateEmailDeliveryStatusRequest:
      $ref: './email-notification.yaml#/components/schemas/UpdateEmailDeliveryStatusRequest'
    ImmediateEmailNotificationRequest:
      $ref: './email-notification.yaml#/components/schemas/ImmediateEmailNotificationRequest'
    ImmediateEmailDeliveryResponse:
      $ref: './email-notification.yaml#/components/schemas/ImmediateEmailDeliveryResponse'
    CreateEmailTemplateRequest:
      $ref: './email-notification.yaml#/components/schemas/CreateEmailTemplateRequest'
    UpdateEmailTemplateRequest:
      $ref: './email-notification.yaml#/components/schemas/UpdateEmailTemplateRequest'
    EmailTemplateResponse:
      $ref: './email-notification.yaml#/components/schemas/EmailTemplateResponse'
    EmailTemplateListResponse:
      $ref: './email-notification.yaml#/components/schemas/EmailTemplateListResponse'
    EmailTemplatePreviewRequest:
      $ref: './email-notification.yaml#/components/schemas/EmailTemplatePreviewRequest'
    EmailTemplatePreviewResponse:
      $ref: './email-notification.yaml#/components/schemas/EmailTemplatePreviewResponse'
```

### Step 3.3: Regenerate DTOs

```bash
mvn clean generate-sources -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

### Step 3.4: Verify generated code

```bash
find notification-system/target/generated-sources -name "*Email*" -type f | sort
find notification-system/target/generated-sources -name "*Api.java" | sort
```

Confirm:
- DTO classes generated: `CreateEmailDeliveryRequestDTO`, `EmailDeliveryResponseDTO`, `ImmediateEmailNotificationRequestDTO`, etc.
- API interfaces generated — note the exact names (likely `EmailDeliveryApi`, `ImmediateEmailApi`, `EmailTemplatesApi` based on OpenAPI tags)

**IMPORTANT**: Read the generated API interface files and note the exact method signatures. You will implement these in Phase 8.

### Verify Phase 3

```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

### Commit Phase 3

```bash
git add notification-system/src/main/resources/openapi/
git commit -m "api(notification-system): wire email notification OpenAPI paths and schemas

Expand notification-system-module.yaml to reference individual email
endpoints. Add schema refs for all email DTOs. Generated API interfaces
and DTOs for delivery, immediate send, and template operations."
```

---

## Phase 4: Email Configuration

### Read first

```bash
cat notification-system/pom.xml
cat application/src/main/resources/application.properties
cat notification-system/src/main/java/com/akademiaplus/config/NotificationModelMapperConfiguration.java
```

### Step 4.1: Add spring-boot-starter-mail dependency

**File**: `notification-system/pom.xml`

Add in the `<dependencies>` section:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

### Step 4.2: Create EmailConfiguration

**File**: `notification-system/src/main/java/com/akademiaplus/config/EmailConfiguration.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configures the {@link JavaMailSender} bean for SMTP-based email delivery.
 *
 * <p>Supports any SMTP-compatible provider (AWS SES, Mailtrap, etc.)
 * via externalized properties.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
public class EmailConfiguration {

    /** Default SMTP port for STARTTLS connections. */
    public static final int DEFAULT_SMTP_PORT = 587;

    /** JavaMail property key for transport protocol. */
    public static final String PROP_TRANSPORT_PROTOCOL = "mail.transport.protocol";

    /** JavaMail property key for SMTP authentication. */
    public static final String PROP_SMTP_AUTH = "mail.smtp.auth";

    /** JavaMail property key for STARTTLS enablement. */
    public static final String PROP_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";

    /** JavaMail property key for STARTTLS requirement. */
    public static final String PROP_SMTP_STARTTLS_REQUIRED = "mail.smtp.starttls.required";

    /**
     * Creates a {@link JavaMailSender} configured with SMTP properties.
     *
     * @param host     SMTP server hostname
     * @param port     SMTP server port
     * @param username SMTP authentication username
     * @param password SMTP authentication password
     * @return configured JavaMailSender
     */
    @Bean
    public JavaMailSender emailMailSender(
            @Value("${akademia.email.host}") String host,
            @Value("${akademia.email.port}") int port,
            @Value("${akademia.email.username}") String username,
            @Value("${akademia.email.password}") String password) {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put(PROP_TRANSPORT_PROTOCOL, "smtp");
        props.put(PROP_SMTP_AUTH, "true");
        props.put(PROP_SMTP_STARTTLS_ENABLE, "true");
        props.put(PROP_SMTP_STARTTLS_REQUIRED, "true");

        return mailSender;
    }
}
```

### Step 4.3: Add SMTP properties

**File**: `application/src/main/resources/application.properties`

Append at the end:

```properties
# Email SMTP Configuration (AWS SES via SMTP)
akademia.email.host=${EMAIL_SMTP_HOST:email-smtp.us-east-1.amazonaws.com}
akademia.email.port=${EMAIL_SMTP_PORT:587}
akademia.email.username=${EMAIL_SMTP_USERNAME:}
akademia.email.password=${EMAIL_SMTP_PASSWORD:}
akademia.email.from-address=${EMAIL_FROM_ADDRESS:noreply@akademiaplus.com}
akademia.email.from-name=${EMAIL_FROM_NAME:AkademiaPlus}
```

### Verify Phase 4

```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

### Commit Phase 4

```bash
git add notification-system/pom.xml notification-system/src/main/java/com/akademiaplus/config/EmailConfiguration.java application/src/main/resources/application.properties
git commit -m "feat(notification-system): add Jakarta Mail configuration

Add spring-boot-starter-mail dependency and EmailConfiguration bean
with SMTP/STARTTLS properties. Add env-backed SMTP config properties
for AWS SES compatibility."
```

---

## Phase 5: EmailDeliveryChannelStrategy + Dispatch Parameterization

### Read first

```bash
cat notification-system/src/main/java/com/akademiaplus/notification/usecases/WebappDeliveryChannelStrategy.java
cat notification-system/src/main/java/com/akademiaplus/notification/usecases/DeliveryChannelStrategy.java
cat notification-system/src/main/java/com/akademiaplus/notification/usecases/DeliveryResult.java
cat notification-system/src/main/java/com/akademiaplus/notification/usecases/NotificationDispatchService.java
```

Find the email repository:
```bash
find notification-system -name "EmailRepository.java" | head -1
cat <result>
```

### Step 5.1: Create EmailDeliveryChannelStrategy

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/EmailDeliveryChannelStrategy.java`

Mirror `WebappDeliveryChannelStrategy` structure:
- `@Slf4j @Service @RequiredArgsConstructor`
- Implements `DeliveryChannelStrategy`
- Dependencies: `JavaMailSender javaMailSender`, `EmailRepository emailRepository`
- `@Value("${akademia.email.from-address}") String fromAddress`
- `@Value("${akademia.email.from-name}") String fromName`
- `getChannel()` → `DeliveryChannel.EMAIL`
- `deliver(notification, recipientIdentifier)`:
  1. Look up `EmailDataModel` by notification's subject/metadata to find email content
  2. Build `MimeMessage` via `javaMailSender.createMimeMessage()`
  3. Use `MimeMessageHelper` to set from, to, subject, HTML body
  4. Call `javaMailSender.send(message)`
  5. Return `DeliveryResult.sent()` on success
  6. Catch `MailException` → return `DeliveryResult.failed(String.format(ERROR_SEND_FAILED, recipientIdentifier, e.getMessage()))`

Constants:
```java
public static final String ERROR_SEND_FAILED = "Failed to send email to %s: %s";
public static final String ERROR_EMAIL_NOT_FOUND = "Email not found for notification: %s";
```

### Step 5.2: Parameterize NotificationDispatchService

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/NotificationDispatchService.java`

Add overloaded method — keep existing `dispatch(notification)` as backward-compatible default:

```java
/**
 * Dispatches a notification via the specified delivery channel.
 *
 * @param notification the notification to dispatch
 * @param channel      the delivery channel to use
 * @return the saved delivery record
 * @throws IllegalArgumentException if the notification has no targetUserId
 * @throws IllegalStateException    if no strategy is registered for the channel
 */
@Transactional
public NotificationDeliveryDataModel dispatch(NotificationDataModel notification, DeliveryChannel channel) {
    if (notification.getTargetUserId() == null) {
        throw new IllegalArgumentException(ERROR_TARGET_USER_REQUIRED);
    }
    final DeliveryChannelStrategy strategy = resolveStrategy(channel);
    final String recipientIdentifier = String.valueOf(notification.getTargetUserId());
    DeliveryResult result = strategy.deliver(notification, recipientIdentifier);
    return persistDeliveryRecord(notification, channel, recipientIdentifier, result);
}
```

Refactor the existing `dispatch(notification)` to delegate:

```java
@Transactional
public NotificationDeliveryDataModel dispatch(NotificationDataModel notification) {
    return dispatch(notification, DeliveryChannel.WEBAPP);
}
```

### Verify Phase 5

```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

### Commit Phase 5

```bash
git add notification-system/src/main/java/com/akademiaplus/notification/usecases/
git commit -m "feat(notification-system): add EmailDeliveryChannelStrategy and parameterize dispatch

Implement EMAIL channel strategy using JavaMailSender and MimeMessage.
Add overloaded dispatch(notification, channel) to NotificationDispatchService
while preserving backward-compatible dispatch(notification) defaulting to WEBAPP."
```

---

## Phase 6: Email Use Cases

### Read first

```bash
cat notification-system/src/main/java/com/akademiaplus/notification/usecases/NotificationCreationUseCase.java
```

Find existing use cases for creation pattern:
```bash
grep -rn "class.*CreationUseCase" notification-system/src/main/java/
```

Find existing repositories:
```bash
find notification-system -name "*Repository.java" -path "*/interfaceadapters/*" | sort
```

### Step 6.1: Create EmailDeliveryConfig record

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/domain/EmailDeliveryConfig.java`

```java
public record EmailDeliveryConfig(
    String fromEmail,
    String fromName,
    String replyTo,
    String priority,
    Long templateId,
    Map<String, Object> templateData,
    List<String> attachmentUrls,
    Map<String, String> customHeaders,
    boolean trackingEnabled
) {}
```

### Step 6.2: Create EmailTemplateRepository

**File**: `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/EmailTemplateRepository.java`

```java
public interface EmailTemplateRepository extends TenantScopedRepository<EmailTemplateDataModel, EmailTemplateDataModel.EmailTemplateCompositeId> {
}
```

### Step 6.3: Create EmailTemplateVariableRepository

**File**: `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/EmailTemplateVariableRepository.java`

```java
public interface EmailTemplateVariableRepository extends TenantScopedRepository<EmailTemplateVariableDataModel, EmailTemplateVariableDataModel.EmailTemplateVariableCompositeId> {
}
```

### Step 6.4: Create EmailCreationUseCase

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/EmailCreationUseCase.java`

Two-method pattern:
- `create(dto)` — `@Transactional`, calls `transform()`, saves via `emailRepository.save()`
- `transform(dto)` — gets `EmailDataModel` prototype bean, maps fields, creates recipients/attachments

### Step 6.5: Create EmailDeliveryManagementUseCase

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/EmailDeliveryManagementUseCase.java`

Methods: `createDelivery()`, `getDeliveriesByNotificationId()`, `getDeliveryById()`, `updateDeliveryStatus()`, `retryDelivery()`.

### Step 6.6: Create ImmediateSendUseCase

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/ImmediateSendUseCase.java`

Orchestrates: build MimeMessage per recipient → send → collect results → return aggregated response.

### Verify Phase 6

```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

### Commit Phase 6

```bash
git add notification-system/src/main/java/
git commit -m "feat(notification-system): add email use cases and repositories

Add EmailCreationUseCase (two-method pattern), EmailDeliveryManagementUseCase
(CRUD + retry), ImmediateSendUseCase (fire-and-forget). Add EmailDeliveryConfig
domain record, EmailTemplateRepository, EmailTemplateVariableRepository."
```

---

## Phase 7: Template Use Cases

### Read first

```bash
cat notification-system/src/main/java/com/akademiaplus/notification/usecases/EmailCreationUseCase.java
```

### Step 7.1: Create EmailTemplateCreationUseCase

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/EmailTemplateCreationUseCase.java`

Two-method pattern. Creates template + variables.

### Step 7.2: Create EmailTemplateUpdateUseCase

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/EmailTemplateUpdateUseCase.java`

Finds template by ID, updates fields, syncs variable set. `@Transactional`.

### Step 7.3: Create GetEmailTemplateByIdUseCase

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/GetEmailTemplateByIdUseCase.java`

Finds template or throws not-found exception.

### Step 7.4: Create ListEmailTemplatesUseCase

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/ListEmailTemplatesUseCase.java`

Paginated query with optional category filter.

### Step 7.5: Create EmailTemplatePreviewUseCase

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/EmailTemplatePreviewUseCase.java`

Loads template, delegates rendering to `EmailTemplateRenderingService`, returns preview DTO.

### Step 7.6: Create EmailTemplateRenderingService

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/EmailTemplateRenderingService.java`

Simple `{{variable}}` placeholder replacement using `Pattern.compile("\\{\\{(\\w+)\\}\\}")`.

```java
@Service
public class EmailTemplateRenderingService {

    public static final String VARIABLE_PATTERN = "\\{\\{(\\w+)\\}\\}";
    public static final String ERROR_TEMPLATE_NOT_FOUND = "Email template not found: %s";

    /**
     * Renders a template string by replacing {{variableName}} placeholders.
     */
    public String render(String template, Map<String, Object> variables) {
        // Regex-based replacement of {{var}} patterns
    }
}
```

### Verify Phase 7

```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

### Commit Phase 7

```bash
git add notification-system/src/main/java/
git commit -m "feat(notification-system): add email template use cases and rendering service

Add EmailTemplateCreationUseCase, EmailTemplateUpdateUseCase,
GetEmailTemplateByIdUseCase, ListEmailTemplatesUseCase,
EmailTemplatePreviewUseCase, and EmailTemplateRenderingService
with simple {{variable}} placeholder replacement."
```

---

## Phase 8: Controllers + ModelMapper Config + Controller Advice

### Read first

```bash
cat notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/NotificationController.java
cat notification-system/src/main/java/com/akademiaplus/config/NotificationModelMapperConfiguration.java
cat notification-system/src/main/java/com/akademiaplus/config/NotificationControllerAdvice.java
```

Read the generated API interfaces:
```bash
find notification-system/target/generated-sources -name "*Api.java" | sort
```

Read each API interface to get exact method signatures.

### Step 8.1: Create EmailDeliveryController

**File**: `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/EmailDeliveryController.java`

- `@RestController @RequestMapping("/v1/notification-system")`
- Implements generated `EmailDeliveryApi` (verify exact name)
- Dependencies: `EmailDeliveryManagementUseCase`, `ModelMapper`
- Implement all 5 endpoints (create, list, getById, updateStatus, retry)

### Step 8.2: Create ImmediateEmailController

**File**: `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/ImmediateEmailController.java`

- Implements generated `ImmediateEmailApi` (verify exact name)
- Single method delegates to `ImmediateSendUseCase.send()`

### Step 8.3: Create EmailTemplateController

**File**: `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/EmailTemplateController.java`

- Implements generated `EmailTemplatesApi` (verify exact name)
- Dependencies: creation, update, getById, list, preview use cases + `ModelMapper`
- Implement all 5 endpoints (list, create, getById, update, preview)

### Step 8.4: Update NotificationModelMapperConfiguration

**File**: `notification-system/src/main/java/com/akademiaplus/config/NotificationModelMapperConfiguration.java`

Add named TypeMaps for email and template DTOs. Follow the existing sandwich pattern (`setImplicitMappingEnabled(false/true)`). Skip entity ID and nested object setters.

### Step 8.5: Update NotificationControllerAdvice

**File**: `notification-system/src/main/java/com/akademiaplus/config/NotificationControllerAdvice.java`

Expand `basePackageClasses`:
```java
@ControllerAdvice(basePackageClasses = {
    NotificationController.class,
    EmailDeliveryController.class,
    ImmediateEmailController.class,
    EmailTemplateController.class
})
```

### Verify Phase 8

```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

### Commit Phase 8

```bash
git add notification-system/src/main/java/
git commit -m "feat(notification-system): add email controllers and ModelMapper config

Add EmailDeliveryController (5 endpoints), ImmediateEmailController
(1 endpoint), EmailTemplateController (5 endpoints). Add email and
template TypeMaps to NotificationModelMapperConfiguration. Expand
NotificationControllerAdvice to cover email controllers."
```

---

## Phase 9: Unit Tests

### Read first

```bash
cat notification-system/src/test/java/com/akademiaplus/notification/usecases/WebappDeliveryChannelStrategyTest.java
cat notification-system/src/test/java/com/akademiaplus/notification/usecases/NotificationDispatchServiceTest.java
```

Follow the exact test structure patterns.

### Step 9.1: Create test directory structure

```bash
ls notification-system/src/test/java/com/akademiaplus/notification/usecases/
ls notification-system/src/test/java/com/akademiaplus/notification/interfaceadapters/
ls notification-system/src/test/java/com/akademiaplus/config/
```

### Step 9.2: Create unit test classes (14 total)

Create one test class per production class. Follow conventions:
- `@ExtendWith(MockitoExtension.class)`
- `@DisplayName` on class and all `@Test` methods
- `@Nested` classes for logical grouping
- Given-When-Then comments
- Zero `any()` matchers
- Constants shared from production classes

| # | Test Class | Key @Nested Groups |
|---|-----------|-------------------|
| 1 | `EmailDeliveryChannelStrategyTest` | ChannelIdentity, Delivery (success, mail exception, email not found) |
| 2 | `EmailCreationUseCaseTest` | Creation, Transform (with recipients, with attachments) |
| 3 | `EmailDeliveryManagementUseCaseTest` | CreateDelivery, GetDeliveries, GetById, UpdateStatus, RetryDelivery |
| 4 | `ImmediateSendUseCaseTest` | Send (success, partial failure, all fail), Validation |
| 5 | `EmailTemplateCreationUseCaseTest` | Creation, Transform |
| 6 | `EmailTemplateUpdateUseCaseTest` | Update (happy path, not found, variable changes) |
| 7 | `GetEmailTemplateByIdUseCaseTest` | Get (found, not found) |
| 8 | `ListEmailTemplatesUseCaseTest` | List (all, empty, paginated) |
| 9 | `EmailTemplatePreviewUseCaseTest` | Preview (all vars, partial, missing required) |
| 10 | `EmailTemplateRenderingServiceTest` | Render (simple, multiple vars, missing var, special characters) |
| 11 | `EmailDeliveryControllerTest` | All 5 endpoints (standalone MockMvc) |
| 12 | `ImmediateEmailControllerTest` | Send endpoint (success, validation error) |
| 13 | `EmailTemplateControllerTest` | All 5 template endpoints (standalone MockMvc) |
| 14 | `EmailConfigurationTest` | Bean creation with valid properties |

### Step 9.3: Update NotificationDispatchServiceTest

Add tests for the new `dispatch(notification, channel)` overload. Existing tests for `dispatch(notification)` should still pass unchanged.

### Verify Phase 9

```bash
mvn test -pl notification-system -am -f platform-core-api/pom.xml
```

### Commit Phase 9

```bash
git add notification-system/src/test/
git commit -m "test(notification-system): add email notification unit tests

14 test classes covering EmailDeliveryChannelStrategy, EmailCreationUseCase,
EmailDeliveryManagementUseCase, ImmediateSendUseCase, 5 template use cases,
EmailTemplateRenderingService, 3 controllers, and EmailConfiguration.
Update NotificationDispatchServiceTest for parameterized dispatch."
```

---

## Phase 10: Component Tests + E2E Tests

### Read first

```bash
find notification-system/src/test -name "*ComponentTest.java" | head -5
```

Read the first component test result for pattern reference.

### Step 10.1: Create EmailComponentTest

**File**: `notification-system/src/test/java/com/akademiaplus/notification/EmailComponentTest.java`

- Full Spring context + Testcontainers MariaDB
- `@MockitoBean JavaMailSender` — prevents real email sends
- Stub `javaMailSender.createMimeMessage()` to return a real `MimeMessage`
- `@Nested` groups: EmailDeliveryLifecycle, ImmediateSend, DeliveryStatusUpdate, RetryDelivery

### Step 10.2: Create EmailTemplateComponentTest

**File**: `notification-system/src/test/java/com/akademiaplus/notification/EmailTemplateComponentTest.java`

- `@Nested` groups: TemplateLifecycle, TemplatePreview, TemplateWithVariables

### Step 10.3: E2E Tests (if applicable)

Add requests to `platform-api-e2e/Postman Collections/platform-api-e2e.json`:
- Template CRUD: create, get, update, list
- Template preview with test data
- Delivery tracking: create, get, update status
- Immediate send: conditional skip without SMTP

### Verify Phase 10

```bash
mvn verify -pl notification-system -am -f platform-core-api/pom.xml
```

### Commit Phase 10

```bash
git add notification-system/src/test/ platform-api-e2e/
git commit -m "test(notification-system): add email component and E2E tests

EmailComponentTest — delivery lifecycle, immediate send, status update,
retry with Testcontainers MariaDB and mocked JavaMailSender.
EmailTemplateComponentTest — template CRUD lifecycle and preview rendering.
E2E requests for template and delivery operations."
```

---

## VERIFICATION CHECKLIST

Run after all phases complete:

- [ ] `mvn clean install -DskipTests -f platform-core-api/pom.xml` — full compilation passes
- [ ] `mvn test -pl notification-system -am -f platform-core-api/pom.xml` — all unit tests green
- [ ] `mvn verify -pl notification-system -am -f platform-core-api/pom.xml` — component tests green
- [ ] All new files have ElatusDev copyright header
- [ ] All public classes and methods have Javadoc
- [ ] All string literals extracted to `public static final` constants
- [ ] All tests use Given-When-Then, zero `any()` matchers
- [ ] Strategy in `usecases/` alongside interface (Hard Rule #12)
- [ ] Domain records in `usecases/domain/` (Hard Rule #13)
- [ ] No `new EntityDataModel()` — all via `applicationContext.getBean()`
- [ ] Workflow mentions `JavaMailSender`, `EmailDeliveryChannelStrategy`, `EmailTemplateDataModel` — confirmed
- [ ] Phase 5 references `WebappDeliveryChannelStrategy` as pattern reference — confirmed
- [ ] Phase 9 lists 14 test classes — confirmed
