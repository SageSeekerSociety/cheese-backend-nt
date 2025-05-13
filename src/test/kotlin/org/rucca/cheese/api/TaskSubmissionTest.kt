/*
 *  Description: It tests the feature of task's submission.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      HuanCheng65
 *      nameisyui
 *
 */

package org.rucca.cheese.api

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.floor
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.task.TaskSubmitterType
import org.rucca.cheese.task.toDTO
import org.rucca.cheese.utils.AttachmentCreatorService
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation::class)
class TaskSubmissionTest
@Autowired
constructor(
    private val webTestClient: WebTestClient, // Inject WebTestClient
    private val userCreatorService: UserCreatorService,
    private val attachmentCreatorService: AttachmentCreatorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // User variables remain the same
    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    lateinit var teamCreator: UserCreatorService.CreateUserResponse
    lateinit var teamCreatorToken: String
    lateinit var teamMember: UserCreatorService.CreateUserResponse
    lateinit var teamMemberToken: String
    lateinit var spaceCreator: UserCreatorService.CreateUserResponse
    lateinit var spaceCreatorToken: String
    lateinit var participant: UserCreatorService.CreateUserResponse
    lateinit var participantToken: String
    lateinit var participant2: UserCreatorService.CreateUserResponse
    lateinit var participantToken2: String
    lateinit var irrelevantUser: UserCreatorService.CreateUserResponse
    lateinit var irrelevantUserToken: String

    // IDs and other data remain the same
    private var teamId: IdType = -1
    private var spaceId: IdType = -1
    private var defaultCategoryId: IdType = -1
    private var attachmentId: IdType = -1
    private val taskIds = mutableListOf<IdType>()
    private val randomSuffix = floor(Math.random() * 10000000000).toLong()
    private val taskName = "Test Task ($randomSuffix)"
    private val taskIntro = "This is a test task."
    private val taskDescription = "A lengthy text. ".repeat(1000)
    private val taskDeadline =
        LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val taskSubmissionSchema =
        listOf(
            TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT),
            TaskSubmissionSchemaEntryDTO("Attachment Entry", TaskSubmissionTypeDTO.FILE),
        )
    private var participantTaskMembershipId: IdType = -1
    private var participant2TaskMembershipId: IdType = -1
    private var teamTaskMembershipId: IdType = -1

    // --- Helper DTO for Error Responses ---
    // Reusing the generic error structure from previous refactors
    data class ErrorData(
        val type: String? = null,
        val id: Any? = null,
        val name: String? = null,
        val taskId: IdType? = null,
    )

    data class ErrorDetail(val name: String, val data: ErrorData?)

    data class GenericErrorResponse(val error: ErrorDetail)

    // --- Refactored Helper Methods ---

    // createSpace refactored in previous example, reusing it here
    fun createSpace(
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
                .expectBody<PatchSpace200ResponseDTO>() // Assuming POST returns Patch DTO
                .returnResult()
                .responseBody

        assertNotNull(response?.data?.space, "Space data should not be null in response")
        val createdSpace = response!!.data.space
        assertNotNull(createdSpace.id, "Created space ID cannot be null")
        assertNotNull(createdSpace.defaultCategoryId, "Default category ID cannot be null")

        val createdSpaceId = createdSpace.id
        val createdDefaultCategoryId = createdSpace.defaultCategoryId
        logger.info(
            "Created space: $createdSpaceId with default category ID: $createdDefaultCategoryId"
        )
        return Pair(createdSpaceId, createdDefaultCategoryId)
    }

    fun createTeam(
        creatorToken: String,
        teamName: String,
        teamIntro: String,
        teamDescription: String,
        teamAvatarId: IdType,
    ): IdType {
        // Assuming PostTeamRequestDTO exists
        val requestDTO =
            PostTeamRequestDTO(
                name = teamName,
                intro = teamIntro,
                description = teamDescription,
                avatarId = teamAvatarId,
            )
        // Assuming the response is something like PostTeamResponseDTO containing the team ID
        val response =
            webTestClient
                .post()
                .uri("/teams")
                .header("Authorization", "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk // Assuming 200 OK on creation
                .expectBody<GetTeam200ResponseDTO>() // Adjust DTO name as needed
                .returnResult()
                .responseBody

        assertNotNull(response?.data?.team, "Team data missing in response")
        val createdTeamId = response!!.data.team.id
        assertNotNull(createdTeamId, "Created team ID should not be null")
        logger.info("Created team: $createdTeamId")
        return createdTeamId
    }

    fun requestToJoinTeam(userToken: String, teamId: IdType, message: String? = null): IdType {
        // TeamJoinRequestCreate DTO - message is optional based on API spec
        val requestDTO = mapOf("message" to message).filterValues { it != null }

        val response =
            webTestClient
                .post()
                .uri("/teams/$teamId/requests")
                .header(
                    "Authorization",
                    "Bearer $userToken",
                ) // User who wants to join sends request
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isCreated // API indicates 201 Created
                .expectBody<Map<String, Any>>() // Generic map to handle response
                .returnResult()
                .responseBody

        // Extract the request ID from the response
        val applicationData = (response?.get("data") as? Map<*, *>)
        val application = (applicationData?.get("application") as? Map<*, *>)
        val requestId =
            application?.get("id") as? Number
                ?: throw AssertionError("Request ID not found in response")

        return requestId.toLong()
    }

    fun approveTeamJoinRequest(adminToken: String, teamId: IdType, requestId: IdType) {
        webTestClient
            .post()
            .uri("/teams/$teamId/requests/$requestId/approve")
            .header("Authorization", "Bearer $adminToken") // Team admin approves
            .exchange()
            .expectStatus()
            .isNoContent() // API indicates 204 No Content
    }

    fun joinTeamWithApproval(
        userToken: String,
        adminToken: String,
        teamId: IdType,
        userId: IdType,
    ) {
        val requestId = requestToJoinTeam(userToken, teamId)
        approveTeamJoinRequest(adminToken, teamId, requestId)
    }

    @BeforeAll
    fun prepare() {
        // User creation and login remain the same
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
        teamCreator = userCreatorService.createUser()
        teamCreatorToken = userCreatorService.login(teamCreator.username, teamCreator.password)
        teamMember = userCreatorService.createUser()
        teamMemberToken = userCreatorService.login(teamMember.username, teamMember.password)
        spaceCreator = userCreatorService.createUser()
        spaceCreatorToken = userCreatorService.login(spaceCreator.username, spaceCreator.password)
        participant = userCreatorService.createUser()
        participantToken = userCreatorService.login(participant.username, participant.password)
        participant2 = userCreatorService.createUser()
        participantToken2 = userCreatorService.login(participant2.username, participant2.password)
        irrelevantUser = userCreatorService.createUser()
        irrelevantUserToken =
            userCreatorService.login(irrelevantUser.username, irrelevantUser.password)

        // Setup space, team, attachment using refactored helpers
        val spaceResult =
            createSpace(
                creatorToken = spaceCreatorToken,
                spaceName = "Test Space ($randomSuffix)",
                spaceIntro = "This is a test space.",
                spaceDescription = "A lengthy text. ".repeat(1000),
                spaceAvatarId = userCreatorService.testAvatarId(),
            )
        spaceId = spaceResult.first
        defaultCategoryId = spaceResult.second

        teamId =
            createTeam(
                creatorToken = teamCreatorToken,
                teamName = "Test Team ($randomSuffix)",
                teamIntro = "This is a test team.",
                teamDescription = "A lengthy text. ".repeat(1000),
                teamAvatarId = userCreatorService.testAvatarId(),
            )
        joinTeamWithApproval(
            teamMemberToken,
            teamCreatorToken,
            teamId,
            teamMember.userId,
        ) // Team creator adds member

        attachmentId =
            attachmentCreatorService.createAttachment(creatorToken) // Use any valid token
    }

    fun createTask(
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
        categoryId: IdType?,
        token: String = creatorToken,
        expectedStatus: Int = HttpStatus.OK.value(),
    ): IdType? {
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
                topics = emptyList(), // Add defaults if needed
                rank = null, // Add defaults if needed
            )

        val responseSpec =
            webTestClient
                .post()
                .uri("/tasks")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isEqualTo(expectedStatus) // Check expected status first

        if (expectedStatus == HttpStatus.OK.value()) {
            val responseBody =
                responseSpec
                    .expectBody<PatchTask200ResponseDTO>() // Expect success DTO
                    .returnResult()
                    .responseBody

            assertNotNull(responseBody?.data?.task, "Task data missing in successful response")
            val task = responseBody!!.data.task

            // --- Assertions on the Deserialized DTO ---
            assertEquals(name, task.name)
            assertEquals(submitterType.name, task.submitterType.value)
            assertEquals(deadline, task.deadline)
            assertEquals(defaultDeadline, task.defaultDeadline)
            assertEquals(resubmittable, task.resubmittable)
            assertEquals(editable, task.editable)
            assertEquals(intro, task.intro)
            assertEquals(description, task.description)
            assertEquals(spaceId, task.space?.id)
            assertEquals(ApproveTypeDTO.NONE, task.approved)

            // Assert submission schema
            assertNotNull(task.submissionSchema)
            assertEquals(submissionSchema.size, task.submissionSchema.size)
            submissionSchema.forEach { expectedEntry ->
                val found = task.submissionSchema.find { it.prompt == expectedEntry.prompt }
                assertNotNull(found, "Submission schema entry '${expectedEntry.prompt}' not found")
                assertEquals(
                    expectedEntry.type,
                    found!!.type,
                    "Type mismatch for '${expectedEntry.prompt}'",
                )
            }

            val createdTaskId = task.id
            assertNotNull(createdTaskId, "Created task ID should not be null")
            taskIds.add(createdTaskId)
            logger.info(
                "Created task: $createdTaskId (Type: $submitterType, Space: $spaceId, Category: ${categoryId ?: defaultCategoryId})"
            )
            return createdTaskId
        }
        return null
    }

    fun approveTask(taskId: IdType, token: String) {
        // Assuming PatchTaskRequestDTO exists
        val requestDTO = PatchTaskRequestDTO(approved = ApproveTypeDTO.APPROVED) // Use enum DTO

        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTask200ResponseDTO>() // Expect task response DTO
            .value { response ->
                assertNotNull(response.data.task, "Task data missing")
                assertEquals(ApproveTypeDTO.APPROVED, response.data.task.approved)
            }
    }

    fun approveTaskParticipant(token: String, taskId: IdType, participantMembershipId: IdType) {
        // Assuming PatchTaskMembershipRequestDTO exists
        val requestDTO =
            PatchTaskMembershipRequestDTO(approved = ApproveTypeDTO.APPROVED) // Use enum DTO

        webTestClient
            .patch()
            .uri("/tasks/$taskId/participants/$participantMembershipId")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<
                GetTaskParticipant200ResponseDTO
            >() // Assuming PATCH returns the updated membership DTO
            .value { response ->
                assertNotNull(response.data.taskMembership, "Task membership data missing")
                assertEquals(ApproveTypeDTO.APPROVED, response.data.taskMembership.approved)
            }
    }

    fun addParticipantUser(token: String, taskId: IdType, userId: IdType): IdType {
        // Assuming PostTaskParticipantRequestDTO exists, even if minimal
        val requestDTO = PostTaskParticipantRequestDTO(email = "test@example.com") // Minimal data

        val response =
            webTestClient
                .post()
                .uri { builder ->
                    builder.path("/tasks/$taskId/participants").queryParam("member", userId).build()
                }
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<
                    PostTaskParticipant200ResponseDTO
                >() // Expect the participant response DTO
                .returnResult()
                .responseBody

        // Assuming the DTO has data.participant.id structure based on original JSON parsing
        assertNotNull(response?.data?.participant, "Participant data should not be null")
        val taskMembershipId = response!!.data.participant!!.id
        assertNotNull(taskMembershipId, "Task Membership ID should not be null")
        logger.info("Added user participant, membership ID: $taskMembershipId")
        return taskMembershipId
    }

    fun addParticipantTeam(token: String, taskId: IdType, teamId: IdType): IdType {
        // Assuming PostTaskParticipantRequestDTO exists, even if minimal
        val requestDTO = PostTaskParticipantRequestDTO(email = "test@example.com") // Minimal data

        val response =
            webTestClient
                .post()
                .uri { builder ->
                    builder.path("/tasks/$taskId/participants").queryParam("member", teamId).build()
                } // Use teamId as member
                .header(
                    "Authorization",
                    "Bearer $token",
                ) // Token of someone allowed to add team (e.g., space creator?)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<PostTaskParticipant200ResponseDTO>()
                .returnResult()
                .responseBody

        assertNotNull(response?.data?.participant, "Participant data should not be null")
        val taskMembershipId = response!!.data.participant!!.id
        assertNotNull(taskMembershipId, "Task Membership ID should not be null")
        logger.info("Added team participant, membership ID: $taskMembershipId")
        return taskMembershipId
    }

    // --- Refactored Test Methods ---

    @Test
    @Order(10)
    fun `test create tasks`() { // Renamed
        // Task 1 (User submitter) - Use spaceCreatorToken as per original logic?
        createTask(
            name = "$taskName (1)",
            submitterType = TaskSubmitterType.USER,
            deadline = taskDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            spaceId = spaceId,
            categoryId = defaultCategoryId,
            defaultDeadline = 30,
            token = spaceCreatorToken,
        )
        approveTask(taskIds[0], spaceCreatorToken)

        // Task 2 (Team submitter)
        createTask(
            name = "$taskName (2)",
            submitterType = TaskSubmitterType.TEAM,
            deadline = taskDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            spaceId = spaceId,
            categoryId = defaultCategoryId,
            defaultDeadline = 30,
            token = spaceCreatorToken,
        )
        approveTask(taskIds[1], spaceCreatorToken)

        // Task 3 (User submitter)
        createTask(
            name = "$taskName (3)",
            submitterType = TaskSubmitterType.USER,
            deadline = taskDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            spaceId = spaceId,
            categoryId = defaultCategoryId,
            defaultDeadline = 30,
            token = spaceCreatorToken,
        )
        approveTask(taskIds[2], spaceCreatorToken)

        // Task 4 (User submitter, not approved initially)
        createTask(
            name = "$taskName (4)",
            submitterType = TaskSubmitterType.USER,
            deadline = taskDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            spaceId = spaceId,
            categoryId = defaultCategoryId,
            defaultDeadline = 30,
            token = spaceCreatorToken,
        )
        // Task 4 is not approved here in the original logic
    }

    @Test
    @Order(40)
    fun `test update task details`() { // Renamed
        val taskIdToUpdate = taskIds[0]
        val updatedName = "$taskName (1) (updated)"
        val updatedDeadline = taskDeadline + 1000000000L // Use L suffix for Long
        val updatedResubmittable = false
        val updatedEditable = false
        val updatedIntro = "This is an updated test task."
        val updatedDescription = "$taskDescription (updated)"
        val updatedSchema =
            listOf(
                TaskSubmissionSchemaEntryDTO(
                    prompt = "Text Entry",
                    type = TaskSubmissionTypeDTO.TEXT,
                )
            ) // Only text
        val updatedRank = 1

        // Assuming PatchTaskRequestDTO exists
        val requestDTO =
            PatchTaskRequestDTO(
                name = updatedName,
                deadline = updatedDeadline,
                resubmittable = updatedResubmittable,
                editable = updatedEditable,
                intro = updatedIntro,
                description = updatedDescription,
                submissionSchema = updatedSchema,
                rank = updatedRank,
                // Specify hasDeadline=true if needed by DTO/API, otherwise Long? handles it
            )

        webTestClient
            .patch()
            .uri("/tasks/$taskIdToUpdate")
            .header(
                "Authorization",
                "Bearer $spaceCreatorToken",
            ) // Original test used creatorToken, but spaceCreator created the task
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTask200ResponseDTO>() // Expect response DTO
            .value { response ->
                assertNotNull(response.data.task)
                val task = response.data.task
                assertEquals(updatedName, task.name)
                assertEquals(updatedDeadline, task.deadline)
                assertEquals(updatedResubmittable, task.resubmittable)
                assertEquals(updatedEditable, task.editable)
                assertEquals(updatedIntro, task.intro)
                assertEquals(updatedDescription, task.description)
                assertEquals(updatedRank, task.rank)
                assertNotNull(task.submissionSchema)
                assertEquals(1, task.submissionSchema.size)
                assertEquals("Text Entry", task.submissionSchema[0].prompt)
            }
    }

    @Test
    @Order(85)
    fun `test add participant user 1`() { // Renamed
        participantTaskMembershipId =
            addParticipantUser(participantToken, taskIds[0], participant.userId)
        assertNotEquals(-1L, participantTaskMembershipId)
    }

    @Test
    @Order(86)
    fun `test add participant user 2`() { // Renamed
        participant2TaskMembershipId =
            addParticipantUser(participantToken2, taskIds[0], participant2.userId)
        assertNotEquals(-1L, participant2TaskMembershipId)
    }

    @Test
    @Order(95)
    fun `test add participant team`() { // Renamed
        // Use teamCreator token as they are likely allowed to add the team they created
        teamTaskMembershipId = addParticipantTeam(teamCreatorToken, taskIds[1], teamId)
        assertNotEquals(-1L, teamTaskMembershipId)
    }

    @Test
    @Order(106)
    fun `test get task 1 eligibility for participant 1 before approval`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/${taskIds[0]}")
                    .queryParam("queryJoinability", true)
                    .queryParam("querySubmittability", true)
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTask200ResponseDTO>() // Expect Get Task DTO
            .value { response ->
                assertNotNull(response.data.task)
                val task = response.data.task
                assertEquals(
                    TaskSubmitterTypeDTO.USER,
                    task.submitterType,
                    "Task type should be USER",
                )

                assertNotNull(
                    task.participationEligibility,
                    "Participation eligibility data should be present",
                )
                val eligibility = task.participationEligibility!!

                assertNull(eligibility.teams, "Team status should be null for USER task")
                assertNotNull(eligibility.user, "User status should be present for USER task")

                val userStatus = eligibility.user!!
                // User has joined (added in 85) but not approved yet.
                // Therefore, they are NOT eligible to join again.
                assertEquals(
                    false,
                    userStatus.eligible,
                    "User should not be eligible (already joined/pending)",
                )
                assertFalse(
                    userStatus.reasons!!.isEmpty(),
                    "Reasons for ineligibility should be present",
                )
                // Check for the specific reason code
                assertTrue(
                    userStatus.reasons!!.any {
                        it.code == EligibilityRejectReasonCodeDTO.ALREADY_PARTICIPATING
                    },
                    "Reason should include ALREADY_PARTICIPATING",
                )
                assertEquals(
                    false,
                    task.submittable,
                    "Should not be submittable before participant approval",
                )
            }
    }

    @Test
    @Order(111)
    fun `test get task 2 eligibility for team creator before approval`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/${taskIds[1]}")
                    .queryParam("queryJoinability", true)
                    .queryParam("querySubmittability", true)
                    .build()
            }
            .header("Authorization", "Bearer $teamCreatorToken") // Team creator checks
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                val task = response.data.task
                assertEquals(
                    TaskSubmitterTypeDTO.TEAM,
                    task.submitterType,
                    "Task type should be TEAM",
                )

                assertNotNull(
                    task.participationEligibility,
                    "Participation eligibility data should be present",
                )
                val eligibility = task.participationEligibility!!

                assertNull(eligibility.user, "User status should be null for TEAM task")
                assertNotNull(eligibility.teams, "Team status should be present for TEAM task")

                val teamStatusList = eligibility.teams!!
                // Find the status for the specific team
                val relevantTeamStatus = teamStatusList.find { it.team.id == teamId }
                assertNotNull(relevantTeamStatus, "Status for team $teamId not found")

                // The team has been added (Order 95) but not approved yet.
                // Therefore, the team is NOT eligible to join again.
                assertEquals(
                    false,
                    relevantTeamStatus!!.eligibility.eligible,
                    "Team should not be eligible (already joined/pending)",
                )
                assertFalse(
                    relevantTeamStatus.eligibility.reasons!!.isEmpty(),
                    "Reasons for team ineligibility should be present",
                )
                // Check for the specific reason code
                assertTrue(
                    relevantTeamStatus.eligibility.reasons!!.any {
                        it.code == EligibilityRejectReasonCodeDTO.ALREADY_PARTICIPATING
                    },
                    "Reason should include ALREADY_PARTICIPATING for the team",
                )

                // Team creator cannot submit individually, team not approved yet
                assertEquals(false, task.submittable, "Team creator cannot submit individually")
                assertTrue(
                    task.submittableAsTeam.isNullOrEmpty(),
                    "submittableAsTeam should be null or empty before team approval",
                )
            }
    }

    @Test
    @Order(115)
    fun `test submit task user fails before participant approval`() { // Renamed
        val taskIdToSubmit = taskIds[0]
        // Minimal valid submission content based on updated schema (test 40)
        val submissionContent = listOf(TaskSubmissionContentDTO(text = "Test submission"))

        webTestClient
            .post()
            .uri("/tasks/$taskIdToSubmit/participants/$participantTaskMembershipId/submissions")
            .header("Authorization", "Bearer $participantToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(submissionContent)
            .exchange()
            // Expect Forbidden because the participant membership isn't approved
            .expectStatus()
            .isForbidden
            // Expect an error DTO indicating permission denied or similar
            .expectBody<GenericErrorResponse>()
            .value { errorResponse ->
                assertEquals("AccessDeniedError", errorResponse.error.name) // Or similar error name
            }
    }

    @Test
    @Order(120)
    fun `test approve participant 1 using bulk endpoint`() { // Renamed
        // NOTE: Original test uses PATCH /tasks/{taskId}/participants?member=...
        // This endpoint seems non-standard for approving a *specific* membership ID.
        // The controller snippet provided earlier has `patchTaskMembershipByMember` which takes
        // `member` (user ID)
        // and `patchTaskParticipant` which takes `participantId` (membership ID).
        // Let's assume the controller correctly implements PATCH
        // /tasks/{taskId}/participants?member=... to update *that* user's membership.
        // Assuming PatchTaskMembershipRequestDTO is used by this endpoint too
        val requestDTO = PatchTaskMembershipRequestDTO(approved = ApproveTypeDTO.APPROVED)

        webTestClient
            .patch()
            .uri { builder ->
                builder
                    .path("/tasks/${taskIds[0]}/participants")
                    .queryParam("member", participant.userId)
                    .build()
            }
            .header("Authorization", "Bearer $spaceCreatorToken") // Task owner approves
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTaskMembershipByMember200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.participants)
                val approvedParticipant =
                    response.data.participants!!.find { it.member.id == participant.userId }
                assertNotNull(
                    approvedParticipant,
                    "Participant ${participant.userId} not found in response",
                )
                assertEquals(
                    ApproveTypeDTO.APPROVED,
                    approvedParticipant!!.approved,
                    "Participant should be approved",
                )
            }
    }

    @Test
    @Order(121)
    fun `test get task 1 eligibility for participant 1 after approval`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/${taskIds[0]}")
                    .queryParam("queryJoinability", true)
                    .queryParam("querySubmittability", true)
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                val task = response.data.task
                assertEquals(TaskSubmitterTypeDTO.USER, task.submitterType)

                assertNotNull(task.participationEligibility)
                val eligibility = task.participationEligibility!!

                assertNull(eligibility.teams)
                assertNotNull(eligibility.user)

                val userStatus = eligibility.user!!
                // Still not eligible to *join* again because already participating
                assertEquals(
                    false,
                    userStatus.eligible,
                    "User should still not be eligible to join again",
                )
                assertFalse(
                    userStatus.reasons!!.isEmpty(),
                    "Reasons for ineligibility should be present",
                )
                assertTrue(
                    userStatus.reasons!!.any {
                        it.code == EligibilityRejectReasonCodeDTO.ALREADY_PARTICIPATING
                    },
                    "Reason should include ALREADY_PARTICIPATING",
                )
                assertEquals(
                    true,
                    task.submittable,
                    "Should be submittable after participant approval",
                )
            }
    }

    @Test
    @Order(125)
    fun `test approve participant 2 using bulk endpoint`() { // Renamed
        val requestDTO = PatchTaskMembershipRequestDTO(approved = ApproveTypeDTO.APPROVED)

        webTestClient
            .patch()
            .uri { builder ->
                builder
                    .path("/tasks/${taskIds[0]}/participants")
                    .queryParam("member", participant2.userId)
                    .build()
            }
            .header("Authorization", "Bearer $spaceCreatorToken") // Task owner approves
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTaskMembershipByMember200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.participants)
                val approvedParticipant =
                    response.data.participants!!.find { it.member.id == participant2.userId }
                assertNotNull(
                    approvedParticipant,
                    "Participant ${participant2.userId} not found in response",
                )
                assertEquals(
                    ApproveTypeDTO.APPROVED,
                    approvedParticipant!!.approved,
                    "Participant 2 should be approved",
                )
            }
    }

    @Test
    @Order(128)
    fun `test approve participant team using bulk endpoint`() { // Renamed
        val requestDTO = PatchTaskMembershipRequestDTO(approved = ApproveTypeDTO.APPROVED)

        webTestClient
            .patch()
            .uri { builder ->
                builder
                    .path("/tasks/${taskIds[1]}/participants")
                    .queryParam("member", teamId)
                    .build()
            } // Use teamId as member
            .header("Authorization", "Bearer $spaceCreatorToken") // Task owner approves
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTaskMembershipByMember200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.participants)
                val approvedParticipant =
                    response.data.participants!!.find { it.member.id == teamId }
                assertNotNull(approvedParticipant, "Team participant $teamId not found in response")
                assertEquals(
                    ApproveTypeDTO.APPROVED,
                    approvedParticipant!!.approved,
                    "Team participant should be approved",
                )
            }
    }

    @Test
    @Order(129)
    fun `test get task 2 eligibility for team creator after approval`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/${taskIds[1]}") // Task 2 (Team Task)
                    .queryParam("queryJoinability", true)
                    .queryParam("querySubmittability", true)
                    .build()
            }
            .header("Authorization", "Bearer $teamCreatorToken") // Team creator checks
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                val task = response.data.task
                assertEquals(TaskSubmitterTypeDTO.TEAM, task.submitterType)

                assertNotNull(task.participationEligibility)
                val eligibility = task.participationEligibility!!

                assertNull(eligibility.user) // User status null for TEAM task
                assertNotNull(eligibility.teams) // Team status present

                val teamStatusList = eligibility.teams!!
                val relevantTeamStatus = teamStatusList.find { it.team.id == teamId }
                assertNotNull(relevantTeamStatus, "Status for team $teamId should be present")

                // Team is approved, so not eligible to *join* again.
                assertEquals(
                    false,
                    relevantTeamStatus!!.eligibility.eligible,
                    "Team should not be eligible to join again",
                )
                assertFalse(
                    relevantTeamStatus.eligibility.reasons!!.isEmpty(),
                    "Reasons should be present",
                )
                assertTrue(
                    relevantTeamStatus.eligibility.reasons!!.any {
                        it.code == EligibilityRejectReasonCodeDTO.ALREADY_PARTICIPATING
                    },
                    "Reason should include ALREADY_PARTICIPATING for the team",
                )

                assertNotNull(task.submittableAsTeam)
                assertFalse(
                    task.submittableAsTeam!!.isEmpty(),
                    "submittableAsTeam should not be empty after team approval",
                )
                assertTrue(
                    task.submittableAsTeam!!.any { it.id == teamId },
                    "Team ID $teamId should be in submittableAsTeam list",
                )
            }
    }

    private fun assertSubmissionDetails(
        submission: TaskSubmissionDTO?,
        expectedMemberId: IdType,
        expectedSubmitterId: IdType,
        expectedVersion: Int,
        expectedContent:
            List<Pair<String, Any>>, // Pair of title and expected value (text or attachmentId)
    ) {
        assertNotNull(submission, "Submission object should not be null")
        submission!!
        assertNotNull(submission.id, "Submission ID should exist")
        assertEquals(expectedMemberId, submission.member.id, "Member ID mismatch")
        assertEquals(expectedSubmitterId, submission.submitter.id, "Submitter ID mismatch")
        assertEquals(expectedVersion, submission.version, "Version mismatch")
        assertNotNull(submission.content, "Submission content list should exist")
        assertEquals(
            expectedContent.size,
            submission.content.size,
            "Submission content size mismatch",
        )

        expectedContent.forEach { (expectedTitle, expectedValue) ->
            val contentEntry = submission.content.find { it.title == expectedTitle }
            assertNotNull(contentEntry, "Content entry with title '$expectedTitle' not found")
            contentEntry!!
            when (contentEntry.type) {
                TaskSubmissionTypeDTO.TEXT -> {
                    assertTrue(expectedValue is String, "Expected value for TEXT should be String")
                    assertEquals(
                        expectedValue,
                        contentEntry.contentText,
                        "Text content mismatch for '$expectedTitle'",
                    )
                }

                TaskSubmissionTypeDTO.FILE -> {
                    assertTrue(
                        expectedValue is IdType,
                        "Expected value for FILE should be IdType (Attachment ID)",
                    )
                    assertEquals(
                        expectedValue,
                        contentEntry.contentAttachment?.id,
                        "Attachment ID mismatch for '$expectedTitle'",
                    )
                }

                else -> fail("Unexpected content type: ${contentEntry.type}")
            }
        }
    }

    @Test
    @Order(130)
    fun `test submit task user 1 first time`() { // Renamed
        val taskIdToSubmit = taskIds[0]
        // Task 1 schema was updated in test 40 to only have "Text Entry"
        val submissionContent =
            listOf(TaskSubmissionContentDTO(text = "This is a test submission."))
        val expectedContent = listOf("Text Entry" to "This is a test submission.")

        webTestClient
            .post()
            .uri("/tasks/$taskIdToSubmit/participants/$participantTaskMembershipId/submissions")
            .header("Authorization", "Bearer $participantToken") // Participant submits
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(submissionContent)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PostTaskSubmission200ResponseDTO>() // Expect submission response DTO
            .value { response ->
                assertSubmissionDetails(
                    response.data.submission,
                    expectedMemberId = participant.userId,
                    expectedSubmitterId = participant.userId,
                    expectedVersion = 1,
                    expectedContent = expectedContent,
                )
            }
    }

    @Test
    @Order(131)
    fun `test submit task user 2 first time`() { // Renamed
        val taskIdToSubmit = taskIds[0]
        val submissionContent =
            listOf(TaskSubmissionContentDTO(text = "This is user 2 submission."))
        val expectedContent = listOf("Text Entry" to "This is user 2 submission.")

        webTestClient
            .post()
            .uri("/tasks/$taskIdToSubmit/participants/$participant2TaskMembershipId/submissions")
            .header("Authorization", "Bearer $participantToken2") // Participant 2 submits
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(submissionContent)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PostTaskSubmission200ResponseDTO>()
            .value { response ->
                assertSubmissionDetails(
                    response.data.submission,
                    expectedMemberId = participant2.userId,
                    expectedSubmitterId = participant2.userId,
                    expectedVersion = 1,
                    expectedContent = expectedContent,
                )
            }
    }

    @Test
    @Order(140)
    fun `test submit task team first time`() { // Renamed
        val taskIdToSubmit = taskIds[1] // Task 2 (Team Task)
        // Task 2 schema has both TEXT and FILE entries
        val submissionText = "This is a team submission."
        val submissionContent =
            listOf(
                TaskSubmissionContentDTO(text = submissionText),
                TaskSubmissionContentDTO(attachmentId = attachmentId),
            )
        val expectedContent =
            listOf("Text Entry" to submissionText, "Attachment Entry" to attachmentId)

        webTestClient
            .post()
            .uri("/tasks/$taskIdToSubmit/participants/$teamTaskMembershipId/submissions")
            .header("Authorization", "Bearer $teamCreatorToken") // Team creator submits for team
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(submissionContent)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PostTaskSubmission200ResponseDTO>()
            .value { response ->
                assertSubmissionDetails(
                    response.data.submission,
                    expectedMemberId = teamId, // Member is the team
                    expectedSubmitterId = teamCreator.userId, // Submitter is the user who acted
                    expectedVersion = 1,
                    expectedContent = expectedContent,
                )
            }
    }

    @Test
    @Order(150)
    fun `test submit again fails when not resubmittable`() { // Renamed
        val taskIdToSubmit = taskIds[0] // Task 0 was set to resubmittable=false in test 40
        val submissionContent = listOf(TaskSubmissionContentDTO(text = "Trying to submit again."))

        webTestClient
            .post()
            .uri("/tasks/$taskIdToSubmit/participants/$participantTaskMembershipId/submissions")
            .header("Authorization", "Bearer $participantToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(submissionContent)
            .exchange()
            .expectStatus()
            .isBadRequest // Expect 400 Bad Request
            .expectBody<GenericErrorResponse>() // Expect error DTO
            .value { errorResponse ->
                assertEquals("TaskNotResubmittableError", errorResponse.error.name)
                assertNotNull(errorResponse.error.data)
                assertEquals(taskIdToSubmit, errorResponse.error.data!!.taskId)
            }
    }

    @Test
    @Order(160)
    fun `test update task to resubmittable fails for non-owner`() { // Renamed
        val taskIdToUpdate = taskIds[0]
        val requestDTO = PatchTaskRequestDTO(resubmittable = true)

        webTestClient
            .patch()
            .uri("/tasks/$taskIdToUpdate")
            .header(
                "Authorization",
                "Bearer $creatorToken",
            ) // Generic creator, not space/task owner
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isForbidden // Expect 403 Forbidden
            .expectBody<GenericErrorResponse>()
            .value { errorResponse ->
                assertEquals("AccessDeniedError", errorResponse.error.name) // Or AccessDeniedError
            }
    }

    @Test
    @Order(161)
    fun `test update task to resubmittable succeeds for owner`() { // Renamed
        val taskIdToUpdate = taskIds[0]
        val requestDTO = PatchTaskRequestDTO(resubmittable = true)

        webTestClient
            .patch()
            .uri("/tasks/$taskIdToUpdate")
            .header("Authorization", "Bearer $spaceCreatorToken") // Space creator owns the task
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                assertEquals(true, response.data.task.resubmittable)
            }
    }

    @Test
    @Order(170)
    fun `test resubmit task user 1 succeeds after update`() { // Renamed
        val taskIdToSubmit = taskIds[0]
        val submissionText = "This is a test submission. (Version 2)"
        val submissionContent = listOf(TaskSubmissionContentDTO(text = submissionText))
        val expectedContent = listOf("Text Entry" to submissionText)

        webTestClient
            .post()
            .uri("/tasks/$taskIdToSubmit/participants/$participantTaskMembershipId/submissions")
            .header("Authorization", "Bearer $participantToken") // Participant resubmits
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(submissionContent)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PostTaskSubmission200ResponseDTO>()
            .value { response ->
                assertSubmissionDetails(
                    response.data.submission,
                    expectedMemberId = participant.userId,
                    expectedSubmitterId = participant.userId,
                    expectedVersion = 2, // Expect version 2
                    expectedContent = expectedContent,
                )
            }
    }

    @Test
    @Order(180)
    fun `test update submission fails when not editable`() { // Renamed
        val taskIdToUpdate = taskIds[0] // Task 0 was set to editable=false in test 40
        val submissionVersion = 1 // Try to edit version 1
        val updatedContent = listOf(TaskSubmissionContentDTO(text = "Editing version 1"))

        webTestClient
            .patch()
            .uri(
                "/tasks/$taskIdToUpdate/participants/$participantTaskMembershipId/submissions/$submissionVersion"
            )
            .header("Authorization", "Bearer $participantToken") // Participant tries to edit
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updatedContent)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody<GenericErrorResponse>()
            .value { errorResponse ->
                assertEquals("TaskSubmissionNotEditableError", errorResponse.error.name)
                assertNotNull(errorResponse.error.data)
                assertEquals(taskIdToUpdate, errorResponse.error.data!!.taskId)
            }
    }

    @Test
    @Order(185)
    fun `test update task to editable succeeds for owner`() { // Renamed
        val taskIdToUpdate = taskIds[0]
        val requestDTO = PatchTaskRequestDTO(editable = true)

        webTestClient
            .patch()
            .uri("/tasks/$taskIdToUpdate")
            .header("Authorization", "Bearer $spaceCreatorToken") // Space creator owns the task
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                assertEquals(true, response.data.task.editable)
            }
    }

    @Test
    @Order(190)
    fun `test update submission succeeds after task made editable`() { // Renamed
        val taskIdToUpdate = taskIds[0]
        val submissionVersion = 2 // Edit the latest version (v2 from test 170)
        val updatedText = "This is a test submission. (Version 2) (edited)"
        val updatedContent = listOf(TaskSubmissionContentDTO(text = updatedText))
        val expectedContent = listOf("Text Entry" to updatedText)

        webTestClient
            .patch()
            .uri(
                "/tasks/$taskIdToUpdate/participants/$participantTaskMembershipId/submissions/$submissionVersion"
            )
            .header("Authorization", "Bearer $participantToken") // Participant edits
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updatedContent)
            .exchange()
            .expectStatus()
            .isOk
            // Assuming PATCH submission returns PostTaskSubmission200ResponseDTO
            .expectBody<PostTaskSubmission200ResponseDTO>()
            .value { response ->
                // Assert details of the edited submission (still version 2)
                assertSubmissionDetails(
                    response.data.submission,
                    expectedMemberId = participant.userId,
                    expectedSubmitterId = participant.userId,
                    expectedVersion = 2,
                    expectedContent = expectedContent,
                )
            }
    }

    @Test
    @Order(198)
    fun `test get submissions fails for irrelevant user`() { // Renamed
        webTestClient
            .get()
            .uri("/tasks/${taskIds[0]}/participants/$participantTaskMembershipId/submissions")
            .header("Authorization", "Bearer $irrelevantUserToken") // Irrelevant user attempts
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<GenericErrorResponse>()
            .value { errorResponse -> assertEquals("AccessDeniedError", errorResponse.error.name) }
    }

    @Test
    @Order(199)
    fun `test get submissions fails for different participant`() { // Renamed
        // Participant 1 tries to get submissions for Participant 2's membership ID
        webTestClient
            .get()
            .uri("/tasks/${taskIds[0]}/participants/$participant2TaskMembershipId/submissions")
            .header("Authorization", "Bearer $participantToken") // Participant 1 token
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<GenericErrorResponse>()
            .value { errorResponse -> assertEquals("AccessDeniedError", errorResponse.error.name) }
    }

    @Test
    @Order(200)
    fun `test get submissions default (latest) for owner`() { // Renamed
        val expectedContent =
            listOf("Text Entry" to "This is a test submission. (Version 2) (edited)")

        webTestClient
            .get()
            .uri("/tasks/${taskIds[0]}/participants/$participantTaskMembershipId/submissions")
            .header("Authorization", "Bearer $spaceCreatorToken") // Owner/creator gets submissions
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskSubmissions200ResponseDTO>() // Expect list DTO
            .value { response ->
                assertNotNull(response.data.submissions)
                val submissions = response.data.submissions
                assertEquals(1, submissions.size, "Should only get latest version by default")
                assertSubmissionDetails(
                    submissions[0],
                    expectedMemberId = participant.userId,
                    expectedSubmitterId = participant.userId,
                    expectedVersion = 2, // Latest version
                    expectedContent = expectedContent,
                )
            }
    }

    @Test
    @Order(210)
    fun `test get submissions with all versions`() { // Renamed
        val expectedContentV2 =
            listOf("Text Entry" to "This is a test submission. (Version 2) (edited)")
        val expectedContentV1 =
            listOf("Text Entry" to "This is a test submission.") // From test 130

        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path(
                        "/tasks/${taskIds[0]}/participants/$participantTaskMembershipId/submissions"
                    )
                    .queryParam("allVersions", true)
                    .build()
            }
            .header("Authorization", "Bearer $spaceCreatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskSubmissions200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submissions)
                val submissions = response.data.submissions
                assertEquals(2, submissions.size, "Should get both versions")

                // Submissions usually ordered descending by version/time
                val submissionV2 = submissions.find { it.version == 2 }
                val submissionV1 = submissions.find { it.version == 1 }

                assertNotNull(submissionV2, "Version 2 not found")
                assertSubmissionDetails(
                    submissionV2,
                    expectedMemberId = participant.userId,
                    expectedSubmitterId = participant.userId,
                    expectedVersion = 2,
                    expectedContent = expectedContentV2,
                )

                assertNotNull(submissionV1, "Version 1 not found")
                assertSubmissionDetails(
                    submissionV1,
                    expectedMemberId = participant.userId,
                    expectedSubmitterId =
                        participant.userId, // Submitter was participant for V1 too
                    expectedVersion = 1,
                    expectedContent = expectedContentV1,
                )
            }
    }

    @Test
    @Order(218)
    fun `test get submissions fails for different participant via member query param`() { // Renamed
        // Participant 2 tries to get Participant 1's submissions using member ID query
        webTestClient
            .get()
            // NOTE: The path uses participant 1's membership ID, but the query targets participant
            // 1 user ID.
            // Access control should likely deny this based on the token (Participant 2).
            .uri { builder ->
                builder
                    .path(
                        "/tasks/${taskIds[0]}/participants/$participantTaskMembershipId/submissions"
                    )
                    .queryParam("member", participant.userId)
                    .build()
            }
            .header("Authorization", "Bearer $participantToken2") // Participant 2 token
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<GenericErrorResponse>()
            .value { errorResponse -> assertEquals("AccessDeniedError", errorResponse.error.name) }
    }

    @Test
    @Order(219)
    fun `test get submissions succeeds for self via member query param`() { // Renamed
        val expectedContent =
            listOf("Text Entry" to "This is a test submission. (Version 2) (edited)")
        // Participant 1 gets their own submissions using member ID query param
        webTestClient
            .get()
            // Path still uses their membership ID, query param redundantly specifies their user ID.
            .uri { builder ->
                builder
                    .path(
                        "/tasks/${taskIds[0]}/participants/$participantTaskMembershipId/submissions"
                    )
                    .queryParam("member", participant.userId)
                    .build()
            }
            .header("Authorization", "Bearer $participantToken") // Participant 1 token
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskSubmissions200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submissions)
                val submissions = response.data.submissions
                assertEquals(
                    1,
                    submissions.size,
                    "Should get latest version",
                ) // Default is latest only
                assertSubmissionDetails(
                    submissions[0],
                    expectedMemberId = participant.userId,
                    expectedSubmitterId = participant.userId,
                    expectedVersion = 2,
                    expectedContent = expectedContent,
                )
                // Check that participant 2's submissions are not included (though unlikely given
                // the path)
                assertFalse(
                    submissions.any { it.member.id == participant2.userId },
                    "Should not include participant 2's submissions",
                )
            }
    }

    @Test
    @Order(220)
    fun `test get submissions succeeds for owner via participantId path`() { // Renamed
        val expectedContent =
            listOf("Text Entry" to "This is a test submission. (Version 2) (edited)")
        // Owner gets participant 1's submissions via path param
        webTestClient
            .get()
            .uri("/tasks/${taskIds[0]}/participants/$participantTaskMembershipId/submissions")
            .header("Authorization", "Bearer $spaceCreatorToken") // Owner token
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskSubmissions200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submissions)
                val submissions = response.data.submissions
                assertEquals(1, submissions.size) // Default latest
                assertSubmissionDetails(
                    submissions[0],
                    expectedMemberId = participant.userId,
                    expectedSubmitterId = participant.userId,
                    expectedVersion = 2,
                    expectedContent = expectedContent,
                )
                // Check participant 2 not included
                assertFalse(
                    submissions.any { it.member.id == participant2.userId },
                    "Should not include participant 2's submissions",
                )
            }
    }

    @Test
    @Order(230)
    fun `test delete task fails for non-owner`() { // Renamed
        val taskIdToDelete = taskIds[1] // Team task created by spaceCreator

        webTestClient
            .delete()
            .uri("/tasks/$taskIdToDelete")
            .header("Authorization", "Bearer $creatorToken") // Generic creator token, not owner
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<GenericErrorResponse>()
            .value { errorResponse ->
                assertEquals("AccessDeniedError", errorResponse.error.name) // Or AccessDeniedError
            }
    }

    @Test
    @Order(240)
    fun `test delete task succeeds for owner`() { // Renamed
        val taskIdToDelete = taskIds[1]

        webTestClient
            .delete()
            .uri("/tasks/$taskIdToDelete")
            .header("Authorization", "Bearer $spaceCreatorToken") // Owner deletes
            .exchange()
            .expectStatus()
            .isNoContent

        // Verify deletion
        webTestClient
            .get()
            .uri("/tasks/$taskIdToDelete")
            .header("Authorization", "Bearer $spaceCreatorToken")
            .exchange()
            .expectStatus()
            .isNotFound // Should be gone
    }
}
