USE makani_db;

CREATE TABLE store_product (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    price DOUBLE PRECISION NOT NULL,
    stock_quantity INT NOT NULL
);

CREATE TABLE card_payment_info (
  card_payment_info_id INT AUTO_INCREMENT PRIMARY KEY,
  payment_id BIGINT NOT NULL,
  token TEXT NOT NULL,
  card_type VARCHAR(20) NOT NULL
);

CREATE TABLE email (
    email_id INT AUTO_INCREMENT PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    sender VARCHAR(150) NOT NULL
);

CREATE TABLE email_recipients (
    email_id INT NOT NULL,
    recipient_email VARCHAR(150) NOT NULL,
    PRIMARY KEY (email_id, recipient_email),
    FOREIGN KEY (email_id) REFERENCES email(email_id)
);

CREATE TABLE email_attachments (
    email_id INT NOT NULL,
    attachment_url TEXT NOT NULL,
    PRIMARY KEY (email_id, attachment_url(255)),
    FOREIGN KEY (email_id) REFERENCES email(email_id)
);

CREATE TABLE course (
    course_id INT AUTO_INCREMENT PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    course_description VARCHAR(500) NOT NULL,
    max_capacity INT NOT NULL
);

CREATE TABLE schedule (
    schedule_id INT AUTO_INCREMENT PRIMARY KEY,
    schedule_day VARCHAR(9) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    course_id INT,
    FOREIGN KEY (course_id) REFERENCES course(course_id)
);

CREATE TABLE customer_auth (
    customer_auth_id INT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    token TEXT NOT NULL
);

