# Space Member Self Dashboard Design

## Goal

Add space-scoped self-service dashboards for members entering from a specific space page:

- publishers can see their own task overview and published task list
- participants can see their own participation overview and participation list

This first phase is intentionally scoped to a single space. Cross-space personal dashboards are out of scope.

## Why New Resources Are Needed

The current backend can partially support both views, but only through general-purpose task APIs:

- publishers can enumerate their tasks with `GET /tasks?space={spaceId}&owner={me}`
- participants can enumerate related tasks with `GET /tasks?space={spaceId}&joined=true`
- task owners can enumerate participants and submissions
- task detail can expose current-user participation identities

Those APIs are insufficient for dashboard pages because they do not expose the state model that the pages need:

- publisher pages need task-level metrics such as pending participant approvals, pending reviews, submission conversion rate, success rate, and latest submission time
- participant pages need stable participation-level state such as completion status, latest submission time, latest review result, and team-vs-user identity

If the frontend builds these pages from existing task APIs, it will either:

- issue N+1 follow-up requests per task, or
- reimplement membership/submission/review aggregation logic client-side

Neither is acceptable for a stable dashboard.

## Information Architecture

The frontend entry point should be inside a specific space page, not in the global personal center.

Recommended navigation:

- admin: analytics
- publisher: my publishing
- participant: my participations

This keeps the first phase aligned with the existing space-scoped admin analytics model and avoids introducing cross-space aggregation early.

## REST Resources

### Publisher resources

- `GET /spaces/{spaceId}/me/publishing`
- `GET /spaces/{spaceId}/me/publishing/tasks`

`/publishing` returns the overview cards for the current user as a publisher in the given space.

`/publishing/tasks` returns the list view for tasks created by the current user in the given
space.

### Participant resources

- `GET /spaces/{spaceId}/me/participating`
- `GET /spaces/{spaceId}/me/participations`

`/participating` returns the overview cards for the current user as a participant in the given space.

`/participations` returns the list view for the current user's participation records in the given space.

## Response Shapes

### `GET /spaces/{spaceId}/me/publishing`

Recommended fields:

- `spaceId`
- `taskCount`
- `approvedTaskCount`
- `pendingTaskApprovalCount`
- `disapprovedTaskCount`
- `participantCount`
- `approvedParticipantCount`
- `pendingParticipantApprovalCount`
- `submittedParticipantCount`
- `pendingReviewCount`
- `successfulParticipantCount`

This resource is a lightweight overview and should not contain task rows.

### `GET /spaces/{spaceId}/me/publishing/tasks`

Recommended filters:

- `categoryId`
- `approved`
- `hasPendingParticipantApproval`
- `hasPendingReview`
- `sortBy=createdAt|participantCount|pendingReviewCount|successRate`
- `sortOrder=asc|desc`

Each row should contain:

- `taskId`
- `taskName`
- `category`
- `approved`
- `createdAt`
- `deadline`
- `participantCount`
- `approvedParticipantCount`
- `pendingParticipantApprovalCount`
- `submittedParticipantCount`
- `pendingReviewCount`
- `successfulParticipantCount`
- `failedParticipantCount`
- `submissionConversionRate`
- `successRate`
- `latestSubmissionAt`

### `GET /spaces/{spaceId}/me/participating`

Recommended fields:

- `spaceId`
- `participationCount`
- `approvedParticipationCount`
- `pendingApprovalCount`
- `awaitingSubmissionCount`
- `pendingReviewCount`
- `resubmittableCount`
- `successfulCount`
- `failedCount`

### `GET /spaces/{spaceId}/me/participations`

Recommended filters:

- `approved`
- `completionStatus`
- `identityType=USER|TEAM`
- `sortBy=joinedAt|deadline|latestSubmissionAt|completionStatus`
- `sortOrder=asc|desc`

Each row should contain:

- `taskId`
- `taskName`
- `publisher`
- `category`
- `participationId`
- `identityType`
- `teamName`
- `approved`
- `completionStatus`
- `canSubmit`
- `joinedAt`
- `deadline`
- `latestSubmissionAt`
- `latestReviewAccepted`
- `latestReviewScore`

## Domain Naming

Service names should stay role-oriented and avoid UI terms such as `dashboard`.

Recommended services:

- `SpacePublisherViewService`
- `SpaceParticipantViewService`

The paths remain current-user-oriented while the services remain domain-oriented.

## Reuse From Existing Backend

Existing code that can be reused:

- task enumeration by owner and joined status
- task owner permissions for participant and submission enumeration
- task membership completion status
- task participation identity modeling
- admin analytics aggregation patterns

Relevant existing pieces:

- `TaskService.enumerateTasks(...)`
- `TaskMembershipViewService.getUserParticipationInfo(...)`
- `TaskMembership.completionStatus`
- `TaskAuth`
- `SpaceAnalyticsQueryService`

## Missing Backend Capabilities

### Publisher-side gaps

The current task list does not expose enough task metrics:

- pending participant approvals
- pending reviews
- submitted participant count
- success rate
- latest submission timestamp

### Participant-side gaps

The current participation identity model is too thin for dashboard usage:

- `TaskParticipationIdentity` has no `completionStatus`
- it has no `joinedAt`
- it has no latest submission metadata
- it has no latest review metadata

This means the participant dashboard needs a dedicated participation read model rather than a repackaged task list.

## Query and Aggregation Model

The new resources should not be built in controllers by chaining existing APIs.

Recommended service split:

- `SpacePublisherViewService`
  - publisher overview
  - published task list
- `SpaceParticipantViewService`
  - participant overview
  - participation list

Recommended aggregation responsibilities:

### Publisher aggregation

Filter tasks by:

- `spaceId`
- `creatorId = currentUserId`

Aggregate per task:

- participant counts by approval state
- submitted participant count
- pending review count
- success and failure counts
- latest submission time

Then aggregate all visible tasks again for the overview cards.

### Participant aggregation

Resolve participation records by:

- personal memberships where `memberId == currentUserId`
- team memberships where one of the current user's teams joined the task

Aggregate per participation:

- approval status
- completion status
- submit capability
- joined time
- deadline
- latest submission time
- latest review accepted/score

Then aggregate all visible participations again for the overview cards.

## Repository Strategy

The first version does not need heavy native SQL immediately.

A pragmatic first cut is:

1. add missing repository helpers and projections
2. aggregate in `SpacePublisherViewService` and `SpaceParticipantViewService`
3. move hot paths to dedicated query repositories only after the resources stabilize

Minimum likely helpers:

- task memberships by task ID set
- latest submission by membership ID set
- latest review by submission ID set
- team memberships for current user in a space

## Permission Model

These resources are self-scoped and should not require admin privileges.

Behavior:

- publishing resources only return tasks whose creator is the current user
- participation resources only return participation records related to the current user

This can be implemented without introducing a large new permission system in the first phase.

If the team wants explicit permission IDs later, suitable candidates are:

- `space:query:self-publishing`
- `space:query:self-participating`

## Delivery Order

Recommended implementation order:

1. publisher overview and published task list
2. participant overview and participation list

Publisher resources are simpler and will validate the `space-scoped self view` pattern before the more complex participation read model lands.

## Non-goals

The first phase does not include:

- cross-space personal dashboards
- teacher/student terminology in API resources
- CSV exports for self dashboards
- trend charts or admin-style analytics distributions
- a new front-end navigation system outside the current space page
