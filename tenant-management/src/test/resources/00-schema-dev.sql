USE multi_tenant_db;

--      TENANT MODULE       --

CREATE TABLE tenants (
    tenant_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(200),
    website_url VARCHAR(255),
    email VARCHAR(200) NOT NULL,
    address VARCHAR(200) NOT NULL,
    phone VARCHAR(20),
    landline VARCHAR(20),
    description TEXT,
    tax_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    INDEX idx_org_name (organization_name, deleted_at)
);

CREATE TABLE tenant_subscriptions (
    tenant_id BIGINT NOT NULL,
    tenant_subscription_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    max_users INT DEFAULT NULL,
    billing_date DATE NOT NULL,
    rate_per_student DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, tenant_subscription_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    INDEX idx_tenant_subscription_type (tenant_id, type, deleted_at),
    INDEX idx_subscription_billing_date (billing_date, deleted_at),
    INDEX idx_tenant_subscription_active (tenant_id, deleted_at)
);

CREATE TABLE tenant_billing_cycles (
    tenant_id BIGINT NOT NULL,
    tenant_billing_cycle_id BIGINT NOT NULL,
    billing_month DATE NOT NULL,
    calculation_date DATE NOT NULL,
    user_count INT NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    billing_status VARCHAR(20) DEFAULT 'PENDING',
    billed_at TIMESTAMP NULL,
    paid_at TIMESTAMP NULL,
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, tenant_billing_cycle_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    UNIQUE KEY uk_tenant_billing_month (tenant_id, billing_month, deleted_at),
    INDEX idx_tenant_billing_status (tenant_id, billing_status, deleted_at),
    INDEX idx_billing_calculation_date (calculation_date, billing_status),
    INDEX idx_billing_month_status (billing_month, billing_status, deleted_at)
);

CREATE TABLE tenant_sequences (
    tenant_id BIGINT NOT NULL,
    entity_name VARCHAR(50) NOT NULL,
    next_value BIGINT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, entity_name),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);

CREATE TABLE tenant_branding (
    tenant_id BIGINT NOT NULL,
    school_name VARCHAR(200) NOT NULL,
    logo_url VARCHAR(500),
    primary_color VARCHAR(7) NOT NULL,
    secondary_color VARCHAR(7) NOT NULL,
    font_family VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);

--      NOTIFICATIONS MODULE     --

CREATE TABLE notifications (
    tenant_id BIGINT NOT NULL,
    notification_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    scheduled_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    target_user_id BIGINT NULL,
    metadata TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, notification_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    INDEX idx_tenant_active_notification (tenant_id, deleted_at),
    INDEX idx_tenant_notification_type (tenant_id, notification_type, deleted_at),
    INDEX idx_tenant_notification_priority (tenant_id, priority, deleted_at),
    INDEX idx_tenant_notification_scheduled (tenant_id, scheduled_at, deleted_at),
    INDEX idx_tenant_notification_target_user (tenant_id, target_user_id, deleted_at),
    INDEX idx_tenant_notification_created (tenant_id, created_at, deleted_at)
);

CREATE TABLE notification_deliveries (
    tenant_id BIGINT NOT NULL,
    notification_delivery_id BIGINT NOT NULL,
    notification_id BIGINT NOT NULL,
    channel VARCHAR(30) NOT NULL,
    recipient_identifier VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    acknowledged_at TIMESTAMP NULL,
    failure_reason VARCHAR(500) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    external_id VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, notification_delivery_id),
    FOREIGN KEY (tenant_id, notification_id) REFERENCES notifications(tenant_id, notification_id),
    INDEX idx_tenant_delivery_notification (tenant_id, notification_id, deleted_at),
    INDEX idx_tenant_delivery_channel (tenant_id, channel, deleted_at),
    INDEX idx_tenant_delivery_status (tenant_id, status, deleted_at),
    INDEX idx_tenant_delivery_recipient (tenant_id, recipient_identifier(255), deleted_at),
    INDEX idx_tenant_delivery_external (tenant_id, external_id, deleted_at),
    INDEX idx_tenant_delivery_sent (tenant_id, sent_at, deleted_at),
    INDEX idx_tenant_delivery_retry (tenant_id, retry_count, status, deleted_at)
);

CREATE TABLE emails (
    tenant_id BIGINT NOT NULL,
    email_id BIGINT NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    sender VARCHAR(150) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, email_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    INDEX idx_tenant_active_email (tenant_id, deleted_at),
    INDEX idx_tenant_active_sender (tenant_id, deleted_at, sender)
);

