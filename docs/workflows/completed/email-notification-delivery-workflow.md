# Email Notification Delivery Workflow — AkademiaPlus

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Module**: `notification-system` (primary), `multi-tenant-data` (entity additions)
**Prerequisite**: Read `docs/directives/CLAUDE.md`, `docs/directives/AI-CODE-REF.md`, and `docs/design/DESIGN.md` before starting. Also read the existing notification-system code (`NotificationDispatchService.java`, `WebappDeliveryChannelStrategy.java`, `DeliveryChannelStrategy.java`, `EmailDataModel.java`, `EmailRecipientDataModel.java`, `EmailAttachmentDataModel.java`).

---

## 1. Architecture Overview

### 1.1 Transport: SMTP via Jakarta Mail

The EMAIL delivery channel uses Jakarta Mail (`JavaMailSender`) to send emails through a standard SMTP server. In production, this targets AWS SES via its SMTP endpoint — no vendor SDK required. The configuration is externalized via Spring `@Value` properties, allowing any SMTP-compatible provider.

### 1.2 Channel Strategy Pattern

The `DeliveryChannelStrategy` interface already exists with `WebappDeliveryChannelStrategy` (SSE) as the reference implementation. This workflow adds `EmailDeliveryChannelStrategy` as a second strategy bean. The `NotificationDispatchService` auto-discovers it via Spring injection — no code changes to the dispatch orchestrator's strategy resolution logic.

```
DeliveryChannelStrategy (interface)
├── WebappDeliveryChannelStrategy  ← existing (SSE)
├── EmailDeliveryChannelStrategy   ← this workflow
├── SmsDeliveryChannelStrategy     ← future
└── PushDeliveryChannelStrategy    ← future
```

### 1.3 Delivery Paths

**Managed delivery** — notification-linked email with full lifecycle tracking:

```
POST /email/notifications/{notificationId}/deliveries
  → EmailDeliveryController
    → EmailDeliveryManagementUseCase.createDelivery(notificationId, request)
      → EmailCreationUseCase.create(email, recipients, attachments)
      → NotificationDispatchService.dispatch(notification, DeliveryChannel.EMAIL)
        → EmailDeliveryChannelStrategy.deliver(notification, recipientEmail)
          → JavaMailSender.send(mimeMessage)
      → persist NotificationDeliveryDataModel (status=SENT|FAILED)
```

**Immediate send** — fire-and-forget email without notification linkage:

```
POST /email/send
  → ImmediateEmailController
    → ImmediateSendUseCase.send(request)
      → for each recipient:
        → build MimeMessage
        → JavaMailSender.send(message)
      → return aggregated results (per-recipient SUCCESS|FAILED)
```

**Template-driven** — rendered from stored templates before delivery:

```
POST /email/templates/{templateId}/preview
  → EmailTemplateController
    → EmailTemplatePreviewUseCase.preview(templateId, templateData)
      → EmailTemplateRenderingService.render(template, variables)
      → return rendered subject + body
```

### 1.4 Data Flow Diagram

```
                      ┌────────────────────────────────────┐
                      │          API Consumers              │
                      │  (Controllers, Schedulers, etc.)    │
                      └──────┬───────────┬─────────┬───────┘
                             │           │         │
                     managed │  immediate│  template│
                    delivery │    send   │   CRUD  │
                             │           │         │
                      ┌──────▼───┐ ┌─────▼──┐ ┌───▼──────────────┐
                      │ Email    │ │Immediat│ │ Email Template   │
                      │ Delivery │ │e Send  │ │ Controller       │
                      │ Ctrl     │ │ Ctrl   │ │                  │
                      └──────┬───┘ └────┬───┘ └───┬──────────────┘
                             │          │         │
                      ┌──────▼───┐ ┌────▼───┐ ┌───▼──────────────┐
                      │ Email    │ │Immediat│ │ Template Use     │
                      │ Delivery │ │e Send  │ │ Cases (CRUD +    │
                      │ Mgmt UC  │ │ UC     │ │ Preview)         │
                      └──────┬───┘ └────┬───┘ └───┬──────────────┘
                             │          │         │
                      ┌──────▼──────────▼───┐ ┌───▼──────────────┐
                      │ EmailDelivery       │ │ Template         │
                      │ ChannelStrategy     │ │ RenderingService │
                      │ → JavaMailSender    │ │ → {{var}} replace│
                      └──────┬──────────────┘ └──────────────────┘
                             │
                      ┌──────▼──────────────┐
                      │    JavaMailSender    │
                      │    (SMTP / AWS SES)  │
                      └─────────────────────┘
```

---

## 2. File Inventory

### 2.1 What Already Exists (no work needed)

| Component | Location | Status |
|-----------|----------|--------|
| `EmailDataModel` | `multi-tenant-data/.../notifications/email/` | Complete — emailId, subject, body, sender + recipients + attachments |
| `EmailRecipientDataModel` | same directory | Complete — composite key (tenantId, emailId, recipientEmail) |
| `EmailAttachmentDataModel` | same directory | Complete — composite key (tenantId, emailId, attachmentUrl) |
| `EmailRepository` | `notification-system/.../interfaceadapters/` | Complete — extends TenantScopedRepository |
| `EmailRecipientRepository` | same package | Complete |
| `EmailAttachmentRepository` | same package | Complete |
| `DeliveryChannel.EMAIL` | `multi-tenant-data/.../notifications/` | Enum value exists |
| `email-notification.yaml` | `notification-system/src/main/resources/openapi/` | 770-line spec — all endpoints defined |
| `DeliveryChannelStrategy` | `notification-system/.../usecases/` | Interface exists |
| `DeliveryResult` | same package | Record with `sent()` / `failed()` factory methods |
| `NotificationDispatchService` | same package | Dispatch orchestrator with `EnumMap<DeliveryChannel, Strategy>` |
| `NotificationDeliveryDataModel` | `multi-tenant-data/.../notifications/` | Delivery tracking entity exists |
| DB schema tables | `db_init/00-schema-qa.sql`, test schema | `emails`, `email_recipients`, `email_attachments` tables exist |

