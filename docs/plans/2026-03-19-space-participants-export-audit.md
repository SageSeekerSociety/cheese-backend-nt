# Space Participants Export Audit Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add per-user audit logging when exporting space participant analytics CSVs that contain real-name data.

**Architecture:** Reuse `user_real_name_access_logs` instead of introducing a new audit table. When `GET /spaces/{spaceId}/analytics/participants/export` is executed, expand the exported memberships into affected user IDs and write one `EXPORT` audit record per user with `moduleType=SPACE` and `moduleEntityId=spaceId`.

**Tech Stack:** Kotlin, Spring Boot, JPA, existing auth/audit services, JUnit 5, MockK

---

## Task 1: Define audit model changes

**Files:**
- Modify: `src/main/kotlin/org/rucca/cheese/user/models/UserRealNameAccessLog.kt`
- Modify: `src/main/kotlin/org/rucca/cheese/user/services/UserRealNameService.kt`

**Step 1: Write the failing test**

Add a service or API test that expects participant export to create per-user `EXPORT` audit rows using `moduleType=SPACE`.

**Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=SpaceAnalyticsServiceTest test`
Expected: FAIL because analytics export does not write audit logs and `SPACE` is not a supported module type.

**Step 3: Write minimal implementation**

- Extend `AccessModuleType` with `SPACE`
- Teach `UserRealNameService` how to render a `SPACE` entity name in access-log DTO conversion

**Step 4: Run test to verify it passes**

Run: `./mvnw -q -Dtest=SpaceAnalyticsServiceTest test`
Expected: PASS for the new audit-model test.

**Step 5: Commit**

```bash
git add src/main/kotlin/org/rucca/cheese/user/models/UserRealNameAccessLog.kt src/main/kotlin/org/rucca/cheese/user/services/UserRealNameService.kt src/test/kotlin/org/rucca/cheese/space/analytics/SpaceAnalyticsServiceTest.kt
git commit -m "feat: add space export audit model support"
```

## Task 2: Audit participants export

**Files:**
- Modify: `src/main/kotlin/org/rucca/cheese/space/analytics/SpaceAnalyticsService.kt`
- Modify: `src/main/kotlin/org/rucca/cheese/space/analytics/SpaceAnalyticsQueryService.kt`
- Modify: `src/main/kotlin/org/rucca/cheese/task/service/TaskMembershipSnapshotService.kt`
- Test: `src/test/kotlin/org/rucca/cheese/space/analytics/SpaceAnalyticsServiceTest.kt`

**Step 1: Write the failing test**

Add a test that exports participants and asserts:
- one audit row is created per exported user
- team memberships expand to team member IDs
- audit reason contains the filter summary

**Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=SpaceAnalyticsServiceTest test`
Expected: FAIL because export does not call `UserRealNameService.logAccess`.

**Step 3: Write minimal implementation**

- Inject `UserRealNameService` into the export path
- Derive target user IDs from exported memberships
- Log `AccessType.EXPORT` per user with `moduleType=SPACE`

**Step 4: Run test to verify it passes**

Run: `./mvnw -q -Dtest=SpaceAnalyticsServiceTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add src/main/kotlin/org/rucca/cheese/space/analytics/SpaceAnalyticsService.kt src/main/kotlin/org/rucca/cheese/space/analytics/SpaceAnalyticsQueryService.kt src/test/kotlin/org/rucca/cheese/space/analytics/SpaceAnalyticsServiceTest.kt
git commit -m "feat: audit participants export"
```

## Task 3: Cover the HTTP contract and document behavior

**Files:**
- Modify: `src/test/kotlin/org/rucca/cheese/api/SpaceAnalyticsTest.kt`
- Modify: `docs/space-admin-analytics-frontend.md`

**Step 1: Write the failing test**

Add or extend an API-level test to verify participant export still returns CSV while compiling with the new audit path, and document that participant export triggers real-name export audit.

**Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=SpaceAnalyticsTest -DskipTests test-compile`
Expected: test compile passes; runtime test may require local dependent services.

**Step 3: Write minimal implementation**

- Update the frontend/admin doc to note export auditing
- Keep API response unchanged

**Step 4: Run test to verify it passes**

Run: `./mvnw -q spotless:check -DspotlessFiles=docs/space-admin-analytics-frontend.md`
Expected: PASS.

**Step 5: Commit**

```bash
git add src/test/kotlin/org/rucca/cheese/api/SpaceAnalyticsTest.kt docs/space-admin-analytics-frontend.md
git commit -m "docs: note participants export auditing"
```
