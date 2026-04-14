package org.rucca.cheese.space.view

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rucca.cheese.common.error.BadRequestError
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.space.models.Space
import org.rucca.cheese.space.models.SpaceCategory
import org.rucca.cheese.task.Task
import org.rucca.cheese.task.TaskCompletionStatus
import org.rucca.cheese.task.TaskMembership
import org.rucca.cheese.task.TaskMembershipRepository
import org.rucca.cheese.task.TaskRepository
import org.rucca.cheese.task.TaskSubmission
import org.rucca.cheese.task.TaskSubmissionRepository
import org.rucca.cheese.task.TaskSubmissionReview
import org.rucca.cheese.task.TaskSubmissionReviewRepository
import org.rucca.cheese.user.User

@ExtendWith(MockKExtension::class)
class SpacePublisherViewServiceTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var taskMembershipRepository: TaskMembershipRepository
    private lateinit var taskSubmissionRepository: TaskSubmissionRepository
    private lateinit var taskSubmissionReviewRepository: TaskSubmissionReviewRepository
    private lateinit var service: SpacePublisherViewService

    @BeforeEach
    fun setUp() {
        taskRepository = mockk()
        taskMembershipRepository = mockk()
        taskSubmissionRepository = mockk()
        taskSubmissionReviewRepository = mockk()
        service =
            SpacePublisherViewService(
                taskRepository = taskRepository,
                taskMembershipRepository = taskMembershipRepository,
                taskSubmissionRepository = taskSubmissionRepository,
                taskSubmissionReviewRepository = taskSubmissionReviewRepository,
            )
    }

    @Test
    fun `getOverview should aggregate publisher task and participation metrics`() {
        val publisherId = 99L
        val taskA =
            createTask(
                id = 101L,
                creatorId = publisherId.toInt(),
                name = "A",
                approved = ApproveType.APPROVED,
                createdHour = 1,
                categoryId = 1L,
                categoryName = "Research",
            )
        val taskB =
            createTask(
                id = 102L,
                creatorId = publisherId.toInt(),
                name = "B",
                approved = ApproveType.NONE,
                createdHour = 2,
                categoryId = 1L,
                categoryName = "Research",
            )
        val taskC =
            createTask(
                id = 103L,
                creatorId = publisherId.toInt(),
                name = "C",
                approved = ApproveType.DISAPPROVED,
                createdHour = 3,
                categoryId = 2L,
                categoryName = "Practice",
            )

        val approvedSuccess =
            createMembership(
                id = 201L,
                task = taskA,
                approved = ApproveType.APPROVED,
                completionStatus = TaskCompletionStatus.SUCCESS,
            )
        val pendingApproval =
            createMembership(
                id = 202L,
                task = taskA,
                approved = ApproveType.NONE,
                completionStatus = TaskCompletionStatus.NOT_SUBMITTED,
            )
        val approvedFailed =
            createMembership(
                id = 203L,
                task = taskB,
                approved = ApproveType.APPROVED,
                completionStatus = TaskCompletionStatus.FAILED,
            )

        val submissionA = createSubmission(301L, approvedSuccess, version = 1, hour = 5)
        val submissionB = createSubmission(302L, approvedFailed, version = 1, hour = 8)
        val reviewB = createReview(401L, submissionB)

        every {
            taskRepository.findAnalyticsTasks(52L, null, null, null, publisherId, null)
        } returns listOf(taskA, taskB, taskC)
        every { taskMembershipRepository.findAllByTaskIdIn(listOf(101L, 102L, 103L)) } returns
            listOf(approvedSuccess, pendingApproval, approvedFailed)
        every { taskSubmissionRepository.findAllByMembershipIdIn(listOf(201L, 202L, 203L)) } returns
            listOf(submissionA, submissionB)
        every { taskSubmissionReviewRepository.findAllBySubmissionIdIn(listOf(301L, 302L)) } returns
            listOf(reviewB)

        val result = service.getOverview(spaceId = 52L, currentUserId = publisherId)

        assertEquals(52L, result.spaceId)
        assertEquals(3, result.taskCount)
        assertEquals(1, result.approvedTaskCount)
        assertEquals(1, result.pendingTaskApprovalCount)
        assertEquals(1, result.disapprovedTaskCount)
        assertEquals(3, result.participantCount)
        assertEquals(2, result.approvedParticipantCount)
        assertEquals(1, result.pendingParticipantApprovalCount)
        assertEquals(2, result.submittedParticipantCount)
        assertEquals(1, result.pendingReviewCount)
        assertEquals(1, result.successfulParticipantCount)
    }

    @Test
    fun `getPublishedTasks should compute task metrics and apply filters and sorting`() {
        val publisherId = 99L
        val taskA =
            createTask(
                id = 101L,
                creatorId = publisherId.toInt(),
                name = "Alpha",
                approved = ApproveType.APPROVED,
                createdHour = 1,
                categoryId = 1L,
                categoryName = "Research",
                deadlineHour = 50,
            )
        val taskB =
            createTask(
                id = 102L,
                creatorId = publisherId.toInt(),
                name = "Beta",
                approved = ApproveType.APPROVED,
                createdHour = 2,
                categoryId = 2L,
                categoryName = "Practice",
                deadlineHour = 60,
            )

        val aApprovedSuccess =
            createMembership(
                id = 201L,
                task = taskA,
                approved = ApproveType.APPROVED,
                completionStatus = TaskCompletionStatus.SUCCESS,
            )
        val aPending =
            createMembership(
                id = 202L,
                task = taskA,
                approved = ApproveType.NONE,
                completionStatus = TaskCompletionStatus.NOT_SUBMITTED,
            )
        val bApprovedFailed =
            createMembership(
                id = 203L,
                task = taskB,
                approved = ApproveType.APPROVED,
                completionStatus = TaskCompletionStatus.FAILED,
            )

        val aSubmissionV1 = createSubmission(301L, aApprovedSuccess, version = 1, hour = 10)
        val aSubmissionV2 = createSubmission(302L, aApprovedSuccess, version = 2, hour = 12)
        val bSubmission = createSubmission(303L, bApprovedFailed, version = 1, hour = 9)
        val bReview = createReview(401L, bSubmission)

        every {
            taskRepository.findAnalyticsTasks(
                52L,
                null,
                null,
                null,
                publisherId,
                ApproveType.APPROVED,
            )
        } returns listOf(taskA, taskB)
        every { taskMembershipRepository.findAllByTaskIdIn(listOf(101L, 102L)) } returns
            listOf(aApprovedSuccess, aPending, bApprovedFailed)
        every { taskSubmissionRepository.findAllByMembershipIdIn(listOf(201L, 202L, 203L)) } returns
            listOf(aSubmissionV1, aSubmissionV2, bSubmission)
        every {
            taskSubmissionReviewRepository.findAllBySubmissionIdIn(listOf(301L, 302L, 303L))
        } returns listOf(bReview)

        val result =
            service.getPublishedTasks(
                spaceId = 52L,
                currentUserId = publisherId,
                from = null,
                to = null,
                categoryId = null,
                approved = "APPROVED",
                hasPendingParticipantApproval = null,
                hasPendingReview = null,
                sortBy = "successRate",
                sortOrder = "desc",
            )

        assertEquals(2, result.tasks.size)
        val alpha = result.tasks.first()
        assertEquals(101L, alpha.taskId)
        assertEquals("Alpha", alpha.taskName)
        assertEquals(2, alpha.participantCount)
        assertEquals(1, alpha.approvedParticipantCount)
        assertEquals(1, alpha.pendingParticipantApprovalCount)
        assertEquals(1, alpha.submittedParticipantCount)
        assertEquals(1, alpha.pendingReviewCount)
        assertEquals(1, alpha.successfulParticipantCount)
        assertEquals(0, alpha.failedParticipantCount)
        assertEquals(1.0, alpha.submissionConversionRate)
        assertEquals(0.5, alpha.successRate)
        assertNotNull(alpha.latestSubmissionAt)

        val pendingOnly =
            service.getPublishedTasks(
                spaceId = 52L,
                currentUserId = publisherId,
                from = null,
                to = null,
                categoryId = null,
                approved = "APPROVED",
                hasPendingParticipantApproval = true,
                hasPendingReview = true,
                sortBy = "createdAt",
                sortOrder = "asc",
            )

        assertEquals(1, pendingOnly.tasks.size)
        assertEquals(101L, pendingOnly.tasks.first().taskId)
    }

    @Test
    fun `getPublishedTasks should reject invalid approved filter`() {
        every {
            taskRepository.findAnalyticsTasks(any(), any(), any(), any(), any(), any())
        } returns emptyList()

        assertThrows(BadRequestError::class.java) {
            service.getPublishedTasks(
                spaceId = 52L,
                currentUserId = 99L,
                from = null,
                to = null,
                categoryId = null,
                approved = "WRONG",
                hasPendingParticipantApproval = null,
                hasPendingReview = null,
                sortBy = "createdAt",
                sortOrder = "desc",
            )
        }
    }

    private fun createTask(
        id: IdType,
        creatorId: Int,
        name: String,
        approved: ApproveType,
        createdHour: Int,
        categoryId: IdType,
        categoryName: String,
        deadlineHour: Int? = null,
    ): Task {
        val space = mockk<Space>(relaxed = true)
        every { space.id } returns 52L
        val creator = mockk<User>(relaxed = true)
        every { creator.id } returns creatorId
        val category = mockk<SpaceCategory>(relaxed = true)
        every { category.id } returns categoryId
        every { category.name } returns categoryName
        every { category.space } returns space
        return mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.name } returns name
            every { this@mockk.approved } returns approved
            every { this@mockk.createdAt } returns LocalDateTime.of(2026, 3, 1, createdHour, 0)
            every { this@mockk.deadline } returns
                deadlineHour?.let { LocalDateTime.of(2026, 3, 3, it % 24, 0) }
            every { this@mockk.creator } returns creator
            every { this@mockk.category } returns category
            every { this@mockk.space } returns space
        }
    }

    private fun createMembership(
        id: IdType,
        task: Task,
        approved: ApproveType,
        completionStatus: TaskCompletionStatus,
    ): TaskMembership =
        mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.task } returns task
            every { this@mockk.approved } returns approved
            every { this@mockk.completionStatus } returns completionStatus
        }

    private fun createSubmission(
        id: IdType,
        membership: TaskMembership,
        version: Int,
        hour: Int,
    ): TaskSubmission =
        mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.membership } returns membership
            every { this@mockk.version } returns version
            every { this@mockk.createdAt } returns LocalDateTime.of(2026, 3, 2, hour, 0)
        }

    private fun createReview(id: IdType, submission: TaskSubmission): TaskSubmissionReview =
        mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.submission } returns submission
        }
}