CREATE TABLE internal_auth (
    internal_auth_id INT AUTO_INCREMENT PRIMARY KEY,
    encrypted_username VARCHAR(500) NOT NULL,
    encrypted_password VARCHAR(500) NOT NULL,
    encrypted_role VARCHAR(500) NOT NULL,
    username_hash VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE compensation (
    compensation_id INT AUTO_INCREMENT PRIMARY KEY,
    compensation_type VARCHAR(50) NOT NULL,
    amount DOUBLE PRECISION NOT NULL
);

CREATE TABLE person_pii (
    person_pii_id INT AUTO_INCREMENT PRIMARY KEY,
    encrypted_first_name VARCHAR(500) NOT NULL,
    encrypted_last_name VARCHAR(500) NOT NULL,
    encrypted_phone_number VARCHAR(500) NOT NULL,
    encrypted_email VARCHAR(500) NOT NULL,
    encrypted_address VARCHAR(500) NOT NULL,
    encrypted_zip_code VARCHAR(500) NOT NULL,
    phone_number_hash VARCHAR(64) NOT NULL UNIQUE,
    email_hash VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE employee (
    employee_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_type VARCHAR(50) NOT NULL,
    birthdate DATE NOT NULL,
    internal_auth_id INT NOT NULL UNIQUE,
    person_pii_id INT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (person_pii_id) REFERENCES person_pii(person_pii_id),
    FOREIGN KEY (internal_auth_id) REFERENCES internal_auth(internal_auth_id)
);

CREATE TABLE store_transaction (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_datetime DATETIME NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    total_amount DOUBLE PRECISION NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    employee_id INT,
    FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
);

CREATE TABLE store_sale_item (
    sale_item_id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price_at_sale DOUBLE PRECISION NOT NULL,
    item_total DOUBLE PRECISION NOT NULL,
    FOREIGN KEY (transaction_id) REFERENCES store_transaction(transaction_id),
    FOREIGN KEY (product_id) REFERENCES store_product(product_id)
);

CREATE TABLE collaborator (
    collaborator_id INT AUTO_INCREMENT PRIMARY KEY,
    internal_auth_id INT NOT NULL UNIQUE,
    encrypted_profile_picture MEDIUMBLOB,
    skills VARCHAR(100) NOT NULL,
    birthdate DATE NOT NULL,
    person_pii_id INT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (person_pii_id) REFERENCES person_pii(person_pii_id),
    FOREIGN KEY (internal_auth_id) REFERENCES internal_auth(internal_auth_id)
);

CREATE TABLE membership (
    membership_id INT AUTO_INCREMENT PRIMARY KEY,
    membership_type VARCHAR(50) NOT NULL,
    fee DOUBLE PRECISION NOT NULL,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE course_available_collaborators (
    course_id INT NOT NULL,
    collaborator_id INT NOT NULL,
    FOREIGN KEY (course_id) REFERENCES course(course_id),
    FOREIGN KEY (collaborator_id) REFERENCES collaborator(collaborator_id),
    PRIMARY KEY (course_id, collaborator_id)
);

CREATE TABLE adult_student (
    adult_student_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_auth_id INT NOT NULL UNIQUE,
    birthdate DATE NOT NULL,
    encrypted_profile_picture MEDIUMBLOB,
    person_pii_id INT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (person_pii_id) REFERENCES person_pii(person_pii_id),
    FOREIGN KEY (customer_auth_id) REFERENCES customer_auth(customer_auth_id)
);

CREATE TABLE tutor (
    tutor_id INT AUTO_INCREMENT PRIMARY KEY,
    birthdate DATE NOT NULL,
    customer_auth_id INT,
    person_pii_id INT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (person_pii_id) REFERENCES person_pii(person_pii_id),
    FOREIGN KEY (customer_auth_id) REFERENCES customer_auth(customer_auth_id)
);

CREATE TABLE minor_student (
    minor_student_id INT AUTO_INCREMENT PRIMARY KEY,
    birthdate DATE NOT NULL,
    encrypted_profile_picture MEDIUMBLOB,
    customer_auth_id INT NOT NULL UNIQUE,
    tutor_id INT NOT NULL,
    person_pii_id INT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (person_pii_id) REFERENCES person_pii(person_pii_id),
    FOREIGN KEY (customer_auth_id) REFERENCES customer_auth(customer_auth_id),
    FOREIGN KEY (tutor_id) REFERENCES tutor(tutor_id)
);

CREATE TABLE adult_student_course (
    adult_student_id INT NOT NULL,
    course_id INT NOT NULL,
    FOREIGN KEY (adult_student_id) REFERENCES adult_student(adult_student_id),
    FOREIGN KEY (course_id) REFERENCES course(course_id),
    PRIMARY KEY (adult_student_id, course_id)
);

CREATE TABLE minor_student_course (
    minor_student_id INT NOT NULL,
    course_id INT NOT NULL,
    FOREIGN KEY (minor_student_id) REFERENCES minor_student(minor_student_id),
    FOREIGN KEY (course_id) REFERENCES course(course_id),
    PRIMARY KEY (minor_student_id, course_id)
);

CREATE TABLE course_event (
    course_event_id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    collaborator_id INT NOT NULL,
    schedule_id INT NOT NULL,
    event_date DATE NOT NULL,
    event_title VARCHAR(100) NOT NULL,
    event_description VARCHAR(500) NOT NULL,
    FOREIGN KEY (course_id) REFERENCES course(course_id),
    FOREIGN KEY (collaborator_id) REFERENCES collaborator(collaborator_id),
    FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id)
);

CREATE TABLE membership_adult_student (
    membership_adult_student_id INT AUTO_INCREMENT PRIMARY KEY,
    membership_id INT NOT NULL,
    adult_student_id INT NOT NULL,
    start_date DATE NOT NULL,
    due_date DATE NOT NULL,
    course_id INT,
    FOREIGN KEY (membership_id) REFERENCES membership(membership_id),
    FOREIGN KEY (course_id) REFERENCES course(course_id),
    FOREIGN KEY (adult_student_id) REFERENCES adult_student(adult_student_id)
);

CREATE TABLE membership_tutor (
    membership_tutor_id INT AUTO_INCREMENT PRIMARY KEY,
    membership_id INT NOT NULL,
    tutor_id INT NOT NULL,
    start_date DATE NOT NULL,
    due_date DATE NOT NULL,
    course_id INT,
    FOREIGN KEY (membership_id) REFERENCES membership(membership_id),
    FOREIGN KEY (course_id) REFERENCES course(course_id),
    FOREIGN KEY (tutor_id) REFERENCES tutor(tutor_id)
);

CREATE TABLE payment_adult_student (
    payment_adult_student_id INT AUTO_INCREMENT PRIMARY KEY,
    payment_date DATE NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    membership_adult_student_id INT NOT NULL,
    FOREIGN KEY (membership_adult_student_id) REFERENCES membership_adult_student(membership_adult_student_id)
);

CREATE TABLE payment_tutor (
    payment_tutor_id INT AUTO_INCREMENT PRIMARY KEY,
    payment_date DATE NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    membership_tutor_id INT NOT NULL,
    FOREIGN KEY (membership_tutor_id) REFERENCES membership_tutor(membership_tutor_id)
);

CREATE TABLE course_event_adult_student_attendees (
    course_event_id INT NOT NULL,
    adult_student_id INT NOT NULL,
    PRIMARY KEY (course_event_id, adult_student_id),
    FOREIGN KEY (course_event_id) REFERENCES course_event(course_event_id),
    FOREIGN KEY (adult_student_id) REFERENCES adult_student(adult_student_id)
);

CREATE TABLE course_event_minor_student_attendees (
    course_event_id INT NOT NULL,
    minor_student_id INT NOT NULL,
    PRIMARY KEY (course_event_id, minor_student_id),
    FOREIGN KEY (course_event_id) REFERENCES course_event(course_event_id),
    FOREIGN KEY (minor_student_id) REFERENCES minor_student(minor_student_id)
);

CREATE TABLE membership_course (
    membership_id INT NOT NULL,
    course_id INT NOT NULL,
    FOREIGN KEY (membership_id) REFERENCES membership(membership_id),
    FOREIGN KEY (course_id) REFERENCES course(course_id),
    PRIMARY KEY (membership_id, course_id)
);