CREATE TABLE email_recipients (
    tenant_id BIGINT NOT NULL,
    email_id BIGINT NOT NULL,
    recipient_email VARCHAR(150) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, email_id, recipient_email),
    FOREIGN KEY (tenant_id, email_id) REFERENCES emails(tenant_id, email_id),
    INDEX idx_tenant_active_email_recipients (tenant_id, deleted_at)
);

CREATE TABLE email_attachments (
    tenant_id BIGINT NOT NULL,
    email_id BIGINT NOT NULL,
    attachment_url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, email_id, attachment_url(255)),
    FOREIGN KEY (tenant_id, email_id) REFERENCES emails(tenant_id, email_id),
    INDEX idx_tenant_active_email_attachments (tenant_id, deleted_at)
);

--           POS SYSTEM MODULE         --

CREATE TABLE store_products (
    tenant_id BIGINT NOT NULL,
    store_product_id BIGINT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(10,2) NOT NULL,
    stock_quantity INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, store_product_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    UNIQUE KEY uk_store_product_name_tenant (tenant_id, product_name, deleted_at),
    INDEX idx_tenant_active_product (tenant_id, deleted_at),
    INDEX idx_tenant_active_price (tenant_id, deleted_at, price)
);

CREATE TABLE store_transactions (
    tenant_id BIGINT NOT NULL,
    store_transaction_id BIGINT NOT NULL,
    transaction_datetime DATETIME NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    employee_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, store_transaction_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    -- FK to employees defined via ALTER TABLE after employees table is created
    INDEX idx_tenant_active_store_transaction (tenant_id, deleted_at),
    INDEX idx_tenant_active_transaction_date (tenant_id, deleted_at, transaction_datetime)
);

CREATE TABLE store_sale_items (
    tenant_id BIGINT NOT NULL,
    store_sale_item_id BIGINT NOT NULL,
    store_transaction_id BIGINT NOT NULL,
    store_product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price_at_sale DECIMAL(10,2) NOT NULL,
    item_total DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, store_sale_item_id),
    FOREIGN KEY (tenant_id, store_transaction_id) REFERENCES store_transactions(tenant_id, store_transaction_id),
    FOREIGN KEY (tenant_id, store_product_id) REFERENCES store_products(tenant_id, store_product_id),
    INDEX idx_tenant_active_sale_item (tenant_id, deleted_at),
    INDEX idx_tenant_active_transaction_items (tenant_id, store_transaction_id, deleted_at)
);

--          SECURITY MODULE         --

CREATE TABLE customer_auths (
    tenant_id BIGINT NOT NULL,
    customer_auth_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    token TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, customer_auth_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    INDEX idx_tenant_active_customer_auth (tenant_id, deleted_at)
);

CREATE TABLE internal_auths (
    tenant_id BIGINT NOT NULL,
    internal_auth_id BIGINT NOT NULL,
    encrypted_username VARCHAR(500) NOT NULL,
    encrypted_password VARCHAR(500) NOT NULL,
    encrypted_role VARCHAR(500) NOT NULL,
    username_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, internal_auth_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    UNIQUE KEY uk_internal_auth_username_tenant (tenant_id, username_hash, deleted_at),
    INDEX idx_tenant_active_internal_auth (tenant_id, deleted_at)
);

--          USER MANAGEMENT MODULE          --

CREATE TABLE person_piis (
    tenant_id BIGINT NOT NULL,
    person_pii_id BIGINT NOT NULL,
    encrypted_first_name VARCHAR(500) NOT NULL,
    encrypted_last_name VARCHAR(500) NOT NULL,
    encrypted_phone_number VARCHAR(500) NOT NULL,
    encrypted_email VARCHAR(500) NOT NULL,
    encrypted_address VARCHAR(500) NOT NULL,
    encrypted_zip_code VARCHAR(500) NOT NULL,
    phone_number_hash VARCHAR(64) NOT NULL,
    email_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, person_pii_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    UNIQUE KEY uk_person_pii_phone_tenant (tenant_id, phone_number_hash, deleted_at),
    UNIQUE KEY uk_person_pii_email_tenant (tenant_id, email_hash, deleted_at),
    INDEX idx_tenant_active_person_pii (tenant_id, deleted_at)
);