### 2.2 New Files (35)

| # | File | Package / Location | Phase | Responsibility |
|---|------|--------------------|-------|----------------|
| 1 | `EmailTemplateDataModel.java` | `multi-tenant-data/.../notifications/email/` | 2 | Email template entity — composite key, subject/body templates, variables relationship |
| 2 | `EmailTemplateVariableDataModel.java` | same | 2 | Template variable metadata — name, type, required flag, default value |
| 3 | `EmailConfiguration.java` | `notification-system/.../config/` | 4 | `JavaMailSender` bean with SMTP properties |
| 4 | `EmailDeliveryChannelStrategy.java` | `notification-system/.../usecases/` | 5 | EMAIL channel strategy — builds MimeMessage, sends via JavaMailSender |
| 5 | `EmailCreationUseCase.java` | same | 6 | Two-method pattern — creates email + recipients + attachments |
| 6 | `EmailDeliveryManagementUseCase.java` | same | 6 | CRUD for delivery records + retry |
| 7 | `ImmediateSendUseCase.java` | same | 6 | Fire-and-forget email send with per-recipient result aggregation |
| 8 | `EmailDeliveryConfig.java` | `notification-system/.../usecases/domain/` | 6 | Record for email delivery configuration (from, replyTo, priority, etc.) |
| 9 | `EmailTemplateRepository.java` | `notification-system/.../interfaceadapters/` | 6 | TenantScopedRepository for templates |
| 10 | `EmailTemplateVariableRepository.java` | same | 6 | TenantScopedRepository for template variables |
| 11 | `EmailTemplateCreationUseCase.java` | `notification-system/.../usecases/` | 7 | Two-method pattern — creates template + variables |
| 12 | `EmailTemplateUpdateUseCase.java` | same | 7 | Updates template fields and variable set |
| 13 | `GetEmailTemplateByIdUseCase.java` | same | 7 | Retrieves single template with variables |
| 14 | `ListEmailTemplatesUseCase.java` | same | 7 | Paginated list by tenant |
| 15 | `EmailTemplatePreviewUseCase.java` | same | 7 | Renders template with test data |
| 16 | `EmailTemplateRenderingService.java` | same | 7 | Simple `{{variable}}` placeholder replacement |
| 17 | `EmailDeliveryController.java` | `notification-system/.../interfaceadapters/` | 8 | Delivery CRUD + retry endpoints |
| 18 | `ImmediateEmailController.java` | same | 8 | Immediate send endpoint |
| 19 | `EmailTemplateController.java` | same | 8 | Template CRUD + preview endpoints |
| 20 | `EmailDeliveryChannelStrategyTest.java` | test package | 9 | Channel identity, delivery success/failure |
| 21 | `EmailCreationUseCaseTest.java` | test package | 9 | Creation, transform (recipients, attachments) |
| 22 | `EmailDeliveryManagementUseCaseTest.java` | test package | 9 | CRUD operations + retry |
| 23 | `ImmediateSendUseCaseTest.java` | test package | 9 | Send success, partial failure, all fail |
| 24 | `EmailTemplateCreationUseCaseTest.java` | test package | 9 | Creation + transform |
| 25 | `EmailTemplateUpdateUseCaseTest.java` | test package | 9 | Update happy, not found, variable changes |
| 26 | `GetEmailTemplateByIdUseCaseTest.java` | test package | 9 | Get found, not found |
| 27 | `ListEmailTemplatesUseCaseTest.java` | test package | 9 | List all, empty, paginated |
| 28 | `EmailTemplatePreviewUseCaseTest.java` | test package | 9 | Preview all vars, partial, missing required |
| 29 | `EmailTemplateRenderingServiceTest.java` | test package | 9 | Render simple, multiple vars, missing var |
| 30 | `EmailDeliveryControllerTest.java` | test package | 9 | All delivery endpoints (standalone MockMvc) |
| 31 | `ImmediateEmailControllerTest.java` | test package | 9 | Send endpoint |
| 32 | `EmailTemplateControllerTest.java` | test package | 9 | All template endpoints |
| 33 | `EmailConfigurationTest.java` | test package | 9 | Bean creation verification |
| 34 | `EmailComponentTest.java` | test package | 10 | Full Spring context — create, dispatch, verify, retry |
| 35 | `EmailTemplateComponentTest.java` | test package | 10 | Template CRUD lifecycle + preview rendering |

### 2.3 Modified Files (10)

