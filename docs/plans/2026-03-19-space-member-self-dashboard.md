# Space Member Self Dashboard Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add space-scoped self-service publisher and participant dashboard resources for use inside a specific space page.

**Architecture:** Introduce two view services, one for publisher-scoped reads and one for participant-scoped reads. Reuse existing task, membership, submission, and review data, but expose dedicated overview and list resources instead of forcing the frontend to compose general-purpose task APIs.

**Tech Stack:** Spring Boot, Kotlin, Spring MVC, Spring Data JPA, existing auth annotations, OpenAPI YAML, JUnit 5, MockK.

---

### Task 1: Add OpenAPI contracts for publisher self resources

**Files:**
- Modify: `design/API/NT-API.yml`
- Test: `src/test/kotlin/org/rucca/cheese/api/SpaceAnalyticsTest.kt`

**Step 1: Write the failing endpoint assertions**

Add or extend API tests so the suite expects:

- `GET /spaces/{spaceId}/me/publishing`
- `GET /spaces/{spaceId}/me/publishing/tasks`

Verify the response wrapper shape and core business fields.

**Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=SpaceAnalyticsTest test`

Expected: FAIL because the new routes and DTOs do not exist yet.

**Step 3: Add the new endpoint contracts**

Update `design/API/NT-API.yml` with:

- path entries for both endpoints
- query parameters for the publishing task list endpoint
- schemas for publisher overview and publisher task row DTOs

Keep the new shapes independent from admin analytics schemas unless a schema is truly identical.

**Step 4: Run compile to verify contract generation works**

Run: `./mvnw -q -DskipTests compile`

Expected: PASS, generated stubs compile cleanly.

**Step 5: Commit**

```bash
git add design/API/NT-API.yml
git commit -m "feat: define publisher self dashboard api"
```

### Task 2: Add publisher overview and list controller endpoints

**Files:**
- Modify: `src/main/kotlin/org/rucca/cheese/space/SpaceController.kt`
- Modify: `src/main/kotlin/org/rucca/cheese/space/analytics/SpaceAnalyticsService.kt` or remove any temporary wiring if needed
- Create: `src/main/kotlin/org/rucca/cheese/space/view/SpacePublisherViewService.kt`
- Test: `src/test/kotlin/org/rucca/cheese/api/SpaceAnalyticsTest.kt`

**Step 1: Write the failing controller tests**

Add integration coverage proving:

- current user can call the publisher endpoints inside a space
- only the caller's own tasks are returned
- overview values match seeded data

**Step 2: Run the targeted tests**

Run: `./mvnw -q -Dtest=SpaceAnalyticsTest test`

Expected: FAIL because controller methods and service wiring do not exist.

**Step 3: Add the controller methods**

Implement new `SpaceController` methods for:

- `getSpaceMyPublishing(...)`
- `getSpaceMyPublishedTasks(...)`

Keep resource IDs and response wrappers aligned with the generated API interfaces.

**Step 4: Create `SpacePublisherViewService`**

Implement service entry points:

- `getOverview(spaceId, currentUserId)`
- `getPublishedTasks(spaceId, currentUserId, ...)`

Do not call back into external HTTP APIs or re-enter controller DTO wrappers.

**Step 5: Run targeted tests**

Run: `./mvnw -q -Dtest=SpaceAnalyticsTest test`

Expected: PASS for publisher routes.

**Step 6: Commit**

```bash
git add src/main/kotlin/org/rucca/cheese/space/SpaceController.kt src/main/kotlin/org/rucca/cheese/space/view/SpacePublisherViewService.kt src/test/kotlin/org/rucca/cheese/api/SpaceAnalyticsTest.kt
git commit -m "feat: add publisher self dashboard endpoints"
```

### Task 3: Add publisher aggregation tests and implementation

**Files:**
- Create: `src/test/kotlin/org/rucca/cheese/space/view/SpacePublisherViewServiceTest.kt`
- Modify: `src/main/kotlin/org/rucca/cheese/task/TaskMembershipRepository.kt`
- Modify: `src/main/kotlin/org/rucca/cheese/task/TaskSubmissionRepository.kt`
- Modify: `src/main/kotlin/org/rucca/cheese/task/TaskSubmissionReviewRepository.kt`
- Modify: `src/main/kotlin/org/rucca/cheese/space/view/SpacePublisherViewService.kt`

**Step 1: Write the failing service tests**

Cover:

- overview counts for approved, pending, and disapproved tasks
- task row metrics for participant approvals, submissions, pending review, success, and failure
- `latestSubmissionAt`
- filtering and sorting behavior

**Step 2: Run the targeted service tests**

Run: `./mvnw -q -Dtest=SpacePublisherViewServiceTest test`

Expected: FAIL because aggregation helpers are missing.

**Step 3: Add the minimal repository helpers**

Add the smallest missing queries needed to build publisher metrics, for example:

- memberships by task ID set
- submissions by membership ID set
- latest reviews or review presence by submission ID set

Prefer projections or focused queries over broad repository APIs.

**Step 4: Implement the aggregation logic**

Compute:

- participant counts by approval state
- submitted participant count
- pending review count
- successful and failed participant count
- submission conversion rate
- success rate
- latest submission timestamp

**Step 5: Run the targeted service tests**

Run: `./mvnw -q -Dtest=SpacePublisherViewServiceTest test`

Expected: PASS.

**Step 6: Commit**

```bash
git add src/test/kotlin/org/rucca/cheese/space/view/SpacePublisherViewServiceTest.kt src/main/kotlin/org/rucca/cheese/task/TaskMembershipRepository.kt src/main/kotlin/org/rucca/cheese/task/TaskSubmissionRepository.kt src/main/kotlin/org/rucca/cheese/task/TaskSubmissionReviewRepository.kt src/main/kotlin/org/rucca/cheese/space/view/SpacePublisherViewService.kt
git commit -m "feat: add publisher dashboard aggregation"
```

### Task 4: Add OpenAPI contracts for participant self resources

**Files:**
- Modify: `design/API/NT-API.yml`
- Test: `src/test/kotlin/org/rucca/cheese/api/SpaceAnalyticsTest.kt`

**Step 1: Write the failing endpoint assertions**

Extend integration coverage to expect:

- `GET /spaces/{spaceId}/me/participating`
- `GET /spaces/{spaceId}/me/participations`

Include participant-specific fields like `completionStatus` and latest review result.

**Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=SpaceAnalyticsTest test`