CREATE TABLE employees (
    tenant_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    employee_type VARCHAR(50) NOT NULL,
    birthdate DATE NOT NULL,
    entry_date DATE NOT NULL,
    internal_auth_id BIGINT NOT NULL,
    person_pii_id BIGINT NOT NULL,
    encrypted_profile_picture MEDIUMBLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, employee_id),
    UNIQUE KEY uk_employee_internal_auth_tenant (tenant_id, internal_auth_id, deleted_at),
    UNIQUE KEY uk_employee_person_pii_tenant (tenant_id, person_pii_id, deleted_at),
    FOREIGN KEY (tenant_id, person_pii_id) REFERENCES person_piis(tenant_id, person_pii_id),
    FOREIGN KEY (tenant_id, internal_auth_id) REFERENCES internal_auths(tenant_id, internal_auth_id),
    INDEX idx_tenant_active_employee (tenant_id, deleted_at),
    INDEX idx_tenant_active_employee_type (tenant_id, deleted_at, employee_type),
    INDEX idx_tenant_employee_anniversary (tenant_id, entry_date, deleted_at)
);

-- Deferred FK: store_transactions → employees
ALTER TABLE store_transactions
ADD CONSTRAINT fk_store_transaction_employee
FOREIGN KEY (tenant_id, employee_id) REFERENCES employees(tenant_id, employee_id);

CREATE TABLE collaborators (
    tenant_id BIGINT NOT NULL,
    collaborator_id BIGINT NOT NULL,
    internal_auth_id BIGINT NOT NULL,
    encrypted_profile_picture MEDIUMBLOB,
    skills VARCHAR(100) NOT NULL,
    birthdate DATE NOT NULL,
    entry_date DATE NOT NULL,
    person_pii_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, collaborator_id),
    UNIQUE KEY uk_collaborator_internal_auth_tenant (tenant_id, internal_auth_id, deleted_at),
    UNIQUE KEY uk_collaborator_person_pii_tenant (tenant_id, person_pii_id, deleted_at),
    FOREIGN KEY (tenant_id, person_pii_id) REFERENCES person_piis(tenant_id, person_pii_id),
    FOREIGN KEY (tenant_id, internal_auth_id) REFERENCES internal_auths(tenant_id, internal_auth_id),
    INDEX idx_tenant_active_collaborator (tenant_id, deleted_at),
    INDEX idx_tenant_active_collaborator_skills (tenant_id, deleted_at, skills),
    INDEX idx_tenant_collaborator_anniversary (tenant_id, entry_date, deleted_at)
);

CREATE TABLE adult_students (
    tenant_id BIGINT NOT NULL,
    adult_student_id BIGINT NOT NULL,
    customer_auth_id BIGINT NOT NULL,
    birthdate DATE NOT NULL,
    entry_date DATE NOT NULL,
    encrypted_profile_picture MEDIUMBLOB,
    person_pii_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, adult_student_id),
    UNIQUE KEY uk_adult_student_customer_auth_tenant (tenant_id, customer_auth_id, deleted_at),
    UNIQUE KEY uk_adult_student_person_pii_tenant (tenant_id, person_pii_id, deleted_at),
    FOREIGN KEY (tenant_id, person_pii_id) REFERENCES person_piis(tenant_id, person_pii_id),
    FOREIGN KEY (tenant_id, customer_auth_id) REFERENCES customer_auths(tenant_id, customer_auth_id),
    INDEX idx_tenant_active_adult_student (tenant_id, deleted_at)
);

CREATE TABLE tutors (
    tenant_id BIGINT NOT NULL,
    tutor_id BIGINT NOT NULL,
    birthdate DATE NOT NULL,
    entry_date DATE NOT NULL,
    encrypted_profile_picture MEDIUMBLOB,
    customer_auth_id BIGINT,
    person_pii_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, tutor_id),
    UNIQUE KEY uk_tutor_person_pii_tenant (tenant_id, person_pii_id, deleted_at),
    UNIQUE KEY uk_tutor_customer_auth_tenant (tenant_id, customer_auth_id, deleted_at),
    FOREIGN KEY (tenant_id, person_pii_id) REFERENCES person_piis(tenant_id, person_pii_id),
    FOREIGN KEY (tenant_id, customer_auth_id) REFERENCES customer_auths(tenant_id, customer_auth_id),
    INDEX idx_tenant_active_tutor (tenant_id, deleted_at)
);

CREATE TABLE minor_students (
    tenant_id BIGINT NOT NULL,
    minor_student_id BIGINT NOT NULL,
    birthdate DATE NOT NULL,
    entry_date DATE NOT NULL,
    encrypted_profile_picture MEDIUMBLOB,
    customer_auth_id BIGINT NOT NULL,
    tutor_id BIGINT NOT NULL,
    person_pii_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, minor_student_id),
    UNIQUE KEY uk_minor_student_customer_auth_tenant (tenant_id, customer_auth_id, deleted_at),
    UNIQUE KEY uk_minor_student_person_pii_tenant (tenant_id, person_pii_id, deleted_at),
    FOREIGN KEY (tenant_id, person_pii_id) REFERENCES person_piis(tenant_id, person_pii_id),
    FOREIGN KEY (tenant_id, customer_auth_id) REFERENCES customer_auths(tenant_id, customer_auth_id),
    FOREIGN KEY (tenant_id, tutor_id) REFERENCES tutors(tenant_id, tutor_id),
    INDEX idx_tenant_active_minor_student (tenant_id, deleted_at)
);

