package org.rucca.cheese.space.analytics

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.rucca.cheese.common.error.BadRequestError
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.space.models.Space
import org.rucca.cheese.space.models.SpaceCategory
import org.rucca.cheese.task.RealNameInfo
import org.rucca.cheese.task.Task
import org.rucca.cheese.task.TaskCompletionStatus
import org.rucca.cheese.task.TaskMembership
import org.rucca.cheese.task.TaskMembershipRepository
import org.rucca.cheese.task.TaskRepository
import org.rucca.cheese.task.TaskSubmission
import org.rucca.cheese.task.TaskSubmissionRepository
import org.rucca.cheese.task.TaskSubmissionReviewRepository
import org.rucca.cheese.task.TeamMemberRealNameInfo
import org.rucca.cheese.task.convert
import org.rucca.cheese.task.service.TaskMembershipSnapshotService
import org.rucca.cheese.user.User

@ExtendWith(MockKExtension::class)
class SpaceAnalyticsQueryServiceTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var taskMembershipRepository: TaskMembershipRepository
    private lateinit var taskSubmissionRepository: TaskSubmissionRepository
    private lateinit var taskSubmissionReviewRepository: TaskSubmissionReviewRepository
    private lateinit var taskMembershipSnapshotService: TaskMembershipSnapshotService
    private lateinit var queryService: SpaceAnalyticsQueryService

    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        taskMembershipRepository = mockk()
        taskSubmissionRepository = mockk()
        taskSubmissionReviewRepository = mockk()
        taskMembershipSnapshotService = mockk()
        every { taskMembershipSnapshotService.getRealNameInfoFromMembership(any()) } answers
            {
                firstArg<TaskMembership>().realNameInfo?.convert()
            }
        every {
            taskMembershipSnapshotService.getRealNameInfoForTeamMemberSnapshot(any(), any())
        } answers { secondArg<TeamMemberRealNameInfo>().realNameInfo.convert() }
        queryService =
            SpaceAnalyticsQueryService(
                taskRepository = taskRepository,
                taskMembershipRepository = taskMembershipRepository,
                taskSubmissionRepository = taskSubmissionRepository,
                taskSubmissionReviewRepository = taskSubmissionReviewRepository,
                taskMembershipSnapshotService = taskMembershipSnapshotService,
            )
    }

    @Test
    fun `getOverview should bulk load memberships and submissions`() {
        val spaceId = 1L
        val now = LocalDateTime.now()
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category = mockk<SpaceCategory>()
        every { category.id } returns 101
        every { category.name } returns "Research"
        every { category.space } returns space

        val publisher = mockk<User>()
        every { publisher.id } returns 201
        every { publisher.username } returns "teacher-a"

        val task1 = mockk<Task>(relaxed = true)
        every { task1.id } returns 1L
        every { task1.name } returns "Task 1"
        every { task1.creator } returns publisher
        every { task1.category } returns category
        every { task1.createdAt } returns now.minusDays(1)
        every { task1.approved } returns ApproveType.APPROVED

        val task2 = mockk<Task>(relaxed = true)
        every { task2.id } returns 2L
        every { task2.name } returns "Task 2"
        every { task2.creator } returns publisher
        every { task2.category } returns category
        every { task2.createdAt } returns now
        every { task2.approved } returns ApproveType.APPROVED

        val membership1 = mockk<TaskMembership>(relaxed = true)
        every { membership1.id } returns 11L
        every { membership1.task } returns task1
        every { membership1.isTeam } returns false
        every { membership1.approved } returns ApproveType.APPROVED
        every { membership1.completionStatus } returns TaskCompletionStatus.SUCCESS
        every { membership1.teamMembersRealNameInfo } returns mutableListOf()
        every { membership1.createdAt } returns now.minusHours(6)
        every { membership1.updatedAt } returns now.minusHours(1)

        val membership2 = mockk<TaskMembership>(relaxed = true)
        every { membership2.id } returns 12L
        every { membership2.task } returns task2
        every { membership2.isTeam } returns true
        every { membership2.approved } returns ApproveType.APPROVED
        every { membership2.completionStatus } returns TaskCompletionStatus.PENDING_REVIEW
        every { membership2.teamMembersRealNameInfo } returns
            mutableListOf(
                TeamMemberRealNameInfo(301L, realNameInfo = RealNameInfo(realName = "A")),
                TeamMemberRealNameInfo(302L, realNameInfo = RealNameInfo(realName = "B")),
            )
        every { membership2.createdAt } returns now.minusHours(5)
        every { membership2.updatedAt } returns now.minusHours(2)

        val submission1 = mockk<TaskSubmission>(relaxed = true)
        every { submission1.membership } returns membership1
        every { submission1.createdAt } returns now.minusHours(3)

        every { taskRepository.findAnalyticsTasks(spaceId, null, null, null, null, null) } returns
            listOf(task1, task2)
        every { taskMembershipRepository.findAllByTaskIdIn(listOf(1L, 2L)) } returns
            listOf(membership1, membership2)
        every { taskSubmissionRepository.findAllByMembershipIdIn(listOf(11L, 12L)) } returns
            listOf(submission1)
        every { taskSubmissionReviewRepository.findAllBySubmissionIdIn(any()) } returns emptyList()

        val result =
            queryService.getOverview(
                spaceId = spaceId,
                from = null,
                to = null,
                categoryId = null,
                publisherId = null,
                taskApproved = null,
                groupBy = "day",
            )

        assertEquals(2, result.entityMetrics.taskCount)
        assertEquals(2, result.entityMetrics.participantCount)
        assertEquals(1, result.entityMetrics.submittedParticipantCount)
        assertEquals(3, result.studentMetrics.studentCount)

        verify(exactly = 1) {
            taskRepository.findAnalyticsTasks(spaceId, null, null, null, null, null)
        }
        verify(exactly = 1) { taskMembershipRepository.findAllByTaskIdIn(listOf(1L, 2L)) }
        verify(exactly = 1) { taskSubmissionRepository.findAllByMembershipIdIn(listOf(11L, 12L)) }
        verify(exactly = 0) { taskMembershipRepository.findAllByTaskId(any()) }
        verify(exactly = 0) { taskSubmissionRepository.findAllByMembershipId(any()) }
    }

    @Test
    fun `participant analytics export should use decrypted real name snapshots`() {
        val spaceId = 1L
        val now = LocalDateTime.now()
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category = mockk<SpaceCategory>()
        every { category.id } returns 101
        every { category.name } returns "Research"
        every { category.space } returns space

        val publisher = mockk<User>()
        every { publisher.id } returns 201
        every { publisher.username } returns "teacher-a"

        val task = mockk<Task>(relaxed = true)
        every { task.id } returns 1L
        every { task.name } returns "Task 1"
        every { task.creator } returns publisher
        every { task.category } returns category
        every { task.createdAt } returns now.minusDays(1)
        every { task.approved } returns ApproveType.APPROVED

        val membership = mockk<TaskMembership>(relaxed = true)
        every { membership.id } returns 11L
        every { membership.task } returns task
        every { membership.isTeam } returns false
        every { membership.memberId } returns 301L
        every { membership.approved } returns ApproveType.APPROVED
        every { membership.completionStatus } returns TaskCompletionStatus.SUCCESS
        every { membership.createdAt } returns now.minusHours(6)
        every { membership.updatedAt } returns now.minusHours(1)
        every { membership.realNameInfo } returns
            RealNameInfo(
                realName = "qlb3tfP/n5aQX3KkQ2ZD6Q==",
                studentId = "D6HzNrxCEyUGihv4I9y7og==",
                grade = "NBGLsUojrFAyWmwtq5oENQ==",
                major = "0Cuz9jqedN1v2JXqlJqYCw==",
                className = "cipher-class",
                encrypted = true,
            )
        every { membership.teamMembersRealNameInfo } returns mutableListOf()
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

        every { taskRepository.findAnalyticsTasks(spaceId, null, null, null, null, null) } returns
            listOf(task)
        every { taskMembershipRepository.findAllByTaskIdIn(listOf(1L)) } returns listOf(membership)
        every { taskSubmissionRepository.findAllByMembershipIdIn(listOf(11L)) } returns emptyList()

        val analytics =
            queryService.getParticipants(
                spaceId = spaceId,
                from = null,
                to = null,
                categoryId = null,
                publisherId = null,
                taskApproved = null,
                participationApproved = null,
                completionStatus = null,
                realName = "",
                groupBy = "day",
            )
        val exportPayload =
            queryService.exportParticipantsCsv(
                spaceId = spaceId,
                from = null,
                to = null,
                categoryId = null,
                publisherId = null,
                taskApproved = null,
                participationApproved = null,
                completionStatus = null,
                realName = "",
            )

        assertEquals(1, analytics.studentMetrics.studentsWithRealNameCount)
        assertEquals("2026", analytics.distributions.byGrade!!.items!!.first().label)
        assertEquals("CS", analytics.distributions.byMajor!!.items!!.first().label)
        assertEquals("CS-1", analytics.distributions.byClassName!!.items!!.first().label)
        assertEquals(1, exportPayload.memberships.size)
        assertTrue(exportPayload.csv.contains("Alice"))
        assertTrue(exportPayload.csv.contains("20260001"))
        assertTrue(!exportPayload.csv.contains("qlb3tfP/n5aQX3KkQ2ZD6Q=="))

        verify(atLeast = 1) {
            taskMembershipSnapshotService.getRealNameInfoFromMembership(membership)
        }
    }

    @Test
    fun `getOverview should reject invalid taskApproved values`() {
        val error =
            assertThrows<BadRequestError> {
                queryService.getOverview(
                    spaceId = 1L,
                    from = null,
                    to = null,
                    categoryId = null,
                    publisherId = null,
                    taskApproved = "approved",
                    groupBy = "day",
                )
            }

        assertEquals("Invalid taskApproved: approved", error.message)
    }
}