| # | File | Change | Phase |
|---|------|--------|-------|
| 1 | `DeliveryStatus.java` | Add `OPENED`, `CLICKED`, `BOUNCED` enum values | 1 |
| 2 | `00-schema-qa.sql` (+ test schema) | Add `email_templates` and `email_template_variables` tables | 2 |
| 3 | `notification-system-module.yaml` | Expand email path `$ref` entries to individual endpoints | 3 |
| 4 | `notification-system/pom.xml` | Add `spring-boot-starter-mail` dependency | 4 |
| 5 | `application.properties` | Add SMTP configuration properties | 4 |
| 6 | `NotificationDispatchService.java` | Parameterize `dispatch()` to accept `DeliveryChannel` argument | 5 |
| 7 | `NotificationModelMapperConfiguration.java` | Add named TypeMaps for email and template DTOs | 8 |
| 8 | `NotificationControllerAdvice.java` | Expand `basePackageClasses` to include email controllers | 8 |
| 9 | `NotificationDispatchServiceTest.java` | Update tests for parameterized channel argument | 9 |
| 10 | `docs/MANIFEST.md` | Register workflow + prompt | post |

---

## 3. Implementation Sequence

Execute phases in strict order. Each phase ends with a compile/test verification gate. Do NOT proceed to the next phase until the current one passes.

```
Phase 1:  DeliveryStatus enum expansion (multi-tenant-data)
    ↓
Phase 2:  Template entities — EmailTemplateDataModel + EmailTemplateVariableDataModel (multi-tenant-data)
    ↓
Phase 3:  OpenAPI spec wiring — expand module.yaml path $refs + DTO generation (notification-system)
    ↓
Phase 4:  Email configuration — JavaMailSender bean + SMTP properties (notification-system)
    ↓
Phase 5:  EmailDeliveryChannelStrategy + dispatch parameterization (notification-system)
    ↓
Phase 6:  Email use cases — creation, delivery management, immediate send (notification-system)
    ↓
Phase 7:  Template use cases — CRUD, preview, rendering service (notification-system)
    ↓
Phase 8:  Controllers + ModelMapper config + controller advice (notification-system)
    ↓
Phase 9:  Unit tests — one per production class (notification-system)
    ↓
Phase 10: Component tests + E2E tests (notification-system + platform-api-e2e)
```

---

## 4. Phase Details

### Phase 1: DeliveryStatus Enum Expansion

**Modifies**: `multi-tenant-data/src/main/java/com/akademiaplus/notifications/DeliveryStatus.java`

Current values: `PENDING, SENT, DELIVERED, ACKNOWLEDGED, FAILED, EXPIRED`

Add 3 email-specific values (from `email-notification.yaml` `DeliveryStatus` schema):

| Value | Meaning |
|-------|---------|
| `OPENED` | Recipient opened the email (tracking pixel) |
| `CLICKED` | Recipient clicked a link in the email |
| `BOUNCED` | Email bounced (hard or soft bounce from provider) |

**Risk check**: `NotificationDispatchService` uses `EnumMap<DeliveryChannel>` not `DeliveryStatus`, and `DeliveryResult.sent()`/`failed()` only reference `SENT`/`FAILED`. No existing code uses exhaustive `switch` on `DeliveryStatus`. Adding values is safe.

**Verification gate**:
```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests -f platform-core-api/pom.xml
```

---

### Phase 2: Template Entities

**Creates** (in `multi-tenant-data/src/main/java/com/akademiaplus/notifications/email/`):

#### 2.1 EmailTemplateDataModel

```java
@Entity
@Table(name = "email_templates")
@SQLDelete(sql = "UPDATE email_templates SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND template_id = ?")
@IdClass(EmailTemplateDataModel.EmailTemplateCompositeId.class)
public class EmailTemplateDataModel extends TenantScoped {

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
}
```

- Composite key: `(tenantId, templateId)`
- Extends `TenantScoped`, `@Scope("prototype")`, `@Component`
- Standard Lombok annotations: `@Getter`, `@Setter`, `@AllArgsConstructor`, `@NoArgsConstructor`

#### 2.2 EmailTemplateVariableDataModel

```java
@Entity
@Table(name = "email_template_variables")
@SQLDelete(sql = "UPDATE email_template_variables SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND template_variable_id = ?")
@IdClass(EmailTemplateVariableDataModel.EmailTemplateVariableCompositeId.class)
public class EmailTemplateVariableDataModel extends TenantScoped {

    @Id
    @Column(name = "template_variable_id")
    private Long templateVariableId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "variable_type", nullable = false, length = 20)
    private String variableType;  // STRING, NUMBER, BOOLEAN, DATE, CURRENCY

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
}
```

- Composite key: `(tenantId, templateVariableId)`
- `@ManyToOne` back to `EmailTemplateDataModel` following the same `insertable=false, updatable=false` pattern as `EmailRecipientDataModel`

#### 2.3 DB Schema

Add to `db_init/00-schema-qa.sql` and test schema:

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

**Verification gate**:
```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests -f platform-core-api/pom.xml
```

---

### Phase 3: OpenAPI Spec Wiring + DTO Generation

**Modifies**: `notification-system/src/main/resources/openapi/notification-system-module.yaml`

The current module YAML uses a single `$ref` for `/email` pointing at the entire email spec's `paths` object. This must be expanded to enumerate individual path references so the OpenAPI generator produces proper API interfaces.

Replace the single `/email` entry with individual path entries:

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

Also add email-specific schema `$ref` entries under `components.schemas`:

```yaml
# Email schemas
CreateEmailDeliveryRequest:
  $ref: './email-notification.yaml#/components/schemas/CreateEmailDeliveryRequest'
EmailDeliveryResponse:
  $ref: './email-notification.yaml#/components/schemas/EmailDeliveryResponse'
ImmediateEmailNotificationRequest:
  $ref: './email-notification.yaml#/components/schemas/ImmediateEmailNotificationRequest'
ImmediateEmailDeliveryResponse:
  $ref: './email-notification.yaml#/components/schemas/ImmediateEmailDeliveryResponse'
CreateEmailTemplateRequest:
  $ref: './email-notification.yaml#/components/schemas/CreateEmailTemplateRequest'
EmailTemplateResponse:
  $ref: './email-notification.yaml#/components/schemas/EmailTemplateResponse'
EmailTemplateListResponse:
  $ref: './email-notification.yaml#/components/schemas/EmailTemplateListResponse'
EmailTemplatePreviewRequest:
  $ref: './email-notification.yaml#/components/schemas/EmailTemplatePreviewRequest'
EmailTemplatePreviewResponse:
  $ref: './email-notification.yaml#/components/schemas/EmailTemplatePreviewResponse'
UpdateEmailDeliveryStatusRequest:
  $ref: './email-notification.yaml#/components/schemas/UpdateEmailDeliveryStatusRequest'
UpdateEmailTemplateRequest:
  $ref: './email-notification.yaml#/components/schemas/UpdateEmailTemplateRequest'
EmailDeliveryDetailResponse:
  $ref: './email-notification.yaml#/components/schemas/EmailDeliveryDetailResponse'
EmailDeliveryListResponse:
  $ref: './email-notification.yaml#/components/schemas/EmailDeliveryListResponse'
```

**Verification gate**:
```bash
mvn clean generate-sources -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

Verify generated DTOs and API interfaces:
```bash
find notification-system/target/generated-sources -name "*Email*" -type f | sort
```

Expected: `CreateEmailDeliveryRequestDTO`, `EmailDeliveryResponseDTO`, `ImmediateEmailNotificationRequestDTO`, `ImmediateEmailDeliveryResponseDTO`, `CreateEmailTemplateRequestDTO`, `EmailTemplateResponseDTO`, etc. Note the exact generated API interface names (likely based on tags: `EmailDeliveryApi`, `ImmediateEmailApi`, `EmailTemplatesApi`).

---

### Phase 4: Email Configuration

**Creates**: `notification-system/src/main/java/com/akademiaplus/config/EmailConfiguration.java`

```java
@Configuration
public class EmailConfiguration {

    public static final int DEFAULT_SMTP_PORT = 587;

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
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        return mailSender;
    }
}
```

**Modifies**: `notification-system/pom.xml` — add dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

**Modifies**: `application/src/main/resources/application.properties` — add SMTP properties:

```properties
# Email SMTP Configuration (AWS SES via SMTP)
akademia.email.host=${EMAIL_SMTP_HOST:email-smtp.us-east-1.amazonaws.com}
akademia.email.port=${EMAIL_SMTP_PORT:587}
akademia.email.username=${EMAIL_SMTP_USERNAME:}
akademia.email.password=${EMAIL_SMTP_PASSWORD:}
akademia.email.from-address=${EMAIL_FROM_ADDRESS:noreply@akademiaplus.com}
akademia.email.from-name=${EMAIL_FROM_NAME:AkademiaPlus}
```

**Verification gate**:
```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

---

### Phase 5: EmailDeliveryChannelStrategy + Dispatch Parameterization

**Creates**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/EmailDeliveryChannelStrategy.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDeliveryChannelStrategy implements DeliveryChannelStrategy {

    public static final String ERROR_SEND_FAILED = "Failed to send email to %s: %s";
    public static final String ERROR_EMAIL_NOT_FOUND = "Email not found for notification: %s";

    private final JavaMailSender javaMailSender;
    private final EmailRepository emailRepository;

    @Value("${akademia.email.from-address}")
    private String fromAddress;

    @Value("${akademia.email.from-name}")
    private String fromName;

    @Override
    public DeliveryChannel getChannel() {
        return DeliveryChannel.EMAIL;
    }

    @Override
    public DeliveryResult deliver(NotificationDataModel notification, String recipientIdentifier) {
        // 1. Look up EmailDataModel by notification ID (via subject/metadata link)
        // 2. Build MimeMessage via javaMailSender.createMimeMessage()
        // 3. Set from (fromName + fromAddress), to (recipientIdentifier = email), subject, HTML body
        // 4. Send and return DeliveryResult.sent() or DeliveryResult.failed(reason)
    }
}
```

**Public API**:

| Method | Description |
|--------|-------------|
| `DeliveryChannel getChannel()` | Returns `DeliveryChannel.EMAIL` |
| `DeliveryResult deliver(NotificationDataModel, String)` | Builds MimeMessage, sends via JavaMailSender, returns result |

**Constants**:
- `public static final String ERROR_SEND_FAILED` — "Failed to send email to %s: %s"
- `public static final String ERROR_EMAIL_NOT_FOUND` — "Email not found for notification: %s"

**Pattern reference**: Mirror `WebappDeliveryChannelStrategy` — same class structure, same constants pattern, same `DeliveryResult` return semantics.

#### 5.1 Dispatch Parameterization

**Modifies**: `NotificationDispatchService.java`

The current `dispatch()` method hardcodes `DeliveryChannel.WEBAPP`. Add an overloaded method:

```java
// Existing method — backwards-compatible, defaults to WEBAPP
@Transactional
public NotificationDeliveryDataModel dispatch(NotificationDataModel notification) {
    return dispatch(notification, DeliveryChannel.WEBAPP);
}

// New method — accepts channel parameter
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

This preserves backward compatibility — all existing callers continue to work unchanged.