Expected: FAIL because participant self resources are not defined yet.

**Step 3: Add the OpenAPI contracts**

Add path entries, filters, and schemas for:

- participant overview
- participation row DTOs

Ensure the row model is participation-oriented, not task-oriented.

**Step 4: Run compile**

Run: `./mvnw -q -DskipTests compile`

Expected: PASS.

**Step 5: Commit**

```bash
git add design/API/NT-API.yml
git commit -m "feat: define participant self dashboard api"
```

### Task 5: Add participant controller endpoints

**Files:**
- Modify: `src/main/kotlin/org/rucca/cheese/space/SpaceController.kt`
- Create: `src/main/kotlin/org/rucca/cheese/space/view/SpaceParticipantViewService.kt`
- Test: `src/test/kotlin/org/rucca/cheese/api/SpaceAnalyticsTest.kt`

**Step 1: Write the failing controller tests**

Cover:

- current user can access the participant endpoints
- only the caller's own participations are returned
- overview values match seeded data

**Step 2: Run the targeted tests**

Run: `./mvnw -q -Dtest=SpaceAnalyticsTest test`

Expected: FAIL because the routes do not exist.

**Step 3: Add controller methods**

Implement:

- `getSpaceMyParticipating(...)`
- `getSpaceMyParticipations(...)`

**Step 4: Add `SpaceParticipantViewService`**

Create service entry points:

- `getOverview(spaceId, currentUserId)`
- `getParticipations(spaceId, currentUserId, ...)`

**Step 5: Run targeted tests**

Run: `./mvnw -q -Dtest=SpaceAnalyticsTest test`

Expected: PASS for route existence and basic payload shape.

**Step 6: Commit**

```bash
git add src/main/kotlin/org/rucca/cheese/space/SpaceController.kt src/main/kotlin/org/rucca/cheese/space/view/SpaceParticipantViewService.kt src/test/kotlin/org/rucca/cheese/api/SpaceAnalyticsTest.kt
git commit -m "feat: add participant self dashboard endpoints"
```

