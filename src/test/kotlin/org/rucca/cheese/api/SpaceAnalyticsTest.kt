package org.rucca.cheese.api

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.task.TaskSubmitterType
import org.rucca.cheese.task.toDTO
import org.rucca.cheese.user.models.KeyPurpose
import org.rucca.cheese.user.services.EncryptionService
import org.rucca.cheese.utils.UserCreatorService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.client.MockMvcWebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpaceAnalyticsTest
@Autowired
constructor(private val userCreatorService: UserCreatorService) {
    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var jdbcTemplate: JdbcTemplate
    @Autowired private lateinit var encryptionService: EncryptionService

    private lateinit var webTestClient: WebTestClient
    private lateinit var spaceOwner: UserCreatorService.CreateUserResponse
    private lateinit var ownerToken: String
    private lateinit var participant: UserCreatorService.CreateUserResponse
    private lateinit var participantToken: String

    private val randomSuffix = Random.nextLong(10_000_000_000L)

    @BeforeAll
    fun prepare() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc).build()
        spaceOwner = userCreatorService.createUser()
        ownerToken = userCreatorService.login(spaceOwner.username, spaceOwner.password)
        participant = userCreatorService.createUser()
        participantToken = userCreatorService.login(participant.username, participant.password)
    }

    @Test
    fun `space analytics overview and tasks should reflect approved submissions`() {
        val (spaceId, defaultCategoryId) =
            createSpace(
                creatorToken = ownerToken,
                spaceName = "Analytics Space ($randomSuffix)",
                spaceIntro = "intro",
                spaceDescription = "description",
                spaceAvatarId = spaceOwner.avatarId,
            )

        val taskId =
            createTask(
                token = ownerToken,
                name = "Analytics Task ($randomSuffix)",
                submitterType = TaskSubmitterType.USER,
                deadline =
                    LocalDateTime.now()
                        .plusDays(7)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                defaultDeadline = 7L,
                resubmittable = true,
                editable = true,
                intro = "intro",
                description = "description",
                submissionSchema =
                    listOf(TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT)),
                spaceId = spaceId,
                categoryId = defaultCategoryId,
            )

        approveTask(taskId, ownerToken)
        val membershipId = addParticipantUser(ownerToken, taskId, participant.userId)
        approveTaskParticipant(ownerToken, taskId, membershipId)
        submitTask(taskId, membershipId, participantToken)

        webTestClient
            .get()
            .uri("/spaces/$spaceId/analytics/overview")
            .header("Authorization", "Bearer $ownerToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetSpaceAnalyticsOverview200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data)
                val overview = response.data!!
                assertEquals(1, overview.entityMetrics.taskCount)
                assertEquals(1, overview.entityMetrics.participantCount)
                assertEquals(1, overview.entityMetrics.approvedParticipantCount)
                assertEquals(1, overview.entityMetrics.submittedParticipantCount)
                assertEquals(0, overview.entityMetrics.successfulParticipantCount)
                assertEquals(1, overview.studentMetrics.studentCount)
                assertEquals(1, overview.studentMetrics.approvedStudentCount)
                assertEquals(0, overview.studentMetrics.successfulStudentCount)
            }

        webTestClient
            .get()
            .uri("/spaces/$spaceId/analytics/tasks?sortBy=createdAt&sortOrder=desc")
            .header("Authorization", "Bearer $ownerToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetSpaceTaskAnalytics200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data)
                val analytics = response.data!!
                assertNotNull(analytics.tasks)
                assertEquals(1, analytics.tasks!!.size)
                val task = analytics.tasks!!.first()
                assertEquals(taskId, task.taskId)
                assertEquals(1, task.participantCount)
                assertEquals(1, task.approvedParticipantCount)
                assertEquals(1, task.submittedParticipantCount)
                assertEquals(1, task.pendingReviewCount)
                assertEquals(0, task.successfulParticipantCount)
            }
    }

    @Test
    fun `space analytics publishers should aggregate by publisher and support sorting and date filters`() {
        val teacherB = userCreatorService.createUser()
        val teacherBToken = userCreatorService.login(teacherB.username, teacherB.password)
        val studentB = userCreatorService.createUser()
        val studentBToken = userCreatorService.login(studentB.username, studentB.password)

        val (spaceId, defaultCategoryId) =
            createSpace(
                creatorToken = ownerToken,
                spaceName = "Publisher Analytics Space ($randomSuffix)",
                spaceIntro = "intro",
                spaceDescription = "description",
                spaceAvatarId = spaceOwner.avatarId,
            )
        addSpaceAdmin(ownerToken, spaceId, teacherB.userId)

        val ownerTask1 =
            createTask(
                token = ownerToken,
                name = "Owner Task 1 ($randomSuffix)",
                submitterType = TaskSubmitterType.USER,
                deadline =
                    LocalDateTime.now()
                        .plusDays(7)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                defaultDeadline = 7L,
                resubmittable = true,
                editable = true,
                intro = "intro",
                description = "description",
                submissionSchema =
                    listOf(TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT)),
                spaceId = spaceId,
                categoryId = defaultCategoryId,
            )
        val ownerTask2 =
            createTask(
                token = ownerToken,
                name = "Owner Task 2 ($randomSuffix)",
                submitterType = TaskSubmitterType.USER,
                deadline =
                    LocalDateTime.now()
                        .plusDays(7)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                defaultDeadline = 7L,
                resubmittable = true,
                editable = true,
                intro = "intro",
                description = "description",
                submissionSchema =
                    listOf(TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT)),
                spaceId = spaceId,
                categoryId = defaultCategoryId,
            )
        val teacherBTask =
            createTask(
                token = teacherBToken,
                name = "Teacher B Task ($randomSuffix)",
                submitterType = TaskSubmitterType.USER,
                deadline =
                    LocalDateTime.now()
                        .plusDays(7)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                defaultDeadline = 7L,
                resubmittable = true,
                editable = true,
                intro = "intro",
                description = "description",
                submissionSchema =
                    listOf(TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT)),
                spaceId = spaceId,
                categoryId = defaultCategoryId,
            )

        approveTask(ownerTask1, ownerToken)
        approveTask(ownerTask2, ownerToken)
        approveTask(teacherBTask, ownerToken)

        val ownerTask1Membership = addParticipantUser(ownerToken, ownerTask1, participant.userId)
        val ownerTask2Membership = addParticipantUser(ownerToken, ownerTask2, studentB.userId)
        val teacherBMembership = addParticipantUser(teacherBToken, teacherBTask, studentB.userId)
        approveTaskParticipant(ownerToken, ownerTask1, ownerTask1Membership)
        approveTaskParticipant(ownerToken, ownerTask2, ownerTask2Membership)
        approveTaskParticipant(teacherBToken, teacherBTask, teacherBMembership)

        val ownerTask1Submission = submitTask(ownerTask1, ownerTask1Membership, participantToken)
        val ownerTask2Submission = submitTask(ownerTask2, ownerTask2Membership, studentBToken)
        val teacherBSubmission = submitTask(teacherBTask, teacherBMembership, studentBToken)

        reviewTaskSubmission(
            ownerToken,
            ownerTask1,
            ownerTask1Membership,
            ownerTask1Submission,
            true,
        )
        reviewTaskSubmission(
            teacherBToken,
            teacherBTask,
            teacherBMembership,
            teacherBSubmission,
            true,
        )
        val taskCountAnalytics =
            awaitSpaceAnalyticsPublishers(
                token = ownerToken,
                spaceId = spaceId,
                sortBy = "taskCount",
                sortOrder = "desc",
                condition = { publishers ->
                    publishers.size == 2 &&
                        publishers[0].taskCount == 2 &&
                        publishers[0].successfulParticipantCount == 1 &&
                        publishers[1].taskCount == 1 &&
                        publishers[1].successfulParticipantCount == 1
                },
            )
        assertEquals(2, taskCountAnalytics.publishers?.size)
        assertEquals(2, taskCountAnalytics.publishers?.get(0)?.taskCount)
        assertEquals(2, taskCountAnalytics.publishers?.get(0)?.participantCount)
        assertEquals(2, taskCountAnalytics.publishers?.get(0)?.submittedParticipantCount)
        assertEquals(1, taskCountAnalytics.publishers?.get(0)?.successfulParticipantCount)
        assertEquals(1.0, taskCountAnalytics.publishers?.get(0)?.avgParticipantsPerTask)
        assertEquals(0.5, taskCountAnalytics.publishers?.get(0)?.successRate)
        assertEquals(1, taskCountAnalytics.publishers?.get(1)?.taskCount)
        assertEquals(1, taskCountAnalytics.publishers?.get(1)?.successfulParticipantCount)

        val successRateAnalytics =
            getSpaceAnalyticsPublishers(
                spaceId = spaceId,
                token = ownerToken,
                sortBy = "successRate",
                sortOrder = "desc",
            )
        assertEquals(1, successRateAnalytics.publishers?.get(0)?.taskCount)
        assertEquals(1.0, successRateAnalytics.publishers?.get(0)?.successRate)
        assertEquals(2, successRateAnalytics.publishers?.get(1)?.taskCount)
        assertEquals(0.5, successRateAnalytics.publishers?.get(1)?.successRate)

        val futureWindowAnalytics =
            getSpaceAnalyticsPublishers(
                spaceId = spaceId,
                token = ownerToken,
                from = System.currentTimeMillis() + 86_400_000,
            )
        assertEquals(0, futureWindowAnalytics.publishers?.size)
    }

    @Test
    fun `space analytics alerts should report pending workload and stalled tasks`() {
        val studentB = userCreatorService.createUser()

        val (spaceId, defaultCategoryId) =
            createSpace(
                creatorToken = ownerToken,
                spaceName = "Alerts Analytics Space ($randomSuffix)",
                spaceIntro = "intro",
                spaceDescription = "description",
                spaceAvatarId = spaceOwner.avatarId,
            )

        val pendingTaskId =
            createTask(
                token = ownerToken,
                name = "Pending Approval Task ($randomSuffix)",
                submitterType = TaskSubmitterType.USER,
                deadline =
                    LocalDateTime.now()
                        .plusDays(7)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                defaultDeadline = 7L,
                resubmittable = true,
                editable = true,
                intro = "intro",
                description = "description",
                submissionSchema =
                    listOf(TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT)),
                spaceId = spaceId,
                categoryId = defaultCategoryId,
            )
        val pendingParticipantTaskId =
            createTask(
                token = ownerToken,
                name = "Pending Participant Task ($randomSuffix)",
                submitterType = TaskSubmitterType.USER,
                deadline =
                    LocalDateTime.now()
                        .plusDays(7)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                defaultDeadline = 7L,
                resubmittable = true,
                editable = true,
                intro = "intro",
                description = "description",
                submissionSchema =
                    listOf(TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT)),
                spaceId = spaceId,
                categoryId = defaultCategoryId,
            )
        val pendingReviewTaskId =
            createTask(
                token = ownerToken,
                name = "Pending Review Task ($randomSuffix)",
                submitterType = TaskSubmitterType.USER,
                deadline =
                    LocalDateTime.now()
                        .plusDays(7)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                defaultDeadline = 7L,
                resubmittable = true,
                editable = true,
                intro = "intro",
                description = "description",
                submissionSchema =
                    listOf(TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT)),
                spaceId = spaceId,
                categoryId = defaultCategoryId,
            )
        val stalledTaskId =
            createTask(
                token = ownerToken,
                name = "Stalled Task ($randomSuffix)",
                submitterType = TaskSubmitterType.USER,
                deadline =
                    LocalDateTime.now()
                        .plusDays(7)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                defaultDeadline = 7L,
                resubmittable = true,
                editable = true,
                intro = "intro",
                description = "description",
                submissionSchema =
                    listOf(TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT)),
                spaceId = spaceId,
                categoryId = defaultCategoryId,
            )

        approveTask(pendingParticipantTaskId, ownerToken)
        approveTask(pendingReviewTaskId, ownerToken)
        approveTask(stalledTaskId, ownerToken)

        val pendingMembership =
            addParticipantUser(ownerToken, pendingParticipantTaskId, participant.userId)
        assertTrue(pendingMembership > 0)

        val pendingReviewMembership =
            addParticipantUser(ownerToken, pendingReviewTaskId, participant.userId)
        approveTaskParticipant(ownerToken, pendingReviewTaskId, pendingReviewMembership)
        submitTask(pendingReviewTaskId, pendingReviewMembership, participantToken)

        val stalledMembership = addParticipantUser(ownerToken, stalledTaskId, studentB.userId)
        approveTaskParticipant(ownerToken, stalledTaskId, stalledMembership)
        markTaskCreatedDaysAgo(stalledTaskId, 15)

        assertTrue(pendingTaskId > 0)

        webTestClient
            .get()
            .uri("/spaces/$spaceId/analytics/alerts")
            .header("Authorization", "Bearer $ownerToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.pendingTaskApprovalCount")
            .isEqualTo(1)
            .jsonPath("$.data.pendingParticipantApprovalCount")
            .isEqualTo(1)
            .jsonPath("$.data.pendingSubmissionReviewCount")
            .isEqualTo(1)
            .jsonPath("$.data.stalledTaskCount")
            .isEqualTo(1)
            .jsonPath("$.data.overdueUnreviewedSubmissionCount")
            .isEqualTo(0)
    }

    @Test
    fun `space analytics participants should summarize participant states and support filtering`() {
        val studentB = userCreatorService.createUser()

        val (spaceId, defaultCategoryId) =
            createSpace(
                creatorToken = ownerToken,
                spaceName = "Participants Analytics Space ($randomSuffix)",
                spaceIntro = "intro",
                spaceDescription = "description",
                spaceAvatarId = spaceOwner.avatarId,
            )

        val successTaskId =
            createTask(
                token = ownerToken,
                name = "Success Task ($randomSuffix)",
                submitterType = TaskSubmitterType.USER,
                deadline =
                    LocalDateTime.now()
                        .plusDays(7)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                defaultDeadline = 7L,
                resubmittable = true,
                editable = true,
                intro = "intro",
                description = "description",
                submissionSchema =
                    listOf(TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT)),
                spaceId = spaceId,
                categoryId = defaultCategoryId,
            )
        val pendingTaskId =
            createTask(
                token = ownerToken,
                name = "Pending Task ($randomSuffix)",
                submitterType = TaskSubmitterType.USER,
                deadline =
                    LocalDateTime.now()
                        .plusDays(7)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                defaultDeadline = 7L,
                resubmittable = true,
                editable = true,
                intro = "intro",
                description = "description",
                submissionSchema =
                    listOf(TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT)),
                spaceId = spaceId,
                categoryId = defaultCategoryId,
            )

        approveTask(successTaskId, ownerToken)
        approveTask(pendingTaskId, ownerToken)

        val successMembershipId = addParticipantUser(ownerToken, successTaskId, participant.userId)
        approveTaskParticipant(ownerToken, successTaskId, successMembershipId)
        markMembershipRealNameInfo(successMembershipId, "Alice", "20260001", "2026", "CS", "CS-1")
        val successSubmissionId = submitTask(successTaskId, successMembershipId, participantToken)
        reviewTaskSubmission(
            ownerToken,
            successTaskId,
            successMembershipId,
            successSubmissionId,
            true,
        )

        addParticipantUser(ownerToken, pendingTaskId, studentB.userId)

        webTestClient
            .get()
            .uri("/spaces/$spaceId/analytics/participants")
            .header("Authorization", "Bearer $ownerToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.entityMetrics.participantCount")
            .isEqualTo(2)
            .jsonPath("$.data.entityMetrics.approvedParticipantCount")
            .isEqualTo(1)
            .jsonPath("$.data.entityMetrics.pendingParticipantCount")
            .isEqualTo(1)
            .jsonPath("$.data.entityMetrics.successfulParticipantCount")
            .isEqualTo(1)
            .jsonPath("$.data.studentMetrics.studentCount")
            .isEqualTo(2)
            .jsonPath("$.data.studentMetrics.studentsWithRealNameCount")
            .isEqualTo(1)

        webTestClient
            .get()
            .uri("/spaces/$spaceId/analytics/participants?realName=with")
            .header("Authorization", "Bearer $ownerToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.entityMetrics.participantCount")
            .isEqualTo(1)
            .jsonPath("$.data.entityMetrics.successfulParticipantCount")
            .isEqualTo(1)
    }

    @Test
    fun `space analytics exports should provide participant task and publisher csv outputs`() {
        val (spaceId, defaultCategoryId) =
            createSpace(
                creatorToken = ownerToken,
                spaceName = "Export Analytics Space ($randomSuffix)",
                spaceIntro = "intro",
                spaceDescription = "description",
                spaceAvatarId = spaceOwner.avatarId,
            )

        val taskId =
            createTask(
                token = ownerToken,
                name = "Export Task ($randomSuffix)",
                submitterType = TaskSubmitterType.USER,
                deadline =
                    LocalDateTime.now()
                        .plusDays(7)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                defaultDeadline = 7L,
                resubmittable = true,
                editable = true,
                intro = "intro",
                description = "description",
                submissionSchema =
                    listOf(TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT)),
                spaceId = spaceId,
                categoryId = defaultCategoryId,
            )

        approveTask(taskId, ownerToken)
        val membershipId = addParticipantUser(ownerToken, taskId, participant.userId)
        approveTaskParticipant(ownerToken, taskId, membershipId)
        markMembershipRealNameInfo(membershipId, "Bob", "20260002", "2026", "Math", "Math-1")
        val submissionId = submitTask(taskId, membershipId, participantToken)
        reviewTaskSubmission(ownerToken, taskId, membershipId, submissionId, true)

        val participantsCsv =
            getAnalyticsCsv("/spaces/$spaceId/analytics/participants/export", ownerToken)
        assertTrue(
            participantsCsv.contains(
                "Task ID,Task Title,Category,Task Rank,Task Creator,Created At,Deadline,Member ID"
            )
        )
        assertTrue(participantsCsv.contains("SUCCESS"))
        assertTrue(participantsCsv.contains("Bob"))

        val tasksCsv = getAnalyticsCsv("/spaces/$spaceId/analytics/tasks/export", ownerToken)
        assertTrue(
            tasksCsv.contains(
                "Task ID,Task Title,Category,Rank,Creator,Created At,Deadline,Total Participants"
            )
        )
        assertTrue(tasksCsv.contains("Export Task"))

        val publishersCsv =
            getAnalyticsCsv("/spaces/$spaceId/analytics/publishers/export", ownerToken)
        assertTrue(
            publishersCsv.contains(
                "Publisher ID,Publisher Name,Total Tasks,Total Participants,Approved Participants"
            )
        )
        assertTrue(publishersCsv.contains(spaceOwner.username))
    }

    @Test
    fun `space analytics participants and exports should decrypt encrypted real name snapshots`() {
        val (spaceId, defaultCategoryId) =
            createSpace(
                creatorToken = ownerToken,
                spaceName = "Encrypted Analytics Space ($randomSuffix)",
                spaceIntro = "intro",
                spaceDescription = "description",
                spaceAvatarId = spaceOwner.avatarId,
            )

        val taskId =
            createTask(
                token = ownerToken,
                name = "Encrypted Task ($randomSuffix)",
                submitterType = TaskSubmitterType.USER,
                deadline =
                    LocalDateTime.now()
                        .plusDays(7)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                defaultDeadline = 7L,
                resubmittable = true,
                editable = true,
                intro = "intro",
                description = "description",
                submissionSchema =
                    listOf(TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT)),
                spaceId = spaceId,
                categoryId = defaultCategoryId,
            )

        approveTask(taskId, ownerToken)
        val membershipId = addParticipantUser(ownerToken, taskId, participant.userId)
        approveTaskParticipant(ownerToken, taskId, membershipId)
        val taskKey = encryptionService.getOrCreateKey(KeyPurpose.TASK_REAL_NAME, taskId)
        val encryptedRealName = encryptionService.encryptData("Carol", taskKey.id)
        markMembershipEncryptedRealNameInfo(
            keyId = taskKey.id,
            membershipId = membershipId,
            realName = "Carol",
            studentId = "20260003",
            grade = "2026",
            major = "Physics",
            className = "Physics-1",
        )

        webTestClient
            .get()
            .uri("/spaces/$spaceId/analytics/participants")
            .header("Authorization", "Bearer $ownerToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.studentMetrics.studentsWithRealNameCount")
            .isEqualTo(1)
            .jsonPath("$.data.distributions.byGrade.items[0].label")
            .isEqualTo("2026")
            .jsonPath("$.data.distributions.byMajor.items[0].label")
            .isEqualTo("Physics")

        val participantsCsv =
            getAnalyticsCsv("/spaces/$spaceId/analytics/participants/export", ownerToken)
        assertTrue(participantsCsv.contains("Carol"))
        assertTrue(participantsCsv.contains("20260003"))
        assertTrue(!participantsCsv.contains(encryptedRealName))
    }

    private fun createSpace(
        creatorToken: String,
        spaceName: String,
        spaceIntro: String,
        spaceDescription: String,
        spaceAvatarId: IdType,
    ): Pair<IdType, IdType> {
        val requestDTO =
            PostSpaceRequestDTO(
                name = spaceName,
                intro = spaceIntro,
                description = spaceDescription,
                avatarId = spaceAvatarId,
                announcements = "[]",
                taskTemplates = "[]",
            )
        val response =
            webTestClient
                .post()
                .uri("/spaces")
                .header("Authorization", "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<PatchSpace200ResponseDTO>()
                .returnResult()
                .responseBody

        return Pair(response!!.data.space.id, response.data.space.defaultCategoryId)
    }

    private fun createTask(
        token: String,
        name: String,
        submitterType: TaskSubmitterType,
        deadline: Long?,
        defaultDeadline: Long?,
        resubmittable: Boolean,
        editable: Boolean,
        intro: String,
        description: String,
        submissionSchema: List<TaskSubmissionSchemaEntryDTO>,
        spaceId: IdType,
        categoryId: IdType,
    ): IdType {
        val requestDTO =
            PostTaskRequestDTO(
                name = name,
                submitterType = submitterType.toDTO(),
                deadline = deadline,
                defaultDeadline = defaultDeadline,
                resubmittable = resubmittable,
                editable = editable,
                intro = intro,
                description = description,
                submissionSchema = submissionSchema,
                space = spaceId,
                categoryId = categoryId,
                topics = emptyList(),
                rank = null,
            )
        val response =
            webTestClient
                .post()
                .uri("/tasks")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<PatchTask200ResponseDTO>()
                .returnResult()
                .responseBody

        return response!!.data.task.id
    }

    private fun approveTask(taskId: IdType, token: String) {
        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(PatchTaskRequestDTO(approved = ApproveTypeDTO.APPROVED))
            .exchange()
            .expectStatus()
            .isOk
    }

    private fun addSpaceAdmin(token: String, spaceId: IdType, userId: IdType) {
        webTestClient
            .post()
            .uri("/spaces/$spaceId/managers")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                PostSpaceAdminRequestDTO(role = SpaceAdminRoleTypeDTO.ADMIN, userId = userId)
            )
            .exchange()
            .expectStatus()
            .isOk
    }

    private fun addParticipantUser(token: String, taskId: IdType, userId: IdType): IdType {
        val response =
            webTestClient
                .post()
                .uri { builder ->
                    builder.path("/tasks/$taskId/participants").queryParam("member", userId).build()
                }
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(PostTaskParticipantRequestDTO(email = "participant@example.com"))
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<PostTaskParticipant200ResponseDTO>()
                .returnResult()
                .responseBody

        return response!!.data.participant!!.id
    }

    private fun approveTaskParticipant(
        token: String,
        taskId: IdType,
        participantMembershipId: IdType,
    ) {
        webTestClient
            .patch()
            .uri("/tasks/$taskId/participants/$participantMembershipId")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(PatchTaskMembershipRequestDTO(approved = ApproveTypeDTO.APPROVED))
            .exchange()
            .expectStatus()
            .isOk
    }

    private fun submitTask(
        taskId: IdType,
        participantMembershipId: IdType,
        participantToken: String,
    ): IdType {
        val response =
            webTestClient
                .post()
                .uri("/tasks/$taskId/participants/$participantMembershipId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(listOf(TaskSubmissionContentDTO(text = "Submission text")))
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<PostTaskSubmission200ResponseDTO>()
                .returnResult()
                .responseBody

        return response!!.data.submission.id
    }

    private fun reviewTaskSubmission(
        token: String,
        taskId: IdType,
        participantMembershipId: IdType,
        submissionId: IdType,
        accepted: Boolean,
    ) {
        webTestClient
            .post()
            .uri(
                "/tasks/$taskId/participants/$participantMembershipId/submissions/$submissionId/review"
            )
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                PostTaskSubmissionReviewRequestDTO(
                    accepted = accepted,
                    score = if (accepted) 100 else 0,
                    comment = "review",
                )
            )
            .exchange()
            .expectStatus()
            .isOk
    }

    private fun getSpaceAnalyticsPublishers(
        token: String,
        spaceId: IdType,
        from: Long? = null,
        sortBy: String = "taskCount",
        sortOrder: String = "desc",
    ): SpaceAnalyticsPublishersDTO {
        val uriBuilder =
            StringBuilder(
                "/spaces/$spaceId/analytics/publishers?sortBy=$sortBy&sortOrder=$sortOrder"
            )
        if (from != null) {
            uriBuilder.append("&from=").append(from)
        }

        val response =
            webTestClient
                .get()
                .uri(uriBuilder.toString())
                .header("Authorization", "Bearer $token")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<GetSpaceAnalyticsPublishers200ResponseDTO>()
                .returnResult()
                .responseBody

        return response!!.data!!
    }

    private fun awaitSpaceAnalyticsPublishers(
        token: String,
        spaceId: IdType,
        sortBy: String,
        sortOrder: String,
        condition: (List<SpaceAnalyticsPublisherMetricsDTO>) -> Boolean,
        timeoutMillis: Long = 15_000,
    ): SpaceAnalyticsPublishersDTO {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var latestAnalytics: SpaceAnalyticsPublishersDTO? = null

        while (System.currentTimeMillis() <= deadline) {
            latestAnalytics =
                getSpaceAnalyticsPublishers(
                    token = token,
                    spaceId = spaceId,
                    sortBy = sortBy,
                    sortOrder = sortOrder,
                )
            val publishers = latestAnalytics.publishers.orEmpty()
            if (condition(publishers)) {
                return latestAnalytics
            }
            Thread.sleep(100)
        }

        assertTrue(
            latestAnalytics != null && condition(latestAnalytics.publishers.orEmpty()),
            "Timed out waiting for publisher analytics to satisfy condition. " +
                "Latest publishers=${latestAnalytics?.publishers.orEmpty()}",
        )
        return latestAnalytics!!
    }

    private fun markTaskCreatedDaysAgo(taskId: IdType, daysAgo: Long) {
        val timestamp = LocalDateTime.now().minusDays(daysAgo)
        jdbcTemplate.update(
            "UPDATE task SET created_at = ?, updated_at = ? WHERE id = ?",
            timestamp,
            timestamp,
            taskId,
        )
    }

    private fun markMembershipRealNameInfo(
        membershipId: IdType,
        realName: String,
        studentId: String,
        grade: String,
        major: String,
        className: String,
    ) {
        jdbcTemplate.update(
            """
                UPDATE task_membership
                SET real_name = ?, student_id = ?, grade = ?, major = ?, class_name = ?, encrypted = false
                WHERE id = ?
            """
                .trimIndent(),
            realName,
            studentId,
            grade,
            major,
            className,
            membershipId,
        )
    }

    private fun markMembershipEncryptedRealNameInfo(
        keyId: String,
        membershipId: IdType,
        realName: String,
        studentId: String,
        grade: String,
        major: String,
        className: String,
    ) {
        jdbcTemplate.update(
            """
                UPDATE task_membership
                SET real_name = ?, student_id = ?, grade = ?, major = ?, class_name = ?,
                    encrypted = true, encryption_key_id = ?
                WHERE id = ?
            """
                .trimIndent(),
            encryptionService.encryptData(realName, keyId),
            encryptionService.encryptData(studentId, keyId),
            encryptionService.encryptData(grade, keyId),
            encryptionService.encryptData(major, keyId),
            encryptionService.encryptData(className, keyId),
            keyId,
            membershipId,
        )
    }

    private fun getAnalyticsCsv(path: String, token: String): String {
        val result: EntityExchangeResult<ByteArray> =
            webTestClient
                .get()
                .uri(path)
                .header("Authorization", "Bearer $token")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()

        return result.responseBody?.toString(Charsets.UTF_8) ?: ""
    }
}