**Verification gate**:
```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

---

### Phase 6: Email Use Cases (Delivery Management + Immediate Send)

**Creates** (in `notification-system/src/main/java/com/akademiaplus/notification/usecases/`):

#### 6.1 EmailCreationUseCase

Two-method pattern (Hard Rule for creation use cases):

| Method | Description |
|--------|-------------|
| `EmailDataModel create(CreateEmailDeliveryRequestDTO dto)` | `@Transactional` — calls `transform()`, saves email, returns saved entity |
| `EmailDataModel transform(CreateEmailDeliveryRequestDTO dto)` | No TX — gets prototype bean, maps DTO fields, adds recipients + attachments |

#### 6.2 EmailDeliveryManagementUseCase

| Method | Description |
|--------|-------------|
| `NotificationDeliveryDataModel createDelivery(Long notificationId, CreateEmailDeliveryRequestDTO dto)` | Creates email via `EmailCreationUseCase`, dispatches via strategy, returns delivery record |
| `List<NotificationDeliveryDataModel> getDeliveriesByNotificationId(Long notificationId)` | Query deliveries for a notification |
| `NotificationDeliveryDataModel getDeliveryById(Long deliveryId)` | Single delivery lookup |
| `NotificationDeliveryDataModel updateDeliveryStatus(Long deliveryId, UpdateEmailDeliveryStatusRequestDTO dto)` | Updates status, timestamps (deliveredAt, openedAt, etc.) |
| `NotificationDeliveryDataModel retryDelivery(Long deliveryId)` | Re-dispatches a FAILED delivery, increments retryCount |

#### 6.3 ImmediateSendUseCase

| Method | Description |
|--------|-------------|
| `ImmediateEmailDeliveryResponseDTO send(ImmediateEmailNotificationRequestDTO dto)` | Iterates recipients, sends each via `EmailDeliveryChannelStrategy`, aggregates per-recipient results |

#### 6.4 EmailDeliveryConfig (domain record)

**Creates**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/domain/EmailDeliveryConfig.java`

```java
public record EmailDeliveryConfig(
    String fromEmail,
    String fromName,
    String replyTo,
    String priority,
    Long templateId,
    Map<String, Object> templateData,
    List<EmailAttachmentConfig> attachments,
    Map<String, String> customHeaders,
    boolean trackingEnabled
) {}
```

Placement in `usecases/domain/` per Hard Rule #13.

#### 6.5 Repositories

**Creates** (in `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/`):

- `EmailTemplateRepository.java` — extends `TenantScopedRepository<EmailTemplateDataModel, EmailTemplateDataModel.EmailTemplateCompositeId>`
- `EmailTemplateVariableRepository.java` — extends `TenantScopedRepository<EmailTemplateVariableDataModel, EmailTemplateVariableDataModel.EmailTemplateVariableCompositeId>`

**Verification gate**:
```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

---

### Phase 7: Template Use Cases

**Creates** (in `notification-system/src/main/java/com/akademiaplus/notification/usecases/`):

#### 7.1 EmailTemplateCreationUseCase

Two-method pattern:

| Method | Description |
|--------|-------------|
| `EmailTemplateDataModel create(CreateEmailTemplateRequestDTO dto)` | `@Transactional` — calls `transform()`, saves template |
| `EmailTemplateDataModel transform(CreateEmailTemplateRequestDTO dto)` | Gets prototype bean, maps fields, adds variables |

#### 7.2 EmailTemplateUpdateUseCase

| Method | Description |
|--------|-------------|
| `EmailTemplateDataModel update(Long templateId, UpdateEmailTemplateRequestDTO dto)` | `@Transactional` — finds template, updates fields, syncs variable set |

#### 7.3 GetEmailTemplateByIdUseCase

| Method | Description |
|--------|-------------|
| `EmailTemplateDataModel getById(Long templateId)` | Finds template or throws not-found exception |

#### 7.4 ListEmailTemplatesUseCase

| Method | Description |
|--------|-------------|
| `Page<EmailTemplateDataModel> list(String category, Pageable pageable)` | Paginated query, optional category filter |

#### 7.5 EmailTemplatePreviewUseCase

| Method | Description |
|--------|-------------|
| `EmailTemplatePreviewResponseDTO preview(Long templateId, EmailTemplatePreviewRequestDTO dto)` | Loads template, renders with `EmailTemplateRenderingService`, returns preview |

#### 7.6 EmailTemplateRenderingService

| Method | Description |
|--------|-------------|
| `String render(String template, Map<String, Object> variables)` | Replaces `{{variableName}}` placeholders with values |
| `String renderSubject(EmailTemplateDataModel template, Map<String, Object> variables)` | Renders subject line |
| `String renderBodyHtml(EmailTemplateDataModel template, Map<String, Object> variables)` | Renders HTML body |
| `String renderBodyText(EmailTemplateDataModel template, Map<String, Object> variables)` | Renders text body (if present) |

**Design decision**: Simple `{{var}}` regex replacement (`Pattern.compile("\\{\\{(\\w+)\\}\\}")`) over Thymeleaf. Sufficient for variable substitution; conditionals/loops deferred to v2.

**Constants**:
- `public static final String VARIABLE_PATTERN` — `"\\{\\{(\\w+)\\}\\}"`
- `public static final String ERROR_TEMPLATE_NOT_FOUND` — "Email template not found: %s"

**Verification gate**:
```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

---

### Phase 8: Controllers + ModelMapper Config + Controller Advice

**Creates** (in `notification-system/src/main/java/com/akademiaplus/notification/interfaceadapters/`):

#### 8.1 EmailDeliveryController

Implements generated API interface (from `Email Delivery` tag). `@RestController @RequestMapping("/v1/notification-system")`.

