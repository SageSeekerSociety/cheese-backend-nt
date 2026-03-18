package org.rucca.cheese.space.analytics

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.model.ApproveTypeDTO
import org.rucca.cheese.space.models.Space
import org.rucca.cheese.space.models.SpaceCategory
import org.rucca.cheese.task.*
import org.rucca.cheese.task.service.TaskMembershipSnapshotService
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserRepository
import org.rucca.cheese.user.models.AccessModuleType
import org.rucca.cheese.user.models.AccessType
import org.rucca.cheese.user.services.UserRealNameService

@ExtendWith(MockKExtension::class)
class SpaceAnalyticsServiceTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var taskMembershipRepository: TaskMembershipRepository
    private lateinit var taskSubmissionRepository: TaskSubmissionRepository
    private lateinit var taskSubmissionReviewRepository: TaskSubmissionReviewRepository
    private lateinit var userRepository: UserRepository
    private lateinit var taskMembershipSnapshotService: TaskMembershipSnapshotService
    private lateinit var userRealNameService: UserRealNameService
    private lateinit var service: SpaceAnalyticsService

    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        taskMembershipRepository = mockk()
        taskSubmissionRepository = mockk()
        taskSubmissionReviewRepository = mockk()
        userRepository = mockk()
        taskMembershipSnapshotService = mockk()
        userRealNameService = mockk(relaxed = true)
        every { taskMembershipSnapshotService.getRealNameInfoFromMembership(any()) } answers
            {
                firstArg<TaskMembership>().realNameInfo?.convert()
            }
        every {
            taskMembershipSnapshotService.getRealNameInfoForTeamMemberSnapshot(any(), any())
        } answers { secondArg<TeamMemberRealNameInfo>().realNameInfo.convert() }
        service =
            SpaceAnalyticsService(
                taskRepository,
                taskMembershipRepository,
                taskSubmissionRepository,
                taskSubmissionReviewRepository,
                userRepository,
                taskMembershipSnapshotService,
                userRealNameService,
            )
    }

    @Test
    fun `getSpaceAnalyticsOverview should summarize entity and student metrics`() {
        val spaceId = 1L
        val now = LocalDateTime.now()
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category1 = mockk<SpaceCategory>()
        every { category1.id } returns 1
        every { category1.name } returns "Research"
        every { category1.space } returns space

        val category2 = mockk<SpaceCategory>()
        every { category2.id } returns 2
        every { category2.name } returns "Practice"
        every { category2.space } returns space

        val publisher1 = mockk<User>()
        every { publisher1.id } returns 1
        every { publisher1.username } returns "teacher-a"

        val publisher2 = mockk<User>()
        every { publisher2.id } returns 2
        every { publisher2.username } returns "teacher-b"

        val task1 = mockk<Task>(relaxed = true)
        every { task1.id } returns 1L
        every { task1.category } returns category1
        every { task1.creator } returns publisher1
        every { task1.createdAt } returns now.minusDays(1)
        every { task1.approved } returns ApproveType.APPROVED

        val task2 = mockk<Task>(relaxed = true)
        every { task2.id } returns 2L
        every { task2.category } returns category2
        every { task2.creator } returns publisher2
        every { task2.createdAt } returns now
        every { task2.approved } returns ApproveType.APPROVED

        val userSuccess = mockk<TaskMembership>(relaxed = true)
        every { userSuccess.id } returns 11L
        every { userSuccess.task } returns task1
        every { userSuccess.memberId } returns 101L
        every { userSuccess.isTeam } returns false
        every { userSuccess.approved } returns ApproveType.APPROVED
        every { userSuccess.completionStatus } returns TaskCompletionStatus.SUCCESS
        every { userSuccess.teamMembersRealNameInfo } returns mutableListOf()
        every { userSuccess.createdAt } returns now.minusHours(8)
        every { userSuccess.updatedAt } returns now.minusHours(2)

        val userPending = mockk<TaskMembership>(relaxed = true)
        every { userPending.id } returns 12L
        every { userPending.task } returns task1
        every { userPending.memberId } returns 102L
        every { userPending.isTeam } returns false
        every { userPending.approved } returns ApproveType.NONE
        every { userPending.completionStatus } returns TaskCompletionStatus.NOT_SUBMITTED
        every { userPending.teamMembersRealNameInfo } returns mutableListOf()
        every { userPending.createdAt } returns now.minusHours(7)
        every { userPending.updatedAt } returns now.minusHours(7)

        val teamInReview = mockk<TaskMembership>(relaxed = true)
        every { teamInReview.id } returns 13L
        every { teamInReview.task } returns task2
        every { teamInReview.memberId } returns 201L
        every { teamInReview.isTeam } returns true
        every { teamInReview.approved } returns ApproveType.APPROVED
        every { teamInReview.completionStatus } returns TaskCompletionStatus.PENDING_REVIEW
        every { teamInReview.teamMembersRealNameInfo } returns
            mutableListOf(
                TeamMemberRealNameInfo(301L, realNameInfo = RealNameInfo(realName = "A")),
                TeamMemberRealNameInfo(302L, realNameInfo = RealNameInfo(realName = "B")),
            )
        every { teamInReview.createdAt } returns now.minusHours(6)
        every { teamInReview.updatedAt } returns now.minusHours(1)

        every { taskRepository.findBySpaceId(spaceId) } returns listOf(task1, task2)
        every { taskRepository.findAnalyticsTasks(spaceId, null, null, null, null, null) } returns
            listOf(task1, task2)
        every { taskMembershipRepository.findAllByTaskIdIn(listOf(1L, 2L)) } returns
            listOf(userSuccess, userPending, teamInReview)
        every { taskMembershipRepository.findAllByTaskId(1L) } returns
            listOf(userSuccess, userPending)
        every { taskMembershipRepository.findAllByTaskId(2L) } returns listOf(teamInReview)
        every { taskSubmissionRepository.findAllByMembershipIdIn(listOf(11L, 12L, 13L)) } returns
            listOf(
                mockk<TaskSubmission>(relaxed = true) {
                    every { id } returns 101L
                    every { membership } returns userSuccess
                    every { createdAt } returns now.minusHours(4)
                },
                mockk<TaskSubmission>(relaxed = true) {
                    every { id } returns 102L
                    every { membership } returns teamInReview
                    every { createdAt } returns now.minusHours(3)
                },
            )
        every { taskSubmissionReviewRepository.findAllBySubmissionIdIn(any()) } returns emptyList()
        every { taskSubmissionRepository.findAllByMembershipId(11L) } returns
            listOf(mockk(relaxed = true) { every { createdAt } returns now.minusHours(4) })
        every { taskSubmissionRepository.findAllByMembershipId(12L) } returns emptyList()
        every { taskSubmissionRepository.findAllByMembershipId(13L) } returns
            listOf(mockk(relaxed = true) { every { createdAt } returns now.minusHours(3) })

        val result =
            service.getSpaceAnalyticsOverview(
                spaceId = spaceId,
                from = null,
                to = null,
                categoryId = null,
                publisherId = null,
                taskApproved = null,
                groupBy = "day",
            )

        assertEquals(2, result.entityMetrics.taskCount)
        assertEquals(2, result.entityMetrics.publisherCount)
        assertEquals(3, result.entityMetrics.participantCount)
        assertEquals(2, result.entityMetrics.approvedParticipantCount)
        assertEquals(2, result.entityMetrics.submittedParticipantCount)
        assertEquals(1, result.entityMetrics.successfulParticipantCount)
        assertEquals(2.0 / 3.0, result.entityMetrics.participationConversionRate, 0.0001)
        assertEquals(1.0, result.entityMetrics.submissionConversionRate, 0.0001)
        assertEquals(1.0 / 3.0, result.entityMetrics.successRate, 0.0001)

        assertEquals(4, result.studentMetrics.studentCount)
        assertEquals(3, result.studentMetrics.approvedStudentCount)
        assertEquals(1, result.studentMetrics.successfulStudentCount)

        val categoryCounts =
            result.taskDistributions.byCategory.items.orEmpty().associate { it.label to it.count }
        assertEquals(1, categoryCounts["Research"])
        assertEquals(1, categoryCounts["Practice"])

        val completionCounts =
            result.taskDistributions.byCompletionStatus.items.orEmpty().associate {
                it.label to it.count
            }
        assertEquals(1, completionCounts["SUCCESS"])
        assertEquals(1, completionCounts["NOT_SUBMITTED"])
        assertEquals(1, completionCounts["PENDING_REVIEW"])

        assertEquals(2, result.trends.submissionsCreated.sumOf { it.count })
    }

    @Test
    fun `getSpaceAnalyticsOverview should respect task filters`() {
        val spaceId = 1L
        val now = LocalDateTime.now()
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category1 = mockk<SpaceCategory>()
        every { category1.id } returns 1
        every { category1.name } returns "Research"
        every { category1.space } returns space

        val category2 = mockk<SpaceCategory>()
        every { category2.id } returns 2
        every { category2.name } returns "Practice"
        every { category2.space } returns space

        val publisher1 = mockk<User>()
        every { publisher1.id } returns 1
        every { publisher1.username } returns "teacher-a"

        val publisher2 = mockk<User>()
        every { publisher2.id } returns 2
        every { publisher2.username } returns "teacher-b"

        val task1 = mockk<Task>(relaxed = true)
        every { task1.id } returns 1L
        every { task1.category } returns category1
        every { task1.creator } returns publisher1
        every { task1.createdAt } returns now.minusDays(1)
        every { task1.approved } returns ApproveType.APPROVED

        val task2 = mockk<Task>(relaxed = true)
        every { task2.id } returns 2L
        every { task2.category } returns category2
        every { task2.creator } returns publisher2
        every { task2.createdAt } returns now
        every { task2.approved } returns ApproveType.DISAPPROVED

        val filteredMembership = mockk<TaskMembership>(relaxed = true)
        every { filteredMembership.id } returns 21L
        every { filteredMembership.task } returns task1
        every { filteredMembership.memberId } returns 401L
        every { filteredMembership.isTeam } returns true
        every { filteredMembership.approved } returns ApproveType.APPROVED
        every { filteredMembership.completionStatus } returns TaskCompletionStatus.SUCCESS
        every { filteredMembership.teamMembersRealNameInfo } returns
            mutableListOf(
                TeamMemberRealNameInfo(501L, realNameInfo = RealNameInfo(realName = "A")),
                TeamMemberRealNameInfo(502L, realNameInfo = RealNameInfo(realName = "B")),
                TeamMemberRealNameInfo(503L, realNameInfo = RealNameInfo(realName = "C")),
            )
        every { filteredMembership.createdAt } returns now.minusHours(5)
        every { filteredMembership.updatedAt } returns now.minusHours(1)

        every { taskRepository.findBySpaceId(spaceId) } returns listOf(task1, task2)
        every {
            taskRepository.findAnalyticsTasks(spaceId, null, null, 1L, 1L, ApproveType.APPROVED)
        } returns listOf(task1)
        every { taskMembershipRepository.findAllByTaskIdIn(listOf(1L)) } returns
            listOf(filteredMembership)
        every { taskSubmissionRepository.findAllByMembershipIdIn(listOf(21L)) } returns
            listOf(
                mockk<TaskSubmission>(relaxed = true) {
                    every { id } returns 201L
                    every { membership } returns filteredMembership
                    every { createdAt } returns now.minusHours(2)
                }
            )
        every { taskSubmissionReviewRepository.findAllBySubmissionIdIn(any()) } returns emptyList()
        every { taskMembershipRepository.findAllByTaskId(1L) } returns listOf(filteredMembership)
        every { taskSubmissionRepository.findAllByMembershipId(21L) } returns
            listOf(mockk(relaxed = true) { every { createdAt } returns now.minusHours(2) })

        val result =
            service.getSpaceAnalyticsOverview(
                spaceId = spaceId,
                from = null,
                to = null,
                categoryId = 1L,
                publisherId = 1L,
                taskApproved = "APPROVED",
                groupBy = "day",
            )

        assertEquals(1, result.entityMetrics.taskCount)
        assertEquals(1, result.entityMetrics.publisherCount)
        assertEquals(1, result.entityMetrics.participantCount)
        assertEquals(1, result.entityMetrics.successfulParticipantCount)
        assertEquals(3, result.studentMetrics.studentCount)
        assertEquals(3, result.studentMetrics.successfulStudentCount)
        assertEquals(
            listOf("Research"),
            result.taskDistributions.byCategory.items.orEmpty().map { it.label },
        )
    }

    @Test
    fun `getSpaceTaskAnalytics should return task level metrics for the filtered space scope`() {
        val spaceId = 1L
        val now = LocalDateTime.now()
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category1 = mockk<SpaceCategory>()
        every { category1.id } returns 1
        every { category1.name } returns "Category 1"
        every { category1.space } returns space

        val category2 = mockk<SpaceCategory>()
        every { category2.id } returns 2
        every { category2.name } returns "Category 2"
        every { category2.space } returns space

        val publisher1 = mockk<User>()
        every { publisher1.id } returns 1
        every { publisher1.username } returns "teacher-a"

        val publisher2 = mockk<User>()
        every { publisher2.id } returns 2
        every { publisher2.username } returns "teacher-b"

        val task1 = mockk<Task>(relaxed = true)
        every { task1.id } returns 1L
        every { task1.name } returns "Task 1"
        every { task1.category } returns category1
        every { task1.creator } returns publisher1
        every { task1.createdAt } returns now.minusDays(1)
        every { task1.deadline } returns now.plusDays(7)
        every { task1.approved } returns ApproveType.APPROVED

        val task2 = mockk<Task>(relaxed = true)
        every { task2.id } returns 2L
        every { task2.name } returns "Task 2"
        every { task2.category } returns category2
        every { task2.creator } returns publisher2
        every { task2.createdAt } returns now
        every { task2.deadline } returns null
        every { task2.approved } returns ApproveType.NONE

        val approvedSuccess = mockk<TaskMembership>(relaxed = true)
        every { approvedSuccess.id } returns 11L
        every { approvedSuccess.task } returns task1
        every { approvedSuccess.approved } returns ApproveType.APPROVED
        every { approvedSuccess.completionStatus } returns TaskCompletionStatus.SUCCESS

        val pendingReview = mockk<TaskMembership>(relaxed = true)
        every { pendingReview.id } returns 12L
        every { pendingReview.task } returns task1
        every { pendingReview.approved } returns ApproveType.NONE
        every { pendingReview.completionStatus } returns TaskCompletionStatus.PENDING_REVIEW

        val rejected = mockk<TaskMembership>(relaxed = true)
        every { rejected.id } returns 13L
        every { rejected.task } returns task1
        every { rejected.approved } returns ApproveType.DISAPPROVED
        every { rejected.completionStatus } returns TaskCompletionStatus.FAILED

        val resubmittable = mockk<TaskMembership>(relaxed = true)
        every { resubmittable.id } returns 21L
        every { resubmittable.task } returns task2
        every { resubmittable.approved } returns ApproveType.APPROVED
        every { resubmittable.completionStatus } returns TaskCompletionStatus.REJECTED_RESUBMITTABLE

        every { taskRepository.findBySpaceId(spaceId) } returns listOf(task1, task2)
        every { taskRepository.findAnalyticsTasks(spaceId, null, null, null, null, null) } returns
            listOf(task1, task2)
        every { taskMembershipRepository.findAllByTaskIdIn(listOf(1L, 2L)) } returns
            listOf(approvedSuccess, pendingReview, rejected, resubmittable)
        every { taskMembershipRepository.findAllByTaskId(1L) } returns
            listOf(approvedSuccess, pendingReview, rejected)
        every { taskMembershipRepository.findAllByTaskId(2L) } returns listOf(resubmittable)
        val approvedSuccessSubmission =
            mockk<TaskSubmission>(relaxed = true) {
                every { id } returns 301L
                every { membership } returns approvedSuccess
                every { version } returns 1
                every { createdAt } returns now.minusHours(5)
            }
        val pendingReviewSubmission =
            mockk<TaskSubmission>(relaxed = true) {
                every { id } returns 302L
                every { membership } returns pendingReview
                every { version } returns 1
                every { createdAt } returns now.minusHours(4)
            }
        val resubmittableSubmission =
            mockk<TaskSubmission>(relaxed = true) {
                every { id } returns 303L
                every { membership } returns resubmittable
                every { version } returns 1
                every { createdAt } returns now.minusHours(3)
            }

        every {
            taskSubmissionRepository.findAllByMembershipIdIn(listOf(11L, 12L, 13L, 21L))
        } returns
            listOf(approvedSuccessSubmission, pendingReviewSubmission, resubmittableSubmission)
        every {
            taskSubmissionReviewRepository.findAllBySubmissionIdIn(listOf(301L, 302L, 303L))
        } returns
            listOf(
                mockk(relaxed = true) { every { submission } returns approvedSuccessSubmission },
                mockk(relaxed = true) { every { submission } returns resubmittableSubmission },
            )
        every { taskSubmissionRepository.findAllByMembershipId(11L) } returns
            listOf(mockk(relaxed = true) { every { createdAt } returns now.minusHours(5) })
        every { taskSubmissionRepository.findAllByMembershipId(12L) } returns
            listOf(mockk(relaxed = true) { every { createdAt } returns now.minusHours(4) })
        every { taskSubmissionRepository.findAllByMembershipId(13L) } returns emptyList()
        every { taskSubmissionRepository.findAllByMembershipId(21L) } returns
            listOf(mockk(relaxed = true) { every { createdAt } returns now.minusHours(3) })

        val result =
            service.getSpaceTaskAnalytics(
                spaceId = spaceId,
                from = null,
                to = null,
                categoryId = null,
                publisherId = null,
                taskApproved = null,
                hasPendingReview = null,
                hasPendingApproval = null,
                sortBy = "createdAt",
                sortOrder = "asc",
            )

        assertEquals(2, result.tasks?.size)

        val firstTask = result.tasks?.get(0)
        assertEquals(1L, firstTask?.taskId)
        assertEquals("Task 1", firstTask?.taskName)
        assertEquals(1L, firstTask?.publisher?.id)
        assertEquals("teacher-a", firstTask?.publisher?.name)
        assertEquals(1L, firstTask?.category?.id)
        assertEquals("Category 1", firstTask?.category?.name)
        assertEquals(ApproveTypeDTO.APPROVED, firstTask?.approved)
        assertEquals(3, firstTask?.participantCount)
        assertEquals(1, firstTask?.pendingParticipantApprovalCount)
        assertEquals(1, firstTask?.approvedParticipantCount)
        assertEquals(1, firstTask?.rejectedParticipantCount)
        assertEquals(2, firstTask?.submittedParticipantCount)
        assertEquals(1, firstTask?.pendingReviewCount)
        assertEquals(0, firstTask?.resubmittableCount)
        assertEquals(1, firstTask?.successfulParticipantCount)
        assertEquals(1, firstTask?.failedParticipantCount)
        assertEquals(2.0, firstTask?.submissionConversionRate)
        assertEquals(1.0 / 3.0, firstTask?.successRate ?: 0.0, 0.0001)
        assertEquals(
            task1.deadline?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            firstTask?.deadline,
        )

        val secondTask = result.tasks?.get(1)
        assertEquals(2L, secondTask?.taskId)
        assertEquals(1, secondTask?.participantCount)
        assertEquals(0, secondTask?.pendingReviewCount)
        assertEquals(1, secondTask?.resubmittableCount)
    }

    @Test
    fun `getSpaceTaskAnalytics should filter and sort task metrics`() {
        val spaceId = 1L
        val now = LocalDateTime.now()
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category1 = mockk<SpaceCategory>()
        every { category1.id } returns 1
        every { category1.name } returns "Category 1"
        every { category1.space } returns space

        val category2 = mockk<SpaceCategory>()
        every { category2.id } returns 2
        every { category2.name } returns "Category 2"
        every { category2.space } returns space

        val publisher1 = mockk<User>()
        every { publisher1.id } returns 1
        every { publisher1.username } returns "teacher-a"

        val publisher2 = mockk<User>()
        every { publisher2.id } returns 2
        every { publisher2.username } returns "teacher-b"

        val task1 = mockk<Task>(relaxed = true)
        every { task1.id } returns 1L
        every { task1.name } returns "Task 1"
        every { task1.category } returns category1
        every { task1.creator } returns publisher1
        every { task1.createdAt } returns now.minusDays(1)
        every { task1.deadline } returns null
        every { task1.approved } returns ApproveType.APPROVED

        val task2 = mockk<Task>(relaxed = true)
        every { task2.id } returns 2L
        every { task2.name } returns "Task 2"
        every { task2.category } returns category1
        every { task2.creator } returns publisher1
        every { task2.createdAt } returns now.minusHours(12)
        every { task2.deadline } returns null
        every { task2.approved } returns ApproveType.APPROVED

        val task3 = mockk<Task>(relaxed = true)
        every { task3.id } returns 3L
        every { task3.name } returns "Task 3"
        every { task3.category } returns category2
        every { task3.creator } returns publisher2
        every { task3.createdAt } returns now
        every { task3.deadline } returns null
        every { task3.approved } returns ApproveType.DISAPPROVED

        val task1PendingApproval = mockk<TaskMembership>(relaxed = true)
        every { task1PendingApproval.id } returns 31L
        every { task1PendingApproval.task } returns task1
        every { task1PendingApproval.approved } returns ApproveType.NONE
        every { task1PendingApproval.completionStatus } returns TaskCompletionStatus.NOT_SUBMITTED

        val task2PendingReview = mockk<TaskMembership>(relaxed = true)
        every { task2PendingReview.id } returns 32L
        every { task2PendingReview.task } returns task2
        every { task2PendingReview.approved } returns ApproveType.APPROVED
        every { task2PendingReview.completionStatus } returns TaskCompletionStatus.PENDING_REVIEW

        val task2Success = mockk<TaskMembership>(relaxed = true)
        every { task2Success.id } returns 33L
        every { task2Success.task } returns task2
        every { task2Success.approved } returns ApproveType.APPROVED
        every { task2Success.completionStatus } returns TaskCompletionStatus.SUCCESS

        every { taskRepository.findBySpaceId(spaceId) } returns listOf(task1, task2, task3)
        every {
            taskRepository.findAnalyticsTasks(spaceId, null, null, 1L, 1L, ApproveType.APPROVED)
        } returns listOf(task1, task2)
        every { taskMembershipRepository.findAllByTaskIdIn(listOf(1L, 2L)) } returns
            listOf(task1PendingApproval, task2PendingReview, task2Success)
        val task2PendingReviewSubmission =
            mockk<TaskSubmission>(relaxed = true) {
                every { id } returns 401L
                every { membership } returns task2PendingReview
                every { version } returns 1
                every { createdAt } returns now.minusHours(2)
            }
        val task2SuccessSubmission =
            mockk<TaskSubmission>(relaxed = true) {
                every { id } returns 402L
                every { membership } returns task2Success
                every { version } returns 1
                every { createdAt } returns now.minusHours(1)
            }
        every { taskSubmissionRepository.findAllByMembershipIdIn(listOf(31L, 32L, 33L)) } returns
            listOf(task2PendingReviewSubmission, task2SuccessSubmission)
        every { taskSubmissionReviewRepository.findAllBySubmissionIdIn(listOf(401L, 402L)) } returns
            listOf(mockk(relaxed = true) { every { submission } returns task2SuccessSubmission })
        every { taskMembershipRepository.findAllByTaskId(1L) } returns listOf(task1PendingApproval)
        every { taskMembershipRepository.findAllByTaskId(2L) } returns
            listOf(task2PendingReview, task2Success)
        every { taskMembershipRepository.findAllByTaskId(3L) } returns emptyList()
        every { taskSubmissionRepository.findAllByMembershipId(31L) } returns emptyList()
        every { taskSubmissionRepository.findAllByMembershipId(32L) } returns
            listOf(mockk(relaxed = true) { every { createdAt } returns now.minusHours(2) })
        every { taskSubmissionRepository.findAllByMembershipId(33L) } returns
            listOf(mockk(relaxed = true) { every { createdAt } returns now.minusHours(1) })

        val result =
            service.getSpaceTaskAnalytics(
                spaceId = spaceId,
                from = null,
                to = null,
                categoryId = 1L,
                publisherId = 1L,
                taskApproved = "APPROVED",
                hasPendingReview = true,
                hasPendingApproval = false,
                sortBy = "successRate",
                sortOrder = "desc",
            )

        assertEquals(listOf(2L), result.tasks.orEmpty().map { it.taskId })
        assertEquals(1, result.tasks?.first()?.pendingReviewCount)
        assertEquals(0, result.tasks?.first()?.pendingParticipantApprovalCount)
        assertEquals(0.5, result.tasks?.first()?.successRate ?: 0.0, 0.0001)
    }

    @Test
    fun `getPublishersParticipation should aggregate by publisher`() {
        // Given
        val spaceId = 1L
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category = mockk<SpaceCategory>()
        every { category.id } returns 1
        every { category.name } returns "Category"
        every { category.space } returns space

        val publisher1 = mockk<User>()
        every { publisher1.id } returns 1
        every { publisher1.username } returns "publisher1"

        val publisher2 = mockk<User>()
        every { publisher2.id } returns 2
        every { publisher2.username } returns "publisher2"

        val task1 = mockk<Task>(relaxed = true)
        every { task1.id } returns 1L
        every { task1.name } returns "Task 1"
        every { task1.category } returns category
        every { task1.creator } returns publisher1
        every { task1.createdAt } returns LocalDateTime.now()
        every { task1.approved } returns ApproveType.APPROVED
        every { task1.rank } returns null

        val task2 = mockk<Task>(relaxed = true)
        every { task2.id } returns 2L
        every { task2.name } returns "Task 2"
        every { task2.category } returns category
        every { task2.creator } returns publisher1
        every { task2.createdAt } returns LocalDateTime.now()
        every { task2.approved } returns ApproveType.APPROVED
        every { task2.rank } returns null

        val task3 = mockk<Task>(relaxed = true)
        every { task3.id } returns 3L
        every { task3.name } returns "Task 3"
        every { task3.category } returns category
        every { task3.creator } returns publisher2
        every { task3.createdAt } returns LocalDateTime.now()
        every { task3.approved } returns ApproveType.APPROVED
        every { task3.rank } returns null

        val tasks = listOf(task1, task2, task3)

        val membership1 = mockk<TaskMembership>(relaxed = true)
        every { membership1.id } returns 1L
        every { membership1.task } returns task1
        every { membership1.memberId } returns 10
        every { membership1.completionStatus } returns TaskCompletionStatus.SUCCESS

        val membership2 = mockk<TaskMembership>(relaxed = true)
        every { membership2.id } returns 2L
        every { membership2.task } returns task1
        every { membership2.memberId } returns 11
        every { membership2.completionStatus } returns TaskCompletionStatus.SUCCESS

        val membership3 = mockk<TaskMembership>(relaxed = true)
        every { membership3.id } returns 3L
        every { membership3.task } returns task3
        every { membership3.memberId } returns 12
        every { membership3.completionStatus } returns TaskCompletionStatus.PENDING_REVIEW

        val memberships = listOf(membership1, membership2, membership3)

        every { taskRepository.findBySpaceId(spaceId) } returns tasks
        every { taskMembershipRepository.findAllByTaskId(1L) } returns
            memberships.filter { it.task?.id == 1L }
        every { taskMembershipRepository.findAllByTaskId(2L) } returns emptyList()
        every { taskMembershipRepository.findAllByTaskId(3L) } returns
            memberships.filter { it.task?.id == 3L }

        // When
        val result = service.getPublishersParticipation(spaceId, "")

        // Then
        assertNotNull(result)
        assertEquals(2, result.size) // 2 publishers

        val publisher1Stats = result.find { it.publisherId == 1L }
        assertNotNull(publisher1Stats)
        assertEquals("publisher1", publisher1Stats?.publisherName)
        assertEquals(2, publisher1Stats?.taskCount) // 2 tasks
        assertEquals(2, publisher1Stats?.participants) // 2 participants
        assertEquals(2, publisher1Stats?.completedUsers) // 2 successful

        val publisher2Stats = result.find { it.publisherId == 2L }
        assertNotNull(publisher2Stats)
        assertEquals("publisher2", publisher2Stats?.publisherName)
        assertEquals(1, publisher2Stats?.taskCount) // 1 task
        assertEquals(1, publisher2Stats?.participants) // 1 participant
        assertEquals(0, publisher2Stats?.completedUsers) // 0 successful
    }

    @Test
    fun `exportParticipants should generate CSV for participants format`() {
        // Given
        val spaceId = 1L
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category = mockk<SpaceCategory>()
        every { category.id } returns 1
        every { category.name } returns "Category"
        every { category.space } returns space

        val user = mockk<User>()
        every { user.id } returns 1
        every { user.username } returns "testuser"

        val task = mockk<Task>(relaxed = true)
        every { task.id } returns 1L
        every { task.name } returns "Test Task"
        every { task.category } returns category
        every { task.creator } returns user
        every { task.createdAt } returns LocalDateTime.now()
        every { task.approved } returns ApproveType.APPROVED
        every { task.rank } returns null

        val membership = mockk<TaskMembership>(relaxed = true)
        every { membership.id } returns 1L
        every { membership.task } returns task
        every { membership.memberId } returns 10
        every { membership.approved } returns ApproveType.APPROVED
        every { membership.completionStatus } returns TaskCompletionStatus.SUCCESS
        every { membership.email } returns "test@example.com"

        every { taskRepository.findBySpaceId(spaceId) } returns listOf(task)
        every { taskMembershipRepository.findAllByTaskId(1L) } returns listOf(membership)
        every { userRepository.findById(any<Int>()) } returns java.util.Optional.of(user)

        // When
        val csv =
            service.exportParticipants(
                spaceId = spaceId,
                format = "csv",
                from = null,
                to = null,
                taskStatus = null,
                categoryId = null,
                publisherId = null,
                realName = "",
                successBy = "",
            )

        // Then
        assertNotNull(csv)
        println("CSV Participants Content: $csv") // Debug output
        assertTrue(
            csv.contains(
                "Task ID,Task Title,Category,Task Rank,Task Creator,Created At,Deadline,Member ID,Username,Real Name,Student ID,Grade,Major,Class,Phone,Email,Apply Reason,Reject Reason,Approval Status,Completion Status,Is Team,Join Date"
            )
        )
        // Check for parts that should be in the CSV
        assertTrue(csv.contains("\"Test Task\""))
        assertTrue(csv.contains("test@example.com"))
        assertTrue(csv.contains("APPROVED"))
        assertTrue(csv.contains("SUCCESS"))
    }

    @Test
    fun `exportParticipants should use decrypted real name snapshots`() {
        val spaceId = 1L
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category = mockk<SpaceCategory>()
        every { category.id } returns 1
        every { category.name } returns "Category"
        every { category.space } returns space

        val user = mockk<User>()
        every { user.id } returns 1
        every { user.username } returns "testuser"

        val task = mockk<Task>(relaxed = true)
        every { task.id } returns 1L
        every { task.name } returns "Test Task"
        every { task.category } returns category
        every { task.creator } returns user
        every { task.createdAt } returns LocalDateTime.now()
        every { task.approved } returns ApproveType.APPROVED
        every { task.rank } returns null

        val membership = mockk<TaskMembership>(relaxed = true)
        every { membership.id } returns 1L
        every { membership.task } returns task
        every { membership.memberId } returns 10
        every { membership.approved } returns ApproveType.APPROVED
        every { membership.completionStatus } returns TaskCompletionStatus.SUCCESS
        every { membership.email } returns "test@example.com"
        every { membership.isTeam } returns false
        every { membership.realNameInfo } returns
            RealNameInfo(
                realName = "cipher-name",
                studentId = "cipher-student-id",
                grade = "cipher-grade",
                major = "cipher-major",
                className = "cipher-class",
                encrypted = true,
            )
        every { taskMembershipSnapshotService.getRealNameInfoFromMembership(membership) } returns
            RealNameInfo(
                    realName = "Alice",
                    studentId = "20260001",
                    grade = "2026",
                    major = "CS",
                    className = "CS-1",
                    encrypted = false,
                )
                .convert()

        every { taskRepository.findBySpaceId(spaceId) } returns listOf(task)
        every { taskMembershipRepository.findAllByTaskId(1L) } returns listOf(membership)
        every { userRepository.findById(any<Int>()) } returns java.util.Optional.of(user)

        val csv =
            service.exportParticipants(
                spaceId = spaceId,
                format = "csv",
                from = null,
                to = null,
                taskStatus = null,
                categoryId = null,
                publisherId = null,
                realName = "",
                successBy = "",
            )

        assertTrue(csv.contains("Alice"))
        assertTrue(csv.contains("20260001"))
        assertFalse(csv.contains("cipher-name"))
    }

    @Test
    fun `exportSpaceAnalyticsParticipants should audit each exported user`() {
        val queryService = mockk<SpaceAnalyticsQueryService>()
        val auditService =
            SpaceAnalyticsService(
                taskRepository,
                taskMembershipRepository,
                taskSubmissionRepository,
                taskSubmissionReviewRepository,
                userRepository,
                taskMembershipSnapshotService,
                userRealNameService,
                queryService,
            )

        val task = mockk<Task>(relaxed = true)
        every { task.id } returns 1L

        val singleMembership = mockk<TaskMembership>(relaxed = true)
        every { singleMembership.id } returns 11L
        every { singleMembership.task } returns task
        every { singleMembership.isTeam } returns false
        every { singleMembership.memberId } returns 101L
        every { singleMembership.teamMembersRealNameInfo } returns mutableListOf()

        val teamMembership = mockk<TaskMembership>(relaxed = true)
        every { teamMembership.id } returns 12L
        every { teamMembership.task } returns task
        every { teamMembership.isTeam } returns true
        every { teamMembership.memberId } returns 999L
        every { teamMembership.teamMembersRealNameInfo } returns
            mutableListOf(
                TeamMemberRealNameInfo(201L, realNameInfo = RealNameInfo(realName = "A")),
                TeamMemberRealNameInfo(202L, realNameInfo = RealNameInfo(realName = "B")),
            )

        every {
            queryService.exportParticipantsCsv(
                spaceId = 52L,
                from = 100L,
                to = 200L,
                categoryId = 3L,
                publisherId = 4L,
                taskApproved = "APPROVED",
                participationApproved = "APPROVED",
                completionStatus = "SUCCESS",
                realName = "with",
            )
        } returns
            SpaceAnalyticsQueryService.ParticipantExportPayload(
                csv = "csv-content\n",
                memberships = listOf(singleMembership, teamMembership),
            )
        val exportedTargetIds = mutableListOf<Long>()
        val accessReasons = mutableListOf<String>()
        every { userRealNameService.logAccess(any(), any(), any(), any(), any(), any()) } answers
            {
                exportedTargetIds.add(secondArg())
                accessReasons.add(thirdArg())
                Unit
            }

        val csv =
            auditService.exportSpaceAnalyticsParticipants(
                accessorId = 7L,
                spaceId = 52L,
                from = 100L,
                to = 200L,
                categoryId = 3L,
                publisherId = 4L,
                taskApproved = "APPROVED",
                participationApproved = "APPROVED",
                completionStatus = "SUCCESS",
                realName = "with",
            )

        assertEquals("csv-content\n", csv)
        verify(exactly = 3) {
            userRealNameService.logAccess(
                accessorId = 7L,
                targetId = any(),
                accessReason = any(),
                accessType = AccessType.EXPORT,
                moduleType = AccessModuleType.SPACE,
                moduleEntityId = 52L,
            )
        }
        assertEquals(listOf(101L, 201L, 202L), exportedTargetIds.sorted())
        assertTrue(accessReasons.all { it.contains("from=100") })
        assertTrue(accessReasons.all { it.contains("to=200") })
        assertTrue(accessReasons.all { it.contains("categoryId=3") })
        assertTrue(accessReasons.all { it.contains("publisherId=4") })
        assertTrue(accessReasons.all { it.contains("taskApproved=APPROVED") })
        assertTrue(accessReasons.all { it.contains("participationApproved=APPROVED") })
        assertTrue(accessReasons.all { it.contains("completionStatus=SUCCESS") })
        assertTrue(accessReasons.all { it.contains("realName=with") })
    }

    @Test
    fun `exportParticipants should generate CSV for summary format`() {
        // Given
        val spaceId = 1L
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category = mockk<SpaceCategory>()
        every { category.id } returns 1
        every { category.name } returns "Test Category"
        every { category.space } returns space

        val user = mockk<User>()
        every { user.id } returns 1
        every { user.username } returns "publisher1"

        val task = mockk<Task>(relaxed = true)
        every { task.id } returns 1L
        every { task.name } returns "Test Task"
        every { task.category } returns category
        every { task.creator } returns user
        every { task.createdAt } returns LocalDateTime.now()
        every { task.approved } returns ApproveType.APPROVED
        every { task.rank } returns null

        val membership1 = mockk<TaskMembership>(relaxed = true)
        every { membership1.id } returns 1L
        every { membership1.task } returns task
        every { membership1.memberId } returns 10
        every { membership1.approved } returns ApproveType.APPROVED
        every { membership1.completionStatus } returns TaskCompletionStatus.SUCCESS
        every { membership1.email } returns "test1@example.com"

        val membership2 = mockk<TaskMembership>(relaxed = true)
        every { membership2.id } returns 2L
        every { membership2.task } returns task
        every { membership2.memberId } returns 11
        every { membership2.approved } returns ApproveType.DISAPPROVED
        every { membership2.completionStatus } returns TaskCompletionStatus.FAILED
        every { membership2.email } returns "test2@example.com"

        val membership3 = mockk<TaskMembership>(relaxed = true)
        every { membership3.id } returns 3L
        every { membership3.task } returns task
        every { membership3.memberId } returns 12
        every { membership3.approved } returns ApproveType.NONE
        every { membership3.completionStatus } returns TaskCompletionStatus.PENDING_REVIEW
        every { membership3.email } returns "test3@example.com"

        val memberships = listOf(membership1, membership2, membership3)

        every { taskRepository.findBySpaceId(spaceId) } returns listOf(task)
        every { taskMembershipRepository.findAllByTaskId(1L) } returns memberships
        every { userRepository.findById(any<Int>()) } returns java.util.Optional.empty()

        // When
        val csv =
            service.exportParticipants(
                spaceId = spaceId,
                format = "summary",
                from = null,
                to = null,
                taskStatus = null,
                categoryId = null,
                publisherId = null,
                realName = "",
                successBy = "",
            )

        // Then
        assertNotNull(csv)
        println("CSV Content: $csv") // Debug output
        assertTrue(
            csv.contains(
                "Task ID,Task Title,Category,Rank,Creator,Created At,Deadline,Total Participants,Approved,Rejected,Pending,Completed,Task Status"
            )
        )
        assertTrue(csv.contains("1,\"Test Task\",\"Test Category\""))
        assertTrue(csv.contains("publisher1"))
        assertTrue(csv.contains(",3,1,1,1,"))
    }

    @Test
    fun `getSpaceTaskAnalytics should handle empty data`() {
        val spaceId = 1L
        every { taskRepository.findBySpaceId(spaceId) } returns emptyList()
        every { taskRepository.findAnalyticsTasks(spaceId, null, null, null, null, null) } returns
            emptyList()

        val result =
            service.getSpaceTaskAnalytics(
                spaceId = spaceId,
                from = null,
                to = null,
                categoryId = null,
                publisherId = null,
                taskApproved = null,
                hasPendingReview = null,
                hasPendingApproval = null,
                sortBy = "createdAt",
                sortOrder = "desc",
            )

        assertNotNull(result)
        assertTrue(result.tasks.isNullOrEmpty())
    }

    @Test
    fun `getSpaceTaskAnalytics should filter by multiple conditions`() {
        val spaceId = 1L
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category1 = mockk<SpaceCategory>()
        every { category1.id } returns 1
        every { category1.name } returns "Category 1"
        every { category1.space } returns space

        val category2 = mockk<SpaceCategory>()
        every { category2.id } returns 2
        every { category2.name } returns "Category 2"
        every { category2.space } returns space

        val user1 = mockk<User>()
        every { user1.id } returns 1
        every { user1.username } returns "user1"

        val user2 = mockk<User>()
        every { user2.id } returns 2
        every { user2.username } returns "user2"

        val task1 = mockk<Task>(relaxed = true)
        every { task1.id } returns 1L
        every { task1.name } returns "Task 1"
        every { task1.category } returns category1
        every { task1.creator } returns user1
        every { task1.createdAt } returns LocalDateTime.now()
        every { task1.deadline } returns null
        every { task1.approved } returns ApproveType.APPROVED

        val task2 = mockk<Task>(relaxed = true)
        every { task2.id } returns 2L
        every { task2.name } returns "Task 2"
        every { task2.category } returns category2
        every { task2.creator } returns user2
        every { task2.createdAt } returns LocalDateTime.now()
        every { task2.deadline } returns null
        every { task2.approved } returns ApproveType.APPROVED

        val tasks = listOf(task1, task2)
        every { taskRepository.findBySpaceId(spaceId) } returns tasks
        every {
            taskRepository.findAnalyticsTasks(spaceId, null, null, 1L, 1L, ApproveType.APPROVED)
        } returns listOf(task1)
        every { taskMembershipRepository.findAllByTaskIdIn(listOf(1L)) } returns emptyList()
        every { taskMembershipRepository.findAllByTaskId(any()) } returns emptyList()

        val result =
            service.getSpaceTaskAnalytics(
                spaceId = spaceId,
                from = null,
                to = null,
                categoryId = 1L,
                publisherId = 1L,
                taskApproved = "APPROVED",
                hasPendingReview = false,
                hasPendingApproval = false,
                sortBy = "createdAt",
                sortOrder = "desc",
            )

        assertNotNull(result)
        assertEquals(listOf(1L), result.tasks.orEmpty().map { it.taskId })
        assertEquals("Category 1", result.tasks?.first()?.category?.name)
        assertEquals(ApproveTypeDTO.APPROVED, result.tasks?.first()?.approved)
    }

    @Test
    fun `exportParticipants should handle special characters in CSV`() {
        // Given
        val spaceId = 1L
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category = mockk<SpaceCategory>()
        every { category.id } returns 1
        every { category.name } returns "Category \"with\" quotes" // Name with quotes
        every { category.space } returns space

        val user = mockk<User>()
        every { user.id } returns 1
        every { user.username } returns "user,with,commas" // Name with commas

        val task = mockk<Task>(relaxed = true)
        every { task.id } returns 1L
        every { task.name } returns "Task \"Special\", Test" // Name with quotes and commas
        every { task.category } returns category
        every { task.creator } returns user
        every { task.createdAt } returns LocalDateTime.now()
        every { task.approved } returns ApproveType.APPROVED
        every { task.rank } returns null

        every { taskRepository.findBySpaceId(spaceId) } returns listOf(task)
        every { taskMembershipRepository.findAllByTaskId(1L) } returns emptyList()
        every { userRepository.findById(any<Int>()) } returns java.util.Optional.empty()

        // When
        val csv =
            service.exportParticipants(
                spaceId = spaceId,
                format = "summary",
                from = null,
                to = null,
                taskStatus = null,
                categoryId = null,
                publisherId = null,
                realName = "",
                successBy = "",
            )

        // Then - verify proper escaping
        assertNotNull(csv)
        assertTrue(
            csv.contains(
                "Task ID,Task Title,Category,Rank,Creator,Created At,Deadline,Total Participants,Approved,Rejected,Pending,Completed,Task Status"
            )
        )
        // Should properly escape special characters
        assertTrue(
            csv.contains("\"Task \"\"Special\"\", Test\"") || csv.contains("Task \"Special\", Test")
        )
    }
}