--      COURSE MANAGEMENT MODULE        --

CREATE TABLE courses (
    tenant_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    course_name VARCHAR(100) NOT NULL,
    course_description VARCHAR(500) NOT NULL,
    max_capacity INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, course_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    INDEX idx_tenant_active_course (tenant_id, deleted_at),
    INDEX idx_tenant_active_course_name (tenant_id, deleted_at, course_name)
);

CREATE TABLE schedules (
    tenant_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    schedule_day VARCHAR(9) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    course_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, schedule_id),
    FOREIGN KEY (tenant_id, course_id) REFERENCES courses(tenant_id, course_id),
    INDEX idx_tenant_active_schedule (tenant_id, deleted_at),
    INDEX idx_tenant_active_course_schedule (tenant_id, course_id, deleted_at)
);

CREATE TABLE course_available_collaborators (
    tenant_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    collaborator_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, course_id, collaborator_id),
    FOREIGN KEY (tenant_id, course_id) REFERENCES courses(tenant_id, course_id),
    FOREIGN KEY (tenant_id, collaborator_id) REFERENCES collaborators(tenant_id, collaborator_id),
    INDEX idx_tenant_active_course_collaborators (tenant_id, deleted_at)
);

CREATE TABLE adult_student_courses (
    tenant_id BIGINT NOT NULL,
    adult_student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, adult_student_id, course_id),
    FOREIGN KEY (tenant_id, adult_student_id) REFERENCES adult_students(tenant_id, adult_student_id),
    FOREIGN KEY (tenant_id, course_id) REFERENCES courses(tenant_id, course_id),
    INDEX idx_tenant_active_adult_student_course (tenant_id, deleted_at)
);

CREATE TABLE minor_student_courses (
    tenant_id BIGINT NOT NULL,
    minor_student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, minor_student_id, course_id),
    FOREIGN KEY (tenant_id, minor_student_id) REFERENCES minor_students(tenant_id, minor_student_id),
    FOREIGN KEY (tenant_id, course_id) REFERENCES courses(tenant_id, course_id),
    INDEX idx_tenant_active_minor_student_course (tenant_id, deleted_at)
);

CREATE TABLE course_events (
    tenant_id BIGINT NOT NULL,
    course_event_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    collaborator_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    event_date DATE NOT NULL,
    event_title VARCHAR(100) NOT NULL,
    event_description VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, course_event_id),
    FOREIGN KEY (tenant_id, course_id) REFERENCES courses(tenant_id, course_id),
    FOREIGN KEY (tenant_id, collaborator_id) REFERENCES collaborators(tenant_id, collaborator_id),
    FOREIGN KEY (tenant_id, schedule_id) REFERENCES schedules(tenant_id, schedule_id),
    INDEX idx_tenant_active_course_event (tenant_id, deleted_at),
    INDEX idx_tenant_active_event_date (tenant_id, deleted_at, event_date)
);

CREATE TABLE course_event_adult_student_attendees (
    tenant_id BIGINT NOT NULL,
    course_event_id BIGINT NOT NULL,
    adult_student_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, course_event_id, adult_student_id),
    FOREIGN KEY (tenant_id, course_event_id) REFERENCES course_events(tenant_id, course_event_id),
    FOREIGN KEY (tenant_id, adult_student_id) REFERENCES adult_students(tenant_id, adult_student_id),
    INDEX idx_tenant_active_course_event_adult_attendees (tenant_id, deleted_at)
);

CREATE TABLE course_event_minor_student_attendees (
    tenant_id BIGINT NOT NULL,
    course_event_id BIGINT NOT NULL,
    minor_student_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, course_event_id, minor_student_id),
    FOREIGN KEY (tenant_id, course_event_id) REFERENCES course_events(tenant_id, course_event_id),
    FOREIGN KEY (tenant_id, minor_student_id) REFERENCES minor_students(tenant_id, minor_student_id),
    INDEX idx_tenant_active_course_event_minor_attendees (tenant_id, deleted_at)
);

---     BILLING MODULE      ---