| Endpoint | Method | Delegates to |
|----------|--------|-------------|
| `POST /email/notifications/{notificationId}/deliveries` | `createEmailDelivery` | `EmailDeliveryManagementUseCase.createDelivery()` |
| `GET /email/notifications/{notificationId}/deliveries` | `getEmailDeliveries` | `EmailDeliveryManagementUseCase.getDeliveriesByNotificationId()` |
| `GET /email/deliveries/{deliveryId}` | `getEmailDeliveryById` | `EmailDeliveryManagementUseCase.getDeliveryById()` |
| `PATCH /email/deliveries/{deliveryId}` | `updateEmailDeliveryStatus` | `EmailDeliveryManagementUseCase.updateDeliveryStatus()` |
| `POST /email/deliveries/{deliveryId}/retry` | `retryEmailDelivery` | `EmailDeliveryManagementUseCase.retryDelivery()` |

#### 8.2 ImmediateEmailController

Implements generated API interface (from `Immediate Email` tag).

| Endpoint | Method | Delegates to |
|----------|--------|-------------|
| `POST /email/send` | `sendImmediateEmailNotification` | `ImmediateSendUseCase.send()` |

#### 8.3 EmailTemplateController

Implements generated API interface (from `Email Templates` tag).

| Endpoint | Method | Delegates to |
|----------|--------|-------------|
| `GET /email/templates` | `listEmailTemplates` | `ListEmailTemplatesUseCase.list()` |
| `POST /email/templates` | `createEmailTemplate` | `EmailTemplateCreationUseCase.create()` |
| `GET /email/templates/{templateId}` | `getEmailTemplateById` | `GetEmailTemplateByIdUseCase.getById()` |
| `PUT /email/templates/{templateId}` | `updateEmailTemplate` | `EmailTemplateUpdateUseCase.update()` |
| `POST /email/templates/{templateId}/preview` | `previewEmailTemplate` | `EmailTemplatePreviewUseCase.preview()` |

#### 8.4 NotificationModelMapperConfiguration

Add named TypeMaps for email and template DTOs. Follow the existing sandwich pattern:

```java
// Example for email delivery response
modelMapper.createTypeMap(NotificationDeliveryDataModel.class, EmailDeliveryResponseDTO.class, "EMAIL_DELIVERY_RESPONSE_MAP")
    .setImplicitMappingEnabled(false)
    .addMappings(mapper -> {
        mapper.skip(EmailDeliveryResponseDTO::setDeliveryId);
        // explicit field mappings
    })
    .setImplicitMappingEnabled(true);
```

#### 8.5 NotificationControllerAdvice

Expand `basePackageClasses` to include the new controllers:

```java
@ControllerAdvice(basePackageClasses = {
    NotificationController.class,
    EmailDeliveryController.class,
    ImmediateEmailController.class,
    EmailTemplateController.class
})
```

Or create a separate `EmailControllerAdvice` if the exception handling differs.