### Task 6: Add participant read-model aggregation

**Files:**
- Create: `src/test/kotlin/org/rucca/cheese/space/view/SpaceParticipantViewServiceTest.kt`
- Modify: `src/main/kotlin/org/rucca/cheese/task/TaskMembershipRepository.kt`
- Modify: `src/main/kotlin/org/rucca/cheese/task/TaskSubmissionRepository.kt`
- Modify: `src/main/kotlin/org/rucca/cheese/task/TaskSubmissionReviewRepository.kt`
- Modify: `src/main/kotlin/org/rucca/cheese/space/view/SpaceParticipantViewService.kt`
- Modify: `src/main/kotlin/org/rucca/cheese/task/service/TaskMembershipViewService.kt` only if a small reusable helper is genuinely needed

**Step 1: Write the failing service tests**

Cover both user and team participation:

- personal task participation row
- team task participation row
- overview counts by approval and completion status
- `canSubmit`
- `latestSubmissionAt`
- `latestReviewAccepted`
- `latestReviewScore`

**Step 2: Run the targeted service tests**

Run: `./mvnw -q -Dtest=SpaceParticipantViewServiceTest test`

Expected: FAIL because the participation read model does not exist yet.

**Step 3: Add minimal repository helpers**

Add focused helpers to resolve:

- memberships related to the current user in a space
- latest submission per membership
- latest review per latest submission

Avoid changing write-path services unless a reusable helper is clearly justified.

**Step 4: Implement the participant read model**

Build participation rows with:

- task info
- publisher summary
- category summary
- participation identity type
- team name if applicable
- approval status
- completion status
- `canSubmit`
- joined and deadline timestamps
- latest submission metadata
- latest review metadata

**Step 5: Run the targeted service tests**

Run: `./mvnw -q -Dtest=SpaceParticipantViewServiceTest test`

Expected: PASS.

**Step 6: Commit**

```bash
git add src/test/kotlin/org/rucca/cheese/space/view/SpaceParticipantViewServiceTest.kt src/main/kotlin/org/rucca/cheese/task/TaskMembershipRepository.kt src/main/kotlin/org/rucca/cheese/task/TaskSubmissionRepository.kt src/main/kotlin/org/rucca/cheese/task/TaskSubmissionReviewRepository.kt src/main/kotlin/org/rucca/cheese/space/view/SpaceParticipantViewService.kt
git commit -m "feat: add participant dashboard read model"
```

### Task 7: Final verification and docs pass

**Files:**
- Modify: any touched implementation/test/spec files from previous tasks

**Step 1: Run focused tests**

Run:

```bash
./mvnw -q -Dtest=SpacePublisherViewServiceTest,SpaceParticipantViewServiceTest,SpaceAnalyticsTest test
```

Expected: PASS.

**Step 2: Run full verification**

Run:

```bash
./mvnw clean verify
```

Expected: `BUILD SUCCESS`.

**Step 3: Review git status**

Run:

```bash
git status --short
```

Expected: only intended files are staged or modified. Do not include unrelated `src/main/resources/application.yml`.

**Step 4: Final commit if needed**

```bash
git add design/API/NT-API.yml src/main/kotlin/org/rucca/cheese/space/SpaceController.kt src/main/kotlin/org/rucca/cheese/space/view/SpacePublisherViewService.kt src/main/kotlin/org/rucca/cheese/space/view/SpaceParticipantViewService.kt src/main/kotlin/org/rucca/cheese/task/TaskMembershipRepository.kt src/main/kotlin/org/rucca/cheese/task/TaskSubmissionRepository.kt src/main/kotlin/org/rucca/cheese/task/TaskSubmissionReviewRepository.kt src/test/kotlin/org/rucca/cheese/api/SpaceAnalyticsTest.kt src/test/kotlin/org/rucca/cheese/space/view/SpacePublisherViewServiceTest.kt src/test/kotlin/org/rucca/cheese/space/view/SpaceParticipantViewServiceTest.kt
git commit -m "feat: add space member self dashboards"
```
