package org.rucca.cheese.space.view

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rucca.cheese.common.error.BadRequestError
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TaskCompletionStatusDTO
import org.rucca.cheese.model.TaskSubmitterTypeDTO
import org.rucca.cheese.model.TeamDTO
import org.rucca.cheese.space.models.Space
import org.rucca.cheese.space.models.SpaceCategory
import org.rucca.cheese.task.Task
import org.rucca.cheese.task.TaskCompletionStatus
import org.rucca.cheese.task.TaskMembership
import org.rucca.cheese.task.TaskMembershipRepository
import org.rucca.cheese.task.TaskSubmission
import org.rucca.cheese.task.TaskSubmissionRepository
import org.rucca.cheese.task.TaskSubmissionReview
import org.rucca.cheese.task.TaskSubmissionReviewRepository
import org.rucca.cheese.team.TeamService
import org.rucca.cheese.user.User

@ExtendWith(MockKExtension::class)
class SpaceParticipantViewServiceTest {
    private lateinit var taskMembershipRepository: TaskMembershipRepository
    private lateinit var taskSubmissionRepository: TaskSubmissionRepository
    private lateinit var taskSubmissionReviewRepository: TaskSubmissionReviewRepository
    private lateinit var teamService: TeamService
    private lateinit var service: SpaceParticipantViewService

    @BeforeEach
    fun setUp() {
        taskMembershipRepository = mockk()
        taskSubmissionRepository = mockk()
        taskSubmissionReviewRepository = mockk()
        teamService = mockk()
        service =
            SpaceParticipantViewService(
                taskMembershipRepository = taskMembershipRepository,
                taskSubmissionRepository = taskSubmissionRepository,
                taskSubmissionReviewRepository = taskSubmissionReviewRepository,
                teamService = teamService,
            )
    }

    @Test
    fun `getOverview should aggregate personal and team participation states`() {
        val currentUserId = 88L
        val team = createTeamSummary(501L, "Alpha Team")

        val successTask = createTask(101L, "Success", 1L, "Research")
        val pendingApprovalTask = createTask(102L, "Pending Approval", 1L, "Research")
        val awaitingTask = createTask(103L, "Awaiting", 2L, "Practice")
        val teamReviewTask = createTask(104L, "Team Review", 2L, "Practice")

        val successMembership =
            createMembership(
                id = 201L,
                task = successTask,
                memberId = currentUserId,
                approved = ApproveType.APPROVED,
                completionStatus = TaskCompletionStatus.SUCCESS,
                isTeam = false,
            )
        val pendingApprovalMembership =
            createMembership(
                id = 202L,
                task = pendingApprovalTask,
                memberId = currentUserId,
                approved = ApproveType.NONE,
                completionStatus = TaskCompletionStatus.NOT_SUBMITTED,
                isTeam = false,
            )
        val awaitingMembership =
            createMembership(
                id = 203L,
                task = awaitingTask,
                memberId = currentUserId,
                approved = ApproveType.APPROVED,
                completionStatus = TaskCompletionStatus.NOT_SUBMITTED,
                isTeam = false,
            )
        val teamReviewMembership =
            createMembership(
                id = 204L,
                task = teamReviewTask,
                memberId = team.id,
                approved = ApproveType.APPROVED,
                completionStatus = TaskCompletionStatus.PENDING_REVIEW,
                isTeam = true,
            )

        val successSubmission = createSubmission(301L, successMembership, version = 1, hour = 9)
        val teamSubmission = createSubmission(302L, teamReviewMembership, version = 1, hour = 11)
        val successReview = createReview(401L, successSubmission, accepted = true, score = 95)

        every { teamService.getTeamsOfUser(currentUserId) } returns listOf(team)
        every { teamService.isTeamAtLeastAdmin(501L, currentUserId) } returns true
        every {
            taskMembershipRepository.findAllByTaskSpaceIdAndMemberIdIn(
                52L,
                match { it.toSet() == setOf(currentUserId, team.id) },
            )
        } returns
            listOf(
                successMembership,
                pendingApprovalMembership,
                awaitingMembership,
                teamReviewMembership,
            )
        every {
            taskSubmissionRepository.findAllByMembershipIdIn(listOf(201L, 202L, 203L, 204L))
        } returns listOf(successSubmission, teamSubmission)
        every { taskSubmissionReviewRepository.findAllBySubmissionIdIn(listOf(301L, 302L)) } returns
            listOf(successReview)

        val result = service.getOverview(spaceId = 52L, currentUserId = currentUserId)

        assertEquals(52L, result.spaceId)
        assertEquals(4, result.participationCount)
        assertEquals(3, result.approvedParticipationCount)
        assertEquals(1, result.pendingApprovalCount)
        assertEquals(1, result.awaitingSubmissionCount)
        assertEquals(1, result.pendingReviewCount)
        assertEquals(0, result.resubmittableCount)
        assertEquals(1, result.successfulCount)
        assertEquals(0, result.failedCount)
    }