**Verification gate**:
```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

---

### Phase 9: Unit Tests

One test class per production class (Hard Rule #11). All tests follow: Given-When-Then, `shouldDoX_whenY()`, `@DisplayName`, `@Nested`, zero `any()` matchers.

| # | Test Class | Production Class | Key @Nested Groups |
|---|-----------|-----------------|-------------------|
| 1 | `EmailDeliveryChannelStrategyTest` | `EmailDeliveryChannelStrategy` | ChannelIdentity, Delivery (success, mail exception, email not found) |
| 2 | `EmailCreationUseCaseTest` | `EmailCreationUseCase` | Creation, Transform (with recipients, with attachments) |
| 3 | `EmailDeliveryManagementUseCaseTest` | `EmailDeliveryManagementUseCase` | CreateDelivery, GetDeliveries, GetById, UpdateStatus, RetryDelivery |
| 4 | `ImmediateSendUseCaseTest` | `ImmediateSendUseCase` | Send (success, partial failure, all fail), Validation |
| 5 | `EmailTemplateCreationUseCaseTest` | `EmailTemplateCreationUseCase` | Creation, Transform |
| 6 | `EmailTemplateUpdateUseCaseTest` | `EmailTemplateUpdateUseCase` | Update (happy path, not found, variable changes) |
| 7 | `GetEmailTemplateByIdUseCaseTest` | `GetEmailTemplateByIdUseCase` | Get (found, not found) |
| 8 | `ListEmailTemplatesUseCaseTest` | `ListEmailTemplatesUseCase` | List (all, empty, paginated) |
| 9 | `EmailTemplatePreviewUseCaseTest` | `EmailTemplatePreviewUseCase` | Preview (all vars, partial, missing required) |
| 10 | `EmailTemplateRenderingServiceTest` | `EmailTemplateRenderingService` | Render (simple, multiple vars, missing var, special characters) |
| 11 | `EmailDeliveryControllerTest` | `EmailDeliveryController` | All 5 endpoints (standalone MockMvc) |
| 12 | `ImmediateEmailControllerTest` | `ImmediateEmailController` | Send endpoint (success, validation error) |
| 13 | `EmailTemplateControllerTest` | `EmailTemplateController` | All 5 template endpoints (standalone MockMvc) |
| 14 | `EmailConfigurationTest` | `EmailConfiguration` | Bean creation with valid properties |

**Also modifies**: `NotificationDispatchServiceTest.java` — update existing tests for the new parameterized `dispatch(notification, channel)` overload. Existing tests calling `dispatch(notification)` should still pass (backward-compatible default).

**Test plan example** — `EmailDeliveryChannelStrategyTest`:

| @Nested | Test Method | Verifies |
|---------|-------------|----------|
| ChannelIdentity | `shouldReturnEmailChannel_whenAskedForChannel` | `getChannel() == DeliveryChannel.EMAIL` |
| Delivery | `shouldReturnSent_whenEmailSendsSuccessfully` | `DeliveryResult.status() == SENT` |
| Delivery | `shouldReturnFailed_whenMailExceptionOccurs` | `DeliveryResult.status() == FAILED`, message contains error |
| Delivery | `shouldReturnFailed_whenEmailNotFoundForNotification` | `DeliveryResult.status() == FAILED`, message uses `ERROR_EMAIL_NOT_FOUND` |
| Delivery | `shouldBuildMimeMessage_whenDeliveringEmail` | Verify `javaMailSender.createMimeMessage()` called |

**Verification gate**:
```bash
mvn test -pl notification-system -am -f platform-core-api/pom.xml
```

---

### Phase 10: Component Tests + E2E Tests

#### 10.1 EmailComponentTest

Full Spring context + Testcontainers MariaDB. Mock `JavaMailSender` to avoid real email sends (or use GreenMail embedded SMTP).

| @Nested | Tests |
|---------|-------|
| `EmailDeliveryLifecycle` | Create notification → create email delivery → verify delivery record persisted with SENT status |
| `ImmediateSend` | POST to `/email/send` → verify per-recipient results |
| `DeliveryStatusUpdate` | Create delivery → PATCH status to DELIVERED → verify updated timestamps |
| `RetryDelivery` | Create failed delivery → POST retry → verify retryCount incremented |
| `DeliveryListing` | Create multiple deliveries → GET list → verify pagination |

#### 10.2 EmailTemplateComponentTest

| @Nested | Tests |
|---------|-------|
| `TemplateLifecycle` | Create template → get by ID → update → list → verify all operations |
| `TemplatePreview` | Create template with variables → POST preview with data → verify rendered output |
| `TemplateWithVariables` | Create template with 3 variables → verify variable persistence → update variables |

#### 10.3 E2E Tests

Add requests to `platform-api-e2e/Postman Collections/platform-api-e2e.json`:

- Template CRUD: create template, get template, update template, list templates
- Template preview: preview with full data
- Delivery tracking: create delivery, get delivery, update status
- Immediate send: conditional on SMTP availability (skip in CI without SMTP)

**Note**: Component tests should use `@MockitoBean JavaMailSender` to prevent real sends. The mocked sender's `createMimeMessage()` should return a real `MimeMessage` (from `Session.getDefaultInstance(new Properties())`).

**Verification gate**:
```bash
mvn verify -pl notification-system -am -f platform-core-api/pom.xml
```

---

## 5. Key Design Decisions

### 5.1 Email Provider: Jakarta Mail over AWS SES SDK

| Factor | Jakarta Mail (JavaMailSender) | AWS SES SDK |
|--------|------------------------------|-------------|
| Protocol | Standard SMTP | AWS API (HTTP) |
| Vendor lock-in | None — any SMTP server | AWS-specific |
| Configuration | SMTP host/port/credentials | AWS region + access keys |
| Local testing | GreenMail / Mailtrap | LocalStack or mocks |
| Complexity | Low — Spring Boot auto-config | Medium — SDK dependency |

**Decision**: Jakarta Mail. Standard SMTP works with AWS SES (which exposes an SMTP endpoint), Mailtrap for dev, GreenMail for tests. No vendor SDK dependency.

### 5.2 Template Engine: `{{var}}` Replacement over Thymeleaf

| Factor | `{{var}}` Regex | Thymeleaf |
|--------|-----------------|-----------|
| Dependencies | None (java.util.regex) | `spring-boot-starter-thymeleaf` |
| Features | Variable substitution only | Conditionals, loops, layouts |
| Complexity | ~20 lines of code | Template engine configuration |
| Upgrade path | Replace service impl, keep interface | N/A (already full-featured) |

**Decision**: Simple `{{var}}` replacement for v1. Sufficient for personalized emails (greeting, names, dates, amounts). Thymeleaf upgrade path documented in Future Extensibility.

### 5.3 Controller Split: 3 Controllers

| Factor | Single Controller | 3 Controllers |
|--------|------------------|---------------|
| Responsibility | Mixed — delivery + send + templates | One per OpenAPI tag |
| Generated API | May be 1 or 3 interfaces | Maps 1:1 to generated interfaces |
| Testability | Large test class | Focused test classes |

**Decision**: 3 controllers matching the 3 OpenAPI tags (`Email Delivery`, `Immediate Email`, `Email Templates`). Each implements its generated API interface.

### 5.4 DeliveryStatus: Extend Existing Enum

| Factor | Extend `DeliveryStatus` | New `EmailDeliveryStatus` |
|--------|------------------------|---------------------------|
| Type system | Unified — one enum for all channels | Parallel enums |
| `NotificationDeliveryDataModel` | Uses existing `DeliveryStatus` field | Would need new field or type union |
| Existing code impact | Minimal (no exhaustive switches) | Schema change required |

**Decision**: Extend `DeliveryStatus` with `OPENED`, `CLICKED`, `BOUNCED`. These values are email-specific but the unified type system avoids schema complexity. Non-email channels simply never transition to these states.

### 5.5 Dispatch Parameterization: Overloaded Method

| Factor | Replace dispatch() | Overload dispatch() |
|--------|-------------------|---------------------|
| Backward compatibility | Breaking change | Fully compatible |
| Caller impact | All callers must update | No existing callers affected |
| API clarity | Single method signature | Two methods, default channel |

**Decision**: Add overloaded `dispatch(notification, channel)` alongside existing `dispatch(notification)` which defaults to WEBAPP. Zero impact on existing code.

---

## 6. Multi-Tenancy Considerations

### 6.1 Template Entity Isolation

`EmailTemplateDataModel` and `EmailTemplateVariableDataModel` follow the standard composite key pattern (`tenantId` + entityId). The Hibernate tenant filter ensures templates are scoped to the requesting tenant. Templates from tenant A are invisible to tenant B.

### 6.2 Email Delivery Records

`NotificationDeliveryDataModel` already supports multi-tenancy via its composite key. Email deliveries use the same entity, with `channel=EMAIL` distinguishing them from WEBAPP deliveries.

### 6.3 Cross-Tenant Concerns

The email delivery system operates within standard request-scoped tenant context. Unlike the SSE scheduler (which runs cross-tenant), email operations are always triggered by authenticated API requests with a tenant context already established.

### 6.4 Entity ID Assignment

All new entities (`EmailTemplateDataModel`, `EmailTemplateVariableDataModel`) follow the standard pattern:
- IDs assigned by `EntityIdAssigner` via Hibernate `PreInsertEvent`
- Created via `applicationContext.getBean()` (prototype scope)
- Never set `tenantId` or entity IDs manually

---

## 7. Future Extensibility

### 7.1 Thymeleaf Template Engine

Replace `EmailTemplateRenderingService` implementation with Thymeleaf for conditionals, loops, and layouts:

```java
@Service
public class ThymeleafEmailTemplateRenderingService implements EmailTemplateRenderingService {
    private final SpringTemplateEngine templateEngine;
    // renders using Thymeleaf context
}
```

Requires: `spring-boot-starter-thymeleaf` dependency. The service interface stays the same — only the implementation changes.

### 7.2 Email Tracking Webhooks

AWS SES (and other providers) can send delivery/bounce/complaint notifications via SNS→HTTP webhooks. Add:

1. `EmailWebhookController` — receives provider callbacks
2. Maps provider event types to `DeliveryStatus` transitions (DELIVERED, BOUNCED, OPENED, CLICKED)
3. Calls `EmailDeliveryManagementUseCase.updateDeliveryStatus()`

### 7.3 Attachment Storage

Current spec defines attachment URLs. Future enhancement: integrate with S3 or object storage for file uploads, generate pre-signed URLs, attach to emails as inline or file attachments via `MimeMessageHelper.addAttachment()`.

### 7.4 Email Queue with Retry

For high-volume sending, add an async queue (Redis or SQS):

1. `ImmediateSendUseCase` publishes to queue instead of sending synchronously
2. `EmailQueueConsumer` processes queue entries with exponential backoff
3. Dead-letter queue for permanently failed emails

### 7.5 Rate Limiting

AWS SES enforces sending rate limits. Add rate limiting via:
- `@RateLimiter` annotation (Resilience4j)
- Or token bucket algorithm in `EmailDeliveryChannelStrategy`

---

## 8. Verification Checklist

Run after all phases complete:

```bash
# 1. Full module compile
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml

