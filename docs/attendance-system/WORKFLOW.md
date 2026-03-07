# Attendance System — Workflow Documentation

## Overview

The Attendance System validates student entrance to class using **Animated QR codes** that rotate every ~15 seconds. The architecture supports future extensibility to NFC, BLE, and manual verification methods.

Attendance is tracked via dedicated `AttendanceSession` and `AttendanceRecord` entities — existing M2M junction tables on `CourseEventDataModel` are left untouched.

---

## End-to-End Flow

```
┌──────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌──────────────┐
│   Scheduler  │───>│  Create Session  │───>│  QR Generation  │───>│  Student App │
│  (auto-start)│    │  (HMAC secret)   │    │  (rotating token)│    │  (scan QR)   │
└──────────────┘    └─────────────────┘    └─────────────────┘    └──────┬───────┘
                                                                         │
                                                                         v
                    ┌─────────────────┐    ┌─────────────────┐    ┌──────────────┐
                    │  Session Close   │<───│  AttendanceRecord│<───│   Check-In   │
                    │  (manual/auto)   │    │  (persisted)     │    │  (validate)  │
                    └─────────────────┘    └─────────────────┘    └──────────────┘
```

---

## 1. Session Auto-Start (Scheduler)

### Component
`AttendanceSessionScheduler` — `@Scheduled(fixedDelay)` background task

### Behavior
1. Runs periodically (configurable interval, default 60s)
2. Queries `ScheduleRepository.findActiveByDayAndTime()` across **all tenants** for schedules matching the current day-of-week and whose start/end time window contains `now`
3. Groups matching schedules by `tenantId`
4. For each tenant group:
   - Creates synthetic `RequestAttributes` (same pattern as `ScheduledNotificationDispatcher`)
   - Sets `TenantContextHolder.setTenantId(tenantId)` via `ApplicationContext.getBean()`
   - For each matching schedule:
     - Finds or creates a `CourseEvent` for today's date
     - Checks if an ACTIVE `AttendanceSession` already exists for that event
     - If not, creates a new session with a random HMAC secret
5. Cleans up `RequestContextHolder` in `finally` block

### Cross-Tenant Safety
```
┌─ Scheduler Thread ─────────────────────────────────┐
│                                                     │
│  for each tenantId in matchingSchedules:            │
│    ┌─ Synthetic RequestScope ─────────────────────┐ │
│    │  TenantContextHolder.setTenantId(tenantId)   │ │
│    │  → find/create CourseEvent for today          │ │
│    │  → create AttendanceSession if none active    │ │
│    └──────────────────────────────────────────────┘ │
│    finally: resetRequestAttributes()                │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 2. Session Creation

### API
`POST /v1/attendance-system/sessions`

### Request
```json
{
  "courseEventId": 42
}
```

### Process
1. `AttendanceSessionCreationUseCase.create(dto)` — `@Transactional`
2. `transform(dto)`:
   - Gets prototype `AttendanceSessionDataModel` from `ApplicationContext`
   - Maps DTO via named TypeMap (`MAP_NAME = "attendanceSessionMap"`)
   - Generates 256-bit HMAC secret via `SecureRandom`, Base64-encoded
   - Sets `status = ACTIVE`, `startedAt = now`, `tokenIntervalSeconds = 15` (configurable)
   - Resolves `CourseEvent` FK via repository lookup
3. Persists via `saveAndFlush()`
4. Returns `AttendanceSessionCreateResponseDTO` with `attendanceSessionId`

### Response
```json
{
  "attendanceSessionId": 1
}
```

---

## 3. QR Token Generation

### API
`GET /v1/attendance-system/sessions/{sessionId}/qr-token`

### Token Structure
```
Base64URL( {sessionId}:{nonce}:{timestamp} . {signature} )
```

### Process
1. `QrTokenGenerationUseCase.generateToken(sessionId)`
2. Validates session exists and is ACTIVE
3. Computes deterministic window number:
   ```
   windowNumber = currentEpochSeconds / tokenIntervalSeconds
   ```
4. Generates nonce via HMAC-SHA256:
   ```
   nonce = HMAC-SHA256(sessionSecret, windowNumber)
   ```
5. Builds payload: `{sessionId}:{nonce}:{timestamp}`
6. Signs payload: `signature = HMAC-SHA256(sessionSecret, payload)`
7. Returns Base64URL-encoded `{payload}.{signature}`

### Token Rotation
```
Time ──────────────────────────────────────────────>
       │  Window 1  │  Window 2  │  Window 3  │
       │  Token A   │  Token B   │  Token C   │
       │← 15 sec ──>│← 15 sec ──>│← 15 sec ──>│
```

- Tokens rotate deterministically — **no server-side nonce storage** needed
- Both client and server compute the same token for the same time window
- Tolerance: accepts current window ± 1 to handle clock skew

### Response
```json
{
  "token": "eyJzZXNzaW9uSWQiOjE...",
  "expiresInSeconds": 15
}
```

---

## 4. Student Check-In

### API
`POST /v1/attendance-system/check-in`

### Request
```json
{
  "token": "eyJzZXNzaW9uSWQiOjE...",
  "studentId": 5,
  "studentType": "ADULT",
  "verificationMethod": "ANIMATED_QR",
  "deviceFingerprint": "optional-device-id"
}
```

### Validation Chain
```
Token Received
  │
  ├─ Parse token → extract sessionId
  │
  ├─ Load AttendanceSession
  │   └─ 404 if not found (AttendanceSessionNotFoundException)
  │
  ├─ Check session.status == ACTIVE
  │   └─ 409 if CLOSED (AttendanceSessionClosedException)
  │
  ├─ Resolve verification strategy by verificationMethod
  │   └─ strategy.verify(token, session)
  │       └─ 401 if invalid (InvalidQrTokenException)
  │
  ├─ Check student enrolled in course
  │   └─ 403 if not enrolled (StudentNotEnrolledException)
  │
  ├─ Check not already checked in (session + student + type)
  │   └─ 409 if duplicate (StudentAlreadyCheckedInException)
  │
  └─ Create AttendanceRecord
      └─ 201 Created
