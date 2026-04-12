package org.rucca.cheese.task.service

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rucca.cheese.notification.event.NotificationTriggerEvent
import org.rucca.cheese.notification.models.NotificationType
import org.rucca.cheese.space.SpaceService
import org.rucca.cheese.team.TeamService
import org.springframework.context.ApplicationEventPublisher

@ExtendWith(MockKExtension::class)
class TaskNotificationServiceTest {
    @RelaxedMockK private lateinit var eventPublisher: ApplicationEventPublisher

    @MockK private lateinit var spaceService: SpaceService

    @MockK private lateinit var teamService: TeamService

    private lateinit var taskNotificationService: TaskNotificationService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        taskNotificationService =
            TaskNotificationService(
                eventPublisher = eventPublisher,
                spaceService = spaceService,
                teamService = teamService,
            )
    }

    @Test
    fun `publishToSpaceOwners should publish notification event for space owner and admins`() {
        every { spaceService.getSpaceAdminAndOwnerIds(42L) } returns setOf(1L, 2L)

        val payload =
            taskNotificationService.buildTaskPayload(
                taskId = 100L,
                taskName = "Task A",
                spaceId = 42L,
                spaceName = "Space A",
                actorId = 9L,
                actorName = "Alice",
            )

        val eventSlot = slot<Any>()
        taskNotificationService.publishToSpaceOwners(
            spaceId = 42L,
            type = NotificationType.TASK_APPROVED,
            payload = payload,
            actorId = 9L,
        )

        verify { eventPublisher.publishEvent(capture(eventSlot)) }
        val event = eventSlot.captured as NotificationTriggerEvent

        assertThat(event.recipientIds).containsExactlyInAnyOrder(1L, 2L)
        assertThat(event.type).isEqualTo(NotificationType.TASK_APPROVED)
        assertThat(event.actorId).isEqualTo(9L)
        assertThat(event.payload["taskId"]).isEqualTo(100L)
        assertThat(event.payload["taskName"]).isEqualTo("Task A")
        assertThat(event.payload["spaceId"]).isEqualTo(42L)
        assertThat(event.payload["spaceName"]).isEqualTo("Space A")
        assertThat(event.payload["task"]).isEqualTo(mapOf("type" to "task", "id" to "100"))
        assertThat(event.payload["space"]).isEqualTo(mapOf("type" to "space", "id" to "42"))
        assertThat(event.payload["actor"]).isEqualTo(mapOf("type" to "user", "id" to "9"))
    }

    @Test
    fun `buildMembershipPayload should include participant and membership references`() {
        val payload =
            taskNotificationService.buildMembershipPayload(
                taskId = 100L,
                taskName = "Task A",
                spaceId = 42L,
                spaceName = "Space A",
                membershipId = 500L,
                participantId = 88L,
                participantName = "Team A",
                participantType = "team",
                teamId = 88L,
                teamName = "Team A",
                actorId = 9L,
                actorName = "Alice",
            )

        assertThat(payload["membership"])
            .isEqualTo(mapOf("type" to "task_membership", "id" to "500"))
        assertThat(payload["participant"]).isEqualTo(mapOf("type" to "team", "id" to "88"))
        assertThat(payload["team"]).isEqualTo(mapOf("type" to "team", "id" to "88"))
        assertThat(payload["participantType"]).isEqualTo("team")
        assertThat(payload["teamName"]).isEqualTo("Team A")
    }
}
