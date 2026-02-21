-- Test data for course-management soft-delete component tests.
-- Inserts the minimum rows required to exercise Course delete with FK schedule children.

-- ── Tenant ────────────────────────────────────────────────────────────────
INSERT INTO tenants (tenant_id, organization_name, email, address)
VALUES (1, 'Test Tenant', 'test@example.com', '123 Test St');

-- ── Tenant Sequences (required by EntityIdAssigner) ──────────────────────
INSERT INTO tenant_sequences (tenant_id, entity_name, next_value, version)
VALUES (1, 'COURSE', 100, 0),
       (1, 'SCHEDULE', 100, 0);

-- ── Courses (one with schedules for FK test) ─────────────────────────────
INSERT INTO courses (tenant_id, course_id, course_name, course_description, max_capacity)
VALUES (1, 1, 'Mathematics 101', 'Introductory mathematics course', 30);

-- ── Schedules (FK-linked to course 1) ────────────────────────────────────
INSERT INTO schedules (tenant_id, schedule_id, schedule_day, start_time, end_time, course_id)
VALUES (1, 1, 'MONDAY', '09:00:00', '10:30:00', 1),
       (1, 2, 'WEDNESDAY', '09:00:00', '10:30:00', 1);