```

### Response
```json
{
  "attendanceRecordId": 1,
  "checkedInAt": "2026-03-07T10:05:30"
}
```

---

## 5. Get Session Details

### API
`GET /v1/attendance-system/sessions/{sessionId}`

### Response
```json
{
  "attendanceSessionId": 1,
  "courseEventId": 42,
  "status": "ACTIVE",
  "tokenIntervalSeconds": 15,
  "startedAt": "2026-03-07T10:00:00",
  "closedAt": null
}
```

---

## 6. Get Attendance Records

### API
`GET /v1/attendance-system/sessions/{sessionId}/records`

### Response
```json
[
  {
    "attendanceRecordId": 1,
    "studentId": 5,
    "studentType": "ADULT",
    "verificationMethod": "ANIMATED_QR",
    "checkedInAt": "2026-03-07T10:05:30",
    "deviceFingerprint": "device-abc"
  }
]
```

---

## 7. Session Close

### API
`PUT /v1/attendance-system/sessions/{sessionId}/close`

### Process
1. `CloseAttendanceSessionUseCase.close(sessionId)`
2. Loads session, validates it's ACTIVE
3. Sets `status = CLOSED`, `closedAt = now`
4. Persists via `saveAndFlush()`

### Response
```json
{
  "attendanceSessionId": 1,
  "status": "CLOSED",
  "closedAt": "2026-03-07T11:00:00"
}
```

---

## API Sequence Diagram

```
Instructor App         Backend API              Database
     │                      │                       │
     │  POST /sessions      │                       │
     │─────────────────────>│                       │
     │                      │  INSERT session       │
     │                      │──────────────────────>│
     │  201 {sessionId}     │                       │
     │<─────────────────────│                       │
     │                      │                       │
     │  GET /sessions/1/    │                       │
     │     qr-token         │                       │
     │─────────────────────>│                       │
     │                      │  SELECT session       │
     │                      │──────────────────────>│
     │  200 {token, expiry} │  (compute HMAC)       │
     │<─────────────────────│                       │
     │                      │                       │
     │        ┌─── Student App scans QR ───┐        │
     │        │                            │        │
     │        │  POST /check-in            │        │
     │        │───────────────────────────>│        │
     │        │                            │  validate token  │
     │        │                            │  check enrollment│
     │        │                            │  check duplicate │
     │        │                            │  INSERT record   │
     │        │                            │─────────────────>│
     │        │  201 {recordId, time}      │                  │
     │        │<───────────────────────────│                  │
     │        └────────────────────────────┘                  │
     │                      │                       │
     │  PUT /sessions/1/    │                       │
     │     close            │                       │
     │─────────────────────>│                       │
     │                      │  UPDATE status=CLOSED │
     │                      │──────────────────────>│
     │  200 {status: CLOSED}│                       │
     │<─────────────────────│                       │
```

---

## Verification Strategy Architecture

```
                ┌──────────────────────────────┐
                │  AttendanceVerificationStrategy │
                │  (interface)                    │
                │  ─────────────────────────────  │
                │  + getMethod(): VerificationMethod │
                │  + verify(token, session): boolean  │
                └──────────┬───────────────────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
  ┌────────┴──────┐ ┌─────┴──────┐ ┌─────┴──────┐
  │ AnimatedQr    │ │   NFC      │ │   BLE      │
  │ Strategy      │ │ Strategy   │ │ Strategy   │
  │ (HMAC-SHA256) │ │ (future)   │ │ (future)   │
  └───────────────┘ └────────────┘ └────────────┘
```

---

## Database Tables

### attendance_sessions
| Column | Type | Notes |
|--------|------|-------|
| tenant_id | BIGINT | Composite PK |
| attendance_session_id | BIGINT | Composite PK |
| course_event_id | BIGINT | FK → course_events |
| status | VARCHAR(20) | ACTIVE / CLOSED |
| qr_secret | VARCHAR(512) | HMAC-SHA256 secret (Base64) |
| token_interval_seconds | INT | QR rotation interval |
| started_at | TIMESTAMP | Session start time |
| closed_at | TIMESTAMP | Nullable, set on close |
| created_at / updated_at / deleted_at | TIMESTAMP | Audit + soft delete |

### attendance_records
| Column | Type | Notes |
|--------|------|-------|
| tenant_id | BIGINT | Composite PK |
| attendance_record_id | BIGINT | Composite PK |
| attendance_session_id | BIGINT | FK → attendance_sessions |
| student_id | BIGINT | Student identifier |
| student_type | VARCHAR(20) | ADULT / MINOR |
| verification_method | VARCHAR(30) | ANIMATED_QR / NFC / BLE / MANUAL |
| checked_in_at | TIMESTAMP | Check-in timestamp |
| device_fingerprint | VARCHAR(255) | Optional audit field |
| created_at / updated_at / deleted_at | TIMESTAMP | Audit + soft delete |
| UNIQUE | — | (tenant, session, student, type, deleted_at) |