# 2. All unit tests pass
mvn test -pl notification-system -am -f platform-core-api/pom.xml

# 3. Component tests pass
mvn verify -pl notification-system -am -f platform-core-api/pom.xml

# 4. Full project build (no cross-module breakage)
mvn clean install -DskipTests -f platform-core-api/pom.xml

# 5. Convention compliance
grep -rn "any()" notification-system/src/test/ | grep -v "import" | wc -l  # must be 0
grep -rn "// Arrange" notification-system/src/test/ | wc -l               # must be 0
grep -rn "catch (Exception " notification-system/src/main/ | wc -l       # must be 0

# 6. Copyright headers on all new files
for f in $(git diff --name-only --diff-filter=A HEAD); do
  head -1 "$f" | grep -q "Copyright" || echo "MISSING: $f"
done
```

---

## 9. Critical Reminders

1. **Prototype beans**: ALL entity instantiation via `applicationContext.getBean()` — never `new EmailTemplateDataModel()`
2. **ID assignment**: Never set `templateId`, `templateVariableId`, or `tenantId` manually — `EntityIdAssigner` handles all
3. **Named TypeMaps**: All DTO↔entity mappings use named TypeMaps with skip rules, `setImplicitMappingEnabled(false/true)` sandwich
4. **Constants**: ALL string literals → `public static final`, shared between impl and tests
5. **Testing**: Given-When-Then, `shouldDoX_whenY()`, ZERO `any()` matchers, `@DisplayName` on all tests
6. **Copyright header**: Required on ALL new `.java` files (ElatusDev 2025)
7. **Commits**: Conventional Commits (`feat(notification-system): ...`), NO `Co-Authored-By` or AI attribution
8. **Long IDs**: Always `Long`, never `Integer`
9. **`@Transactional`**: Only on methods that write to the database (`create()`, `update()`, `retryDelivery()`)
10. **Entity pattern**: Follow `EmailDataModel` for composite keys, `@SQLDelete`, `@IdClass`, `insertable=false/updatable=false` on `@JoinColumn`
11. **Domain records**: `EmailDeliveryConfig` goes in `usecases/domain/` (Hard Rule #13)
12. **Strategy placement**: `EmailDeliveryChannelStrategy` lives alongside `DeliveryChannelStrategy` interface in `usecases/` (Hard Rule #12)