CREATE TABLE memberships (
    tenant_id BIGINT NOT NULL,
    membership_id BIGINT NOT NULL,
    membership_type VARCHAR(50) NOT NULL,
    fee DECIMAL(10,2) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, membership_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    INDEX idx_tenant_active_membership (tenant_id, deleted_at),
    INDEX idx_tenant_active_membership_type (tenant_id, deleted_at, membership_type)
);

CREATE TABLE membership_adult_students (
    tenant_id BIGINT NOT NULL,
    membership_adult_student_id BIGINT NOT NULL,
    membership_id BIGINT NOT NULL,
    adult_student_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    due_date DATE NOT NULL,
    course_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, membership_adult_student_id),
    FOREIGN KEY (tenant_id, membership_id) REFERENCES memberships(tenant_id, membership_id),
    FOREIGN KEY (tenant_id, course_id) REFERENCES courses(tenant_id, course_id),
    FOREIGN KEY (tenant_id, adult_student_id) REFERENCES adult_students(tenant_id, adult_student_id),
    INDEX idx_tenant_active_membership_adult_student (tenant_id, deleted_at),
    INDEX idx_tenant_active_membership_dates (tenant_id, deleted_at, due_date)
);

CREATE TABLE membership_tutors (
    tenant_id BIGINT NOT NULL,
    membership_tutor_id BIGINT NOT NULL,
    membership_id BIGINT NOT NULL,
    tutor_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    due_date DATE NOT NULL,
    course_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, membership_tutor_id),
    FOREIGN KEY (tenant_id, membership_id) REFERENCES memberships(tenant_id, membership_id),
    FOREIGN KEY (tenant_id, course_id) REFERENCES courses(tenant_id, course_id),
    FOREIGN KEY (tenant_id, tutor_id) REFERENCES tutors(tenant_id, tutor_id),
    INDEX idx_tenant_active_membership_tutor (tenant_id, deleted_at),
    INDEX idx_tenant_active_tutor_membership_dates (tenant_id, deleted_at, due_date)
);

CREATE TABLE card_payment_infos (
    tenant_id BIGINT NOT NULL,
    card_payment_info_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    token TEXT NOT NULL,
    card_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, card_payment_info_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    INDEX idx_tenant_active_card_payment (tenant_id, deleted_at)
);

CREATE TABLE payment_adult_students (
    tenant_id BIGINT NOT NULL,
    payment_adult_student_id BIGINT NOT NULL,
    payment_date DATE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    membership_adult_student_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, payment_adult_student_id),
    FOREIGN KEY (tenant_id, membership_adult_student_id) REFERENCES membership_adult_students(tenant_id, membership_adult_student_id),
    INDEX idx_tenant_active_payment_adult_student (tenant_id, deleted_at),
    INDEX idx_tenant_active_payment_date (tenant_id, deleted_at, payment_date)
);

CREATE TABLE payment_tutors (
    tenant_id BIGINT NOT NULL,
    payment_tutor_id BIGINT NOT NULL,
    payment_date DATE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    membership_tutor_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, payment_tutor_id),
    FOREIGN KEY (tenant_id, membership_tutor_id) REFERENCES membership_tutors(tenant_id, membership_tutor_id),
    INDEX idx_tenant_active_payment_tutor (tenant_id, deleted_at),
    INDEX idx_tenant_active_tutor_payment_date (tenant_id, deleted_at, payment_date)
);

CREATE TABLE membership_courses (
    tenant_id BIGINT NOT NULL,
    membership_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, membership_id, course_id),
    FOREIGN KEY (tenant_id, membership_id) REFERENCES memberships(tenant_id, membership_id),
    FOREIGN KEY (tenant_id, course_id) REFERENCES courses(tenant_id, course_id),
    INDEX idx_tenant_active_membership_course (tenant_id, deleted_at)
);

CREATE TABLE compensations (
    tenant_id BIGINT NOT NULL,
    compensation_id BIGINT NOT NULL,
    compensation_type VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, compensation_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    INDEX idx_tenant_active_compensation (tenant_id, deleted_at)
);

CREATE TABLE compensation_collaborators (
    tenant_id BIGINT NOT NULL,
    compensation_id BIGINT NOT NULL,
    collaborator_id BIGINT NOT NULL,
    assigned_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, compensation_id, collaborator_id),
    FOREIGN KEY (tenant_id, compensation_id) REFERENCES compensations(tenant_id, compensation_id),
    FOREIGN KEY (tenant_id, collaborator_id) REFERENCES collaborators(tenant_id, collaborator_id),
    INDEX idx_compensation_collaborators (tenant_id, compensation_id)
);
