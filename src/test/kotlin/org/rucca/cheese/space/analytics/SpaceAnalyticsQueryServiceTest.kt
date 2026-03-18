package org.rucca.cheese.space.analytics

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
import org.rucca.cheese.user.User

@ExtendWith(MockKExtension::class)
class SpaceAnalyticsQueryServiceTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var taskMembershipRepository: TaskMembershipRepository
    private lateinit var taskSubmissionRepository: TaskSubmissionRepository
    private lateinit var taskSubmissionReviewRepository: TaskSubmissionReviewRepository
    private lateinit var queryService: SpaceAnalyticsQueryService

    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        taskMembershipRepository = mockk()
        taskSubmissionRepository = mockk()
        taskSubmissionReviewRepository = mockk()
        queryService =
            SpaceAnalyticsQueryService(
                taskRepository = taskRepository,
                taskMembershipRepository = taskMembershipRepository,
                taskSubmissionRepository = taskSubmissionRepository,
                taskSubmissionReviewRepository = taskSubmissionReviewRepository,
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
}