    @Test
    fun `getParticipations should support filters sorting and team fields`() {
        val currentUserId = 88L
        val team = createTeamSummary(501L, "Alpha Team")

        val successTask = createTask(101L, "Success", 1L, "Research")
        val teamTask = createTask(102L, "Team Review", 2L, "Practice")

        val successMembership =
            createMembership(
                id = 201L,
                task = successTask,
                memberId = currentUserId,
                approved = ApproveType.APPROVED,
                completionStatus = TaskCompletionStatus.SUCCESS,
                isTeam = false,
            )
        val teamMembership =
            createMembership(
                id = 202L,
                task = teamTask,
                memberId = team.id,
                approved = ApproveType.APPROVED,
                completionStatus = TaskCompletionStatus.PENDING_REVIEW,
                isTeam = true,
            )

        val successSubmission = createSubmission(301L, successMembership, version = 1, hour = 9)
        val teamSubmission = createSubmission(302L, teamMembership, version = 2, hour = 12)
        val successReview = createReview(401L, successSubmission, accepted = true, score = 88)

        every { teamService.getTeamsOfUser(currentUserId) } returns listOf(team)
        every { teamService.isTeamAtLeastAdmin(501L, currentUserId) } returns true
        every {
            taskMembershipRepository.findAllByTaskSpaceIdAndMemberIdIn(
                52L,
                match { it.toSet() == setOf(currentUserId, team.id) },
            )
        } returns listOf(successMembership, teamMembership)
        every { taskSubmissionRepository.findAllByMembershipIdIn(listOf(201L, 202L)) } returns
            listOf(successSubmission, teamSubmission)
        every { taskSubmissionReviewRepository.findAllBySubmissionIdIn(listOf(301L, 302L)) } returns
            listOf(successReview)

        val teamOnly =
            service.getParticipations(
                spaceId = 52L,
                currentUserId = currentUserId,
                approved = "APPROVED",
                completionStatus = TaskCompletionStatusDTO.PENDING_REVIEW,
                identityType = TaskSubmitterTypeDTO.TEAM,
                sortBy = "latestSubmissionAt",
                sortOrder = "desc",
            )

        assertEquals(1, teamOnly.participations.size)
        val participation = teamOnly.participations.first()
        assertEquals(102L, participation.taskId)
        assertEquals(TaskSubmitterTypeDTO.TEAM, participation.identityType)
        assertEquals("Alpha Team", participation.teamName)
        assertEquals(true, participation.canSubmit)
        assertEquals(TaskCompletionStatusDTO.PENDING_REVIEW, participation.completionStatus)
        assertEquals(null, participation.latestReviewAccepted)

        val allRows =
            service.getParticipations(
                spaceId = 52L,
                currentUserId = currentUserId,
                approved = null,
                completionStatus = null,
                identityType = null,
                sortBy = "latestSubmissionAt",
                sortOrder = "desc",
            )

        assertEquals(listOf(102L, 101L), allRows.participations.map { it.taskId })
        assertEquals(88.0, allRows.participations.last().latestReviewScore)
    }

    @Test
    fun `getParticipations should reject invalid approved filter`() {
        every { teamService.getTeamsOfUser(88L) } returns emptyList()
        every {
            taskMembershipRepository.findAllByTaskSpaceIdAndMemberIdIn(52L, listOf(88L))
        } returns emptyList()
        every { taskSubmissionRepository.findAllByMembershipIdIn(any()) } returns emptyList()
        every { taskSubmissionReviewRepository.findAllBySubmissionIdIn(any()) } returns emptyList()

        assertThrows(BadRequestError::class.java) {
            service.getParticipations(
                spaceId = 52L,
                currentUserId = 88L,
                approved = "WRONG",
                completionStatus = null,
                identityType = null,
                sortBy = "joinedAt",
                sortOrder = "desc",
            )
        }
    }

    private fun createTask(
        id: IdType,
        name: String,
        categoryId: IdType,
        categoryName: String,
    ): Task {
        val space = mockk<Space>(relaxed = true)
        every { space.id } returns 52L
        val category = mockk<SpaceCategory>(relaxed = true)
        every { category.id } returns categoryId
        every { category.name } returns categoryName
        every { category.space } returns space
        val creator = mockk<User>(relaxed = true)
        every { creator.id } returns 7
        every { creator.username } returns "teacher"
        return mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.name } returns name
            every { this@mockk.category } returns category
            every { this@mockk.creator } returns creator
            every { this@mockk.space } returns space
        }
    }

    private fun createMembership(
        id: IdType,
        task: Task,
        memberId: IdType,
        approved: ApproveType,
        completionStatus: TaskCompletionStatus,
        isTeam: Boolean,
    ): TaskMembership =
        mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.task } returns task
            every { this@mockk.memberId } returns memberId
            every { this@mockk.approved } returns approved
            every { this@mockk.completionStatus } returns completionStatus
            every { this@mockk.isTeam } returns isTeam
            every { this@mockk.createdAt } returns LocalDateTime.of(2026, 3, 1, id.toInt() % 24, 0)
            every { this@mockk.deadline } returns LocalDateTime.of(2026, 3, 10, id.toInt() % 24, 0)
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

    private fun createReview(
        id: IdType,
        submission: TaskSubmission,
        accepted: Boolean,
        score: Int,
    ): TaskSubmissionReview =
        mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.submission } returns submission
            every { this@mockk.accepted } returns accepted
            every { this@mockk.score } returns score
        }

    private fun createTeamSummary(id: IdType, name: String): TeamDTO =
        mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.name } returns name
        }
}
