/*
 *  Description: It tests the feature of tasks.
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
class TaskTest
@Autowired
constructor(
    private val webTestClient: WebTestClient,
    private val userCreatorService: UserCreatorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // --- User Setup ---
    private lateinit var creator: UserCreatorService.CreateUserResponse
    private lateinit var creatorToken: String
    private lateinit var teamCreator: UserCreatorService.CreateUserResponse
    private lateinit var teamCreatorToken: String
    private lateinit var teamMember: UserCreatorService.CreateUserResponse
    private lateinit var teamMemberToken: String
    private lateinit var spaceCreator: UserCreatorService.CreateUserResponse
    private lateinit var spaceCreatorToken: String
    private lateinit var participant: UserCreatorService.CreateUserResponse
    private lateinit var participantToken: String
    private lateinit var participant2: UserCreatorService.CreateUserResponse
    private lateinit var participantToken2: String
    private lateinit var participant3: UserCreatorService.CreateUserResponse
    private lateinit var participantToken3: String
    private lateinit var participant4: UserCreatorService.CreateUserResponse
    private lateinit var participantToken4: String
    private lateinit var spaceAdmin: UserCreatorService.CreateUserResponse
    private lateinit var spaceAdminToken: String
    private lateinit var teamAdmin: UserCreatorService.CreateUserResponse
    private lateinit var teamAdminToken: String

    // --- Resource IDs ---
    private var teamId: IdType = -1
    private var spaceId: IdType = -1
    private var defaultCategoryId: IdType = -1
    private var customCategoryId: IdType = -1
    private var archivedCategoryId: IdType = -1
    private val taskIds = mutableListOf<IdType>()

    // --- Task Details ---
    private val randomSuffix = floor(Math.random() * 10000000000).toLong()
    private val taskNamePrefix = "Test Task ($randomSuffix)"
    private val taskIntro = "This is a test task."
    private val taskDescription = "A lengthy text. ".repeat(100)
    private val taskDeadline =
        LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val taskDefaultDeadline = 30L
    private val taskMembershipDeadline =
        LocalDateTime.now().plusMonths(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val taskSubmissionSchema =
        listOf(
            TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT),
            TaskSubmissionSchemaEntryDTO("Attachment Entry", TaskSubmissionTypeDTO.FILE),
        )

    // --- Membership IDs ---
    private var participantTaskMembershipId: IdType = -1
    private var participant2TaskMembershipId: IdType = -1 // Will be assigned later if needed
    private var participant3TaskMembershipId: IdType = -1
    private var participant4TaskMembershipId: IdType = -1
    private var teamTaskMembershipId: IdType = -1

    // --- Helper DTO for Error Responses ---
    data class ErrorData(
        val type: String? = null,
        val id: Any? = null,
        val name: String? = null,
        val action: String? = null,
        val resourceType: String? = null,
        val resourceId: IdType? = null,
    )

    data class ErrorDetail(val name: String, val data: ErrorData?)

    data class GenericErrorResponse(val error: ErrorDetail)

    // --- Refactored Helper Functions ---

    /** Creates a Space and returns its ID and the ID of its default category. */
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

    /** Adds a user as an admin to a space. Uses original endpoint /managers. */
    fun addSpaceAdmin(creatorToken: String, spaceId: IdType, adminId: IdType) {
        val requestDTO =
            PostSpaceAdminRequestDTO(role = SpaceAdminRoleTypeDTO.ADMIN, userId = adminId)
        webTestClient
            .post()
            .uri("/spaces/$spaceId/managers")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
    }

    /** Creates a Team and returns its ID. */
    fun createTeam(
        creatorToken: String,
        teamName: String,
        teamIntro: String,
        teamDescription: String,
        teamAvatarId: IdType,
    ): IdType {
        val requestDTO =
            PostTeamRequestDTO(
                name = teamName,
                intro = teamIntro,
                description = teamDescription,
                avatarId = teamAvatarId,
            )
        val response =
            webTestClient
                .post()
                .uri("/teams")
                .header("Authorization", "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<GetTeam200ResponseDTO>()
                .returnResult()
                .responseBody

        assertNotNull(response?.data?.team, "Team data missing in response")
        val createdTeamId = response!!.data.team.id
        assertNotNull(createdTeamId, "Created team ID is null")
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
            .isNoContent()
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

    /** Adds a user to a team as an admin. */
    fun addTeamAdmin(creatorToken: String, teamId: IdType, adminId: IdType, adminToken: String) {
        // Step 1: Team creator sends invitation to user with admin role
        val invitationRequestDTO =
            TeamInvitationCreateDTO(
                userId = adminId,
                role = TeamMemberRoleTypeDTO.ADMIN,
                message = "Please join as an admin",
            )

        val invitationResponse =
            webTestClient
                .post()
                .uri("/teams/$teamId/invitations")
                .header("Authorization", "Bearer $creatorToken") // Team creator sends invitation
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invitationRequestDTO)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody<CreateTeamInvitation201ResponseDTO>()
                .returnResult()
                .responseBody

        assertNotNull(invitationResponse?.data?.invitation, "Invitation data missing")
        val invitationId = invitationResponse!!.data.invitation.id

        // Step 2: User accepts the admin invitation
        webTestClient
            .post()
            .uri("/users/me/team-invitations/$invitationId/accept")
            .header("Authorization", "Bearer $adminToken") // Admin accepts invitation
            .exchange()
            .expectStatus()
            .isNoContent
    }

    @BeforeAll
    fun prepare() {
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
        participant3 = userCreatorService.createUser()
        participantToken3 = userCreatorService.login(participant3.username, participant3.password)
        participant4 = userCreatorService.createUser()
        participantToken4 = userCreatorService.login(participant4.username, participant4.password)
        spaceAdmin = userCreatorService.createUser()
        spaceAdminToken = userCreatorService.login(spaceAdmin.username, spaceAdmin.password)
        teamAdmin = userCreatorService.createUser()
        teamAdminToken = userCreatorService.login(teamAdmin.username, teamAdmin.password)

        // Create space and get default category ID
        val spaceResult =
            createSpace(
                creatorToken = spaceCreatorToken,
                spaceName = "Test Space ($randomSuffix)",
                spaceIntro = "This is a test space.",
                spaceDescription = "A lengthy text. ".repeat(100),
                spaceAvatarId = userCreatorService.testAvatarId(),
            )
        spaceId = spaceResult.first
        defaultCategoryId = spaceResult.second

        // Create team
        teamId =
            createTeam(
                creatorToken = teamCreatorToken,
                teamName = "Test Team ($randomSuffix)",
                teamIntro = "This is a test team.",
                teamDescription = "A lengthy text. ".repeat(100),
                teamAvatarId = userCreatorService.testAvatarId(),
            )

        // Setup memberships/admins
        joinTeamWithApproval(
            teamMemberToken,
            teamCreatorToken,
            teamId,
            teamMember.userId,
        ) // Team creator adds member
        addSpaceAdmin(spaceCreatorToken, spaceId, spaceAdmin.userId) // Space creator adds admin
        addTeamAdmin(
            teamCreatorToken,
            teamId,
            teamAdmin.userId,
            teamAdminToken,
        ) // Team creator adds admin
    }

    /** Creates a Task and returns its ID, or null if creation fails as expected. */
    fun createTask(
        token: String = creatorToken,
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
                topics = emptyList(), // Assuming default empty list if not provided
                rank = null, // Assuming default null if not provided
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
                .isEqualTo(expectedStatus) // Check expected status

        if (expectedStatus == HttpStatus.OK.value()) {
            val responseBody =
                responseSpec
                    .expectBody<
                        PatchTask200ResponseDTO
                    >() // Assuming POST returns PATCH DTO on success
                    .returnResult()
                    .responseBody

            assertNotNull(responseBody?.data?.task, "Task data missing in successful response")
            val task = responseBody!!.data.task

            // --- DTO Assertions ---
            assertEquals(name, task.name)
            assertEquals(
                submitterType.toDTO(),
                task.submitterType,
            ) // Compare with string or enum value
            // Creator check needs adjustment based on who performs the action (passed in `token`)
            // assertEquals(creator.userId, task.creator?.id) // This assumes creatorToken is always
            // used
            assertNotNull(task.creator.id, "Task creator ID should not be null")
            assertEquals(deadline, task.deadline)
            assertEquals(defaultDeadline, task.defaultDeadline)
            assertEquals(resubmittable, task.resubmittable)
            assertEquals(editable, task.editable)
            assertEquals(intro, task.intro)
            assertEquals(description, task.description)
            assertEquals(spaceId, task.space?.id)
            assertEquals(categoryId ?: defaultCategoryId, task.category?.id) // Check category ID
            assertNotNull(task.category?.name, "Category name should exist")
            assertEquals(ApproveTypeDTO.NONE, task.approved) // Expect initial state NONE

            // Schema validation using DTO
            assertNotNull(task.submissionSchema)
            assertEquals(submissionSchema.size, task.submissionSchema.size)
            submissionSchema.forEach { expectedEntry ->
                assertTrue(
                    task.submissionSchema.any { actualEntry ->
                        actualEntry.prompt == expectedEntry.prompt &&
                            actualEntry.type == expectedEntry.type
                    },
                    "Submission schema entry '${expectedEntry.prompt}' with type '${expectedEntry.type}' not found or type mismatch",
                )
            }

            val createdTaskId = task.id
            assertNotNull(createdTaskId, "Task ID should not be null")
            taskIds.add(createdTaskId)
            logger.info(
                "Created task: $createdTaskId (Type: $submitterType, Space: $spaceId, Category: ${task.category?.id})"
            )
            return createdTaskId
        }
        // For non-OK expected status, just return null as original function did implicitly
        return null
    }

    /** Approves a task. */
    fun approveTask(taskId: IdType, token: String) {
        val requestDTO = PatchTaskRequestDTO(approved = ApproveTypeDTO.APPROVED)
        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                assertEquals(ApproveTypeDTO.APPROVED, response.data.task.approved)
            }
    }

    /** Adds a user participant to a task. */
    fun addParticipantUser(token: String, taskId: IdType, userId: IdType): IdType {
        val requestDTO = PostTaskParticipantRequestDTO(email = "test@example.com")
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
                .expectBody<PostTaskParticipant200ResponseDTO>()
                .returnResult()
                .responseBody

        assertNotNull(response?.data?.participant, "Participant data missing")
        val membershipId = response!!.data.participant!!.id
        assertNotNull(membershipId, "Membership ID is null")
        return membershipId
    }

    /** Adds a team participant to a task. */
    fun addParticipantTeam(token: String, taskId: IdType, teamId: IdType): IdType {
        val requestDTO = PostTaskParticipantRequestDTO(email = "test@example.com")
        val response =
            webTestClient
                .post()
                .uri { builder ->
                    builder.path("/tasks/$taskId/participants").queryParam("member", teamId).build()
                }
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<PostTaskParticipant200ResponseDTO>()
                .returnResult()
                .responseBody

        assertNotNull(response?.data?.participant, "Participant data missing")
        val membershipId = response!!.data.participant!!.id
        assertNotNull(membershipId, "Membership ID is null")
        return membershipId
    }

    /** Approves a task participant (membership). */
    fun approveTaskParticipant(token: String, taskId: IdType, participantMembershipId: IdType) {
        val requestDTO = PatchTaskMembershipRequestDTO(approved = ApproveTypeDTO.APPROVED)
        webTestClient
            .patch()
            .uri("/tasks/$taskId/participants/$participantMembershipId")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskParticipant200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.taskMembership)
                assertEquals(ApproveTypeDTO.APPROVED, response.data.taskMembership.approved)
            }
    }

    /** Creates a category within a space. */
    fun createCategory(
        token: String,
        spaceId: IdType,
        name: String,
        description: String? = null,
        displayOrder: Int? = null,
    ): IdType {
        val requestDTO =
            CreateSpaceCategoryRequestDTO(
                name = name,
                description = description,
                displayOrder = displayOrder,
            )
        val response =
            webTestClient
                .post()
                .uri("/spaces/$spaceId/categories")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody<CreateSpaceCategory201ResponseDTO>()
                .returnResult()
                .responseBody

        assertNotNull(response?.data?.category, "Category data missing")
        assertEquals(name, response!!.data!!.category.name)
        val categoryId = response.data!!.category.id
        assertNotNull(categoryId, "Category ID is null")
        logger.info("Created category '$name' (ID: $categoryId) in space $spaceId")
        return categoryId
    }

    /** Archives a category. */
    fun archiveCategory(token: String, spaceId: IdType, categoryId: IdType) {
        webTestClient
            .post()
            .uri("/spaces/$spaceId/categories/$categoryId/archive")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<
                CreateSpaceCategory201ResponseDTO
            >() // Assuming archive returns updated Category DTO
            .value { response ->
                assertNotNull(response.data?.category)
                assertEquals(categoryId, response.data!!.category.id)
                assertNotNull(response.data!!.category.archivedAt)
            }
        logger.info("Archived category $categoryId in space $spaceId")
    }

    /** Unarchives a category. */
    fun unarchiveCategory(token: String, spaceId: IdType, categoryId: IdType) {
        webTestClient
            .delete()
            .uri("/spaces/$spaceId/categories/$categoryId/archive")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<
                CreateSpaceCategory201ResponseDTO
            >() // Assuming unarchive returns updated Category DTO
            .value { response ->
                assertNotNull(response.data?.category)
                assertEquals(categoryId, response.data!!.category.id)
                assertNull(response.data!!.category.archivedAt)
            }
        logger.info("Unarchived category $categoryId in space $spaceId")
    }

    // --- Test Methods ---

    @Test
    @Order(5)
    fun `Category - Setup custom and archived categories`() {
        customCategoryId =
            createCategory(spaceAdminToken, spaceId, "Custom Category", "A specific category")
        archivedCategoryId =
            createCategory(
                spaceAdminToken,
                spaceId,
                "Archived Category",
                "This one will be archived",
            )
        archiveCategory(spaceAdminToken, spaceId, archivedCategoryId)
        assertTrue(customCategoryId > 0)
        assertTrue(archivedCategoryId > 0)
    }

    @Test
    @Order(6)
    fun `Category - List active categories`() {
        webTestClient
            .get()
            .uri("/spaces/$spaceId/categories")
            .header("Authorization", "Bearer $spaceAdminToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListSpaceCategories200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.categories)
                val categories = response.data!!.categories
                assertEquals(2, categories.size, "Expected 2 active categories (Default, Custom)")
                assertTrue(
                    categories.any { it.id == defaultCategoryId && it.name == "General" }
                ) // Assuming default is "General"
                assertTrue(
                    categories.any { it.id == customCategoryId && it.name == "Custom Category" }
                )
                assertFalse(categories.any { it.id == archivedCategoryId })
            }
    }

    @Test
    @Order(7)
    fun `Category - List all categories (including archived)`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/spaces/$spaceId/categories")
                    .queryParam("includeArchived", "true")
                    .build()
            }
            .header("Authorization", "Bearer $spaceAdminToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListSpaceCategories200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.categories)
                val categories = response.data!!.categories
                assertEquals(
                    3,
                    categories.size,
                    "Expected 3 categories (Default, Custom, Archived)",
                )
                val archived = categories.find { it.id == archivedCategoryId }
                assertNotNull(archived)
                assertNotNull(archived!!.archivedAt)
            }
    }

    @Test
    @Order(10)
    fun `Task - Create tasks`() { // Renamed for clarity
        // Task 1
        createTask(
            token = creatorToken, // Explicitly use creator token
            name = "$taskNamePrefix (1 - USER, Default Cat)",
            submitterType = TaskSubmitterType.USER, // Use string or enum if available
            deadline = taskDeadline,
            defaultDeadline = taskDefaultDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            spaceId = spaceId,
            categoryId = null, // Uses default
        )
        // Task 2
        createTask(
            token = creatorToken,
            name = "$taskNamePrefix (2 - TEAM, Custom Cat)",
            submitterType = TaskSubmitterType.TEAM, // Use string or enum if available
            deadline = taskDeadline,
            defaultDeadline = taskDefaultDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            spaceId = spaceId,
            categoryId = customCategoryId,
        )
        // Task 3
        createTask(
            token = creatorToken,
            name = "$taskNamePrefix (3 - USER, Default Cat)",
            submitterType = TaskSubmitterType.USER,
            deadline = taskDeadline,
            defaultDeadline = taskDefaultDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            spaceId = spaceId,
            categoryId = null,
        )
        // Task 4
        createTask(
            token = creatorToken,
            name = "$taskNamePrefix (4 - USER, Custom Cat)",
            submitterType = TaskSubmitterType.USER,
            deadline = taskDeadline,
            defaultDeadline = taskDefaultDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            spaceId = spaceId,
            categoryId = customCategoryId,
        )
        assertEquals(4, taskIds.size, "Should have created 4 tasks")
    }

    @Test
    @Order(11)
    fun `Task - Try creating task in archived category (should fail)`() {
        createTask(
            token = creatorToken,
            name = "$taskNamePrefix (Should Fail)",
            submitterType = TaskSubmitterType.USER,
            deadline = taskDeadline,
            defaultDeadline = taskDefaultDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            spaceId = spaceId,
            categoryId = archivedCategoryId,
            expectedStatus = HttpStatus.BAD_REQUEST.value(),
        )
    }

    @Test
    @Order(13)
    fun `Task - Enumerate tasks owned by creator`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks")
                    .queryParam("space", spaceId)
                    .queryParam("owner", creator.userId)
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken") // Creator requests their owned tasks
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTasks200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.tasks)
                assertEquals(4, response.data!!.tasks!!.size)
                assertTrue(response.data!!.tasks!!.all { it.creator.id == creator.userId })
            }
    }

    @Test
    @Order(14)
    fun `Task - Enumerate unapproved tasks as space admin`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks")
                    .queryParam("space", spaceId)
                    .queryParam(
                        "approved",
                        ApproveTypeDTO.NONE.name,
                    ) // Use enum name for query param
                    .build()
            }
            .header("Authorization", "Bearer $spaceAdminToken") // Admin checks unapproved
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTasks200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.tasks)
                // Initially, all 4 tasks are NONE
                assertEquals(4, response.data!!.tasks!!.size)
                assertTrue(response.data!!.tasks!!.all { it.approved == ApproveTypeDTO.NONE })
            }
    }

    @Test
    @Order(15)
    fun `Task - Enumerate unapproved tasks fails for non-admin`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks")
                    .queryParam("space", spaceId)
                    .queryParam("approved", ApproveTypeDTO.NONE.name)
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken") // Non-admin tries
            .exchange()
            .expectStatus()
            .isForbidden // Expect 403
            // Optionally check error response DTO
            .expectBody<GenericErrorResponse>()
            .value { error -> assertEquals("AccessDeniedError", error.error.name) }
    }

    @Test
    @Order(16)
    fun `Task - Get unapproved task as space admin`() { // Renamed
        val taskId = taskIds[3] // Task 4
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId")
                    .queryParam("queryJoinability", "true")
                    .queryParam("querySubmittability", "true")
                    .build()
            }
            .header("Authorization", "Bearer $spaceAdminToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                val task = response.data.task
                assertEquals(taskId, task.id)
                assertEquals("$taskNamePrefix (4 - USER, Custom Cat)", task.name)
                assertEquals(ApproveTypeDTO.NONE, task.approved)
            }
    }

    @Test
    @Order(17)
    fun `Task - Get unapproved task fails for non-admin participant`() { // Renamed
        val taskId = taskIds[3] // Task 4
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId")
                    .queryParam("queryJoinability", "true")
                    .queryParam("querySubmittability", "true")
                    .build()
            }
            .header("Authorization", "Bearer $participantToken") // Participant tries
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<GenericErrorResponse>()
            .value { error -> assertEquals("AccessDeniedError", error.error.name) }
    }

    @Test
    @Order(18)
    fun `Task - Join unapproved task fails`() { // Renamed
        val taskId = taskIds[3] // Task 4
        val requestDTO = PostTaskParticipantRequestDTO(email = "test@example.com")
        webTestClient
            .post()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants")
                    .queryParam("member", participant.userId)
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<GenericErrorResponse>()
            .value { error ->
                assertEquals("ForbiddenError", error.error.name)
            } // Assuming this specific error
    }

    @Test
    @Order(19) // Keep original order
    fun `Task - Approve some tasks as space admin`() { // Renamed
        approveTask(taskIds[0], spaceAdminToken) // Approve Task 1
        approveTask(taskIds[1], spaceAdminToken) // Approve Task 2
        approveTask(taskIds[2], spaceAdminToken) // Approve Task 3
        // Task 4 (taskIds[3]) remains unapproved
    }

    @Test
    @Order(19) // Keep original order, adjust name slightly
    fun `Task - Approve task fails for non-admin creator`() { // Renamed
        val taskId = taskIds[3] // Target unapproved Task 4
        val requestDTO = PatchTaskRequestDTO(approved = ApproveTypeDTO.APPROVED)
        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $creatorToken") // Non-admin creator tries
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<GenericErrorResponse>()
            .value { error -> assertEquals("AccessDeniedError", error.error.name) }

        // Verify task 3 remains approved (original test logic)
        webTestClient
            .get()
            .uri("/tasks/${taskIds[2]}")
            .header("Authorization", "Bearer $spaceAdminToken") // Use admin to check
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                assertEquals(ApproveTypeDTO.APPROVED, response.data.task.approved)
            }
    }

    @Test
    @Order(20)
    fun `Task - Get approved task details`() { // Renamed
        val taskId = taskIds[0] // Task 1 (USER, Default Cat), approved
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId")
                    .queryParam("querySpace", "true")
                    .build() // Query category info implicitly included by default or via TaskDTO
                // structure
            }
            .header("Authorization", "Bearer $creatorToken") // Creator gets own task
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                val task = response.data.task
                assertEquals(taskId, task.id)
                assertEquals("$taskNamePrefix (1 - USER, Default Cat)", task.name)
                assertEquals(
                    TaskSubmitterTypeDTO.USER,
                    task.submitterType,
                ) // or TaskSubmitterTypeDTO.USER.name
                assertEquals(creator.userId, task.creator.id)
                assertEquals(taskDeadline, task.deadline)
                assertEquals(spaceId, task.space?.id)
                assertEquals(defaultCategoryId, task.category?.id)
                assertEquals("General", task.category?.name) // Assuming default name
                assertEquals(ApproveTypeDTO.APPROVED, task.approved)

                // Schema check
                assertNotNull(task.submissionSchema)
                assertEquals(taskSubmissionSchema.size, task.submissionSchema.size)
                taskSubmissionSchema.forEach { expectedEntry ->
                    assertTrue(
                        task.submissionSchema.any { actualEntry ->
                            actualEntry.prompt == expectedEntry.prompt &&
                                actualEntry.type == expectedEntry.type
                        }
                    )
                }
            }
    }

    @Test
    @Order(21)
    fun `Task - Patch reject reason fails for non-admin`() { // Renamed
        val taskId = taskIds[2] // Task 3, approved
        val requestDTO = PatchTaskRequestDTO(rejectReason = "Garbage.")
        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $creatorToken") // Non-admin creator
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<GenericErrorResponse>()
            .value { error -> assertEquals("AccessDeniedError", error.error.name) }
    }

    @Test
    @Order(22)
    fun `Task - Patch reject reason success for admin`() { // Renamed
        val taskId = taskIds[2]
        val reason = "Garbage."
        val requestDTO = PatchTaskRequestDTO(rejectReason = reason)
        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $spaceAdminToken") // Admin
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                assertEquals(reason, response.data.task.rejectReason)
            }
    }

    // --- Optional Query Param Tests ---

    // Helper function for asserting optional fields
    private fun assertTaskOptionalFields(task: TaskDTO?, participation: TaskParticipationInfoDTO?) {
        assertNotNull(task, "Task DTO is null")
        assertNotNull(participation, "Participation Info DTO is null")
        assertNotNull(task!!.joinable, "'joinable' field is null")
        assertNotNull(task.submittable, "'submittable' field is null")
        assertNotNull(task.joined, "'joined' field is null on participation info")
    }

    @Test
    @Order(25)
    fun `Task - Get TEAM task with optional queries as team creator`() { // Renamed
        val taskId = taskIds[1] // Task 2 (TEAM, Custom Cat), approved
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId")
                    .queryParam("querySpace", "true")
                    // queryTeam likely implicit in TaskDTO if space is queried
                    .queryParam("queryJoinability", "true")
                    .queryParam("querySubmittability", "true")
                    .queryParam("queryJoined", "true")
                    .queryParam("queryUserDeadline", "true")
                    .build()
            }
            .header("Authorization", "Bearer $teamCreatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                assertNotNull(response.data.participation)
                val task = response.data.task
                val participation = response.data.participation

                assertEquals(spaceId, task.space?.id)
                assertEquals(customCategoryId, task.category?.id)

                assertTaskOptionalFields(task, participation) // Basic checks

                // Specific logic for TEAM task as Team Creator
                assertEquals(true, task.joinable) // Can join as a team
                assertNotNull(task.joinableTeams)
                assertTrue(task.joinableTeams!!.any { it.id == teamId }) // Can join as this team
                assertEquals(false, participation.hasParticipation) // Not joined yet
                assertTrue(task.joinedTeams.isNullOrEmpty()) // Not joined as any team yet
                assertEquals(false, task.submittable) // Not joined, so cannot submit
                assertTrue(task.submittableAsTeam.isNullOrEmpty()) // Cannot submit as any team yet
                assertNull(task.userDeadline) // No specific deadline set
            }
    }

    @Test
    @Order(26)
    fun `Task - Get TEAM task with optional queries as space creator (not team member)`() { // Renamed
        val taskId = taskIds[1]
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId")
                    .queryParam("querySpace", "true")
                    .queryParam("queryJoinability", "true")
                    .queryParam("querySubmittability", "true")
                    .queryParam("queryJoined", "true")
                    // queryUserDeadline is irrelevant if not joinable/joined
                    .build()
            }
            .header(
                "Authorization",
                "Bearer $spaceCreatorToken",
            ) // Space creator is not in the team
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                assertNotNull(response.data.participation)
                val task = response.data.task
                val participation = response.data.participation

                assertEquals(spaceId, task.space?.id)
                assertEquals(customCategoryId, task.category?.id)

                assertTaskOptionalFields(task, participation)

                // Specific logic for TEAM task as outsider
                assertEquals(false, task.joinable) // Cannot join as user
                assertTrue(task.joinableTeams.isNullOrEmpty()) // Not admin/member of eligible team
                assertEquals(false, task.joined)
                assertTrue(task.joinedTeams.isNullOrEmpty())
                assertEquals(false, task.submittable)
                assertTrue(task.submittableAsTeam.isNullOrEmpty())
            }
    }

    @Test
    @Order(27)
    fun `Task - Get USER task with optional queries as space creator`() { // Renamed
        val taskId = taskIds[0] // Task 1 (USER, Default Cat), approved
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId")
                    .queryParam("querySpace", "true")
                    .queryParam("queryJoinability", "true")
                    .queryParam("querySubmittability", "true")
                    .queryParam("queryJoined", "true")
                    .queryParam("queryUserDeadline", "true")
                    .build()
            }
            .header(
                "Authorization",
                "Bearer $spaceCreatorToken",
            ) // Anyone can potentially join USER task
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                assertNotNull(response.data.participation)
                val task = response.data.task
                val participation = response.data.participation

                assertEquals(spaceId, task.space?.id)
                assertEquals(defaultCategoryId, task.category?.id)

                assertTaskOptionalFields(task, participation)

                // Specific logic for USER task
                assertEquals(true, task.joinable) // Can join as user
                // joinableTeams might not exist or be empty for USER tasks, depending on DTO
                // definition
                // assertNull(task.joinableTeams) or assertTrue(task.joinableTeams.isNullOrEmpty())
                assertEquals(false, task.joined) // Not joined yet
                assertTrue(task.joinedTeams.isNullOrEmpty()) // joinedTeams irrelevant for user join
                assertEquals(false, task.submittable) // Not joined yet
                // submittableAsTeam might not exist or be empty for USER tasks
                // assertNull(task.submittableAsTeam) or
                // assertTrue(task.submittableAsTeam.isNullOrEmpty())
                assertNull(task.userDeadline)
            }
    }

    // --- PATCH Tests ---

    @Test
    @Order(30)
    fun `Task - Update with empty request`() { // Renamed
        val taskId = taskIds[0]
        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(emptyMap<String, Any>()) // Empty body
            .exchange()
            .expectStatus()
            .isOk
            // Optionally verify the response DTO shows no changes
            .expectBody<PatchTask200ResponseDTO>()
    }

    @Test
    @Order(40)
    fun `Task - Update with full request (including category)`() { // Renamed
        val taskId = taskIds[0]
        val updatedName = "$taskNamePrefix (1 - Updated Full)"
        val updatedIntro = "This is an updated test task."
        val updatedDesc = "$taskDescription (updated)"
        val newDeadline = taskDeadline + 1000000000
        val newDefaultDeadline = taskDefaultDeadline + 1
        val updatedRank = 1

        // Assuming PatchTaskRequestDTO exists
        val requestDTO1 =
            PatchTaskRequestDTO(
                name = updatedName,
                deadline = newDeadline,
                defaultDeadline = newDefaultDeadline,
                resubmittable = false,
                editable = false,
                intro = updatedIntro,
                description = updatedDesc,
                submissionSchema =
                    listOf(
                        TaskSubmissionSchemaEntryDTO(
                            prompt = "Text Entry",
                            type = TaskSubmissionTypeDTO.TEXT,
                        )
                    ), // Update schema
                rank = updatedRank,
                categoryId = customCategoryId, // Update category
            )

        // First update
        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO1)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                val task = response.data.task
                assertEquals(updatedName, task.name)
                assertEquals(newDeadline, task.deadline)
                assertEquals(updatedIntro, task.intro)
                assertEquals(customCategoryId, task.category?.id)
                assertEquals("Custom Category", task.category?.name)
                assertEquals(updatedRank, task.rank)
                assertEquals(1, task.submissionSchema.size) // Verify schema updated
            }

        // Second update (change some back)
        val originalName = "$taskNamePrefix (1 - USER, Default Cat)" // Reset name
        val requestDTO2 =
            PatchTaskRequestDTO(
                name = originalName, // Reset name
                intro = taskIntro, // Reset intro
                description = taskDescription, // Reset description
                categoryId = defaultCategoryId, // Reset category
            )

        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO2)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                val task = response.data.task
                assertEquals(originalName, task.name)
                assertEquals(taskIntro, task.intro)
                assertEquals(taskDescription, task.description)
                assertEquals(defaultCategoryId, task.category?.id)
            }
    }

    @Test
    @Order(41)
    fun `Task - Try updating Task category to archived (should fail)`() {
        val taskId = taskIds[0]
        archiveCategory(spaceAdminToken, spaceId, customCategoryId) // Archive the target category

        val requestDTO = PatchTaskRequestDTO(categoryId = customCategoryId)
        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isBadRequest // Expect 400

        unarchiveCategory(spaceAdminToken, spaceId, customCategoryId) // Clean up
    }

    @Test
    @Order(45)
    fun `Task - Update task removing deadline`() { // Renamed
        val taskId = taskIds[0]
        val requestDTO =
            PatchTaskRequestDTO(hasDeadline = false) // Use the specific field to remove deadline

        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                assertNull(response.data.task.deadline, "Deadline should be null after patch")
            }
    }

    // --- Enumeration Tests ---

    @Test
    @Order(50)
    fun `Task - Enumerate approved tasks not joined`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks")
                    .queryParam("space", spaceId)
                    .queryParam("approved", ApproveTypeDTO.APPROVED.name)
                    .queryParam("joined", "false") // Filter for not joined by requester
                    .queryParam("querySpace", "true")
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken") // Creator hasn't joined any task yet
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTasks200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.tasks)
                val tasks = response.data!!.tasks!!
                // Tasks 0, 1, 2 are approved
                assertEquals(3, tasks.size)
                assertTrue(tasks.all { it.approved == ApproveTypeDTO.APPROVED })
                // Check specific task details
                val task0 = tasks.find { it.id == taskIds[0] }
                assertNotNull(task0)
                assertEquals("$taskNamePrefix (1 - USER, Default Cat)", task0!!.name)
                assertEquals(taskIntro, task0.intro)
                assertEquals(1, task0.rank) // Rank was set in test 40
            }
    }

    @Test
    @Order(51)
    fun `Task - Enumerate approved tasks filtered by custom category`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks")
                    .queryParam("space", spaceId)
                    .queryParam("categoryId", customCategoryId)
                    .queryParam("approved", ApproveTypeDTO.APPROVED.name)
                    .queryParam("querySpace", "true")
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken") // Use any authorized user
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTasks200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.tasks)
                val tasks = response.data!!.tasks!!
                // Only Task 2 is in custom category and approved
                assertEquals(1, tasks.size)
                assertEquals(taskIds[1], tasks[0].id)
                assertEquals(customCategoryId, tasks[0].category?.id)
            }
    }

    @Test
    @Order(60)
    fun `Task - Enumerate approved tasks pagination page 1`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks")
                    .queryParam("space", spaceId)
                    .queryParam("pageSize", 2)
                    .queryParam("approved", ApproveTypeDTO.APPROVED.name)
                    // Default sort is likely createdAt DESC
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTasks200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.tasks)
                assertNotNull(response.data?.page)
                val tasks = response.data!!.tasks!!
                val page = response.data!!.page!!
                assertEquals(2, tasks.size)
                assertEquals(taskIds[0], tasks[0].id) // Task 1
                assertEquals(taskIds[2], tasks[1].id) // Task 2
                assertEquals(true, page.hasMore)
                assertEquals(
                    taskIds[1],
                    page.nextStart,
                ) // Next should be task 3 (ID from taskIds[2])
            }
    }

    @Test
    @Order(65)
    fun `Task - Enumerate approved tasks pagination page 2`() { // Renamed
        val startId = taskIds[1] // Use nextStart from previous page
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks")
                    .queryParam("space", spaceId)
                    .queryParam("pageSize", 2)
                    .queryParam("pageStart", startId) // Start from Task 1
                    .queryParam("approved", ApproveTypeDTO.APPROVED.name)
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTasks200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.tasks)
                assertNotNull(response.data?.page)
                val tasks = response.data!!.tasks!!
                val page = response.data!!.page!!
                assertEquals(1, tasks.size)
                assertEquals(startId, tasks[0].id) // Should contain Task 1
                assertEquals(false, page.hasMore)
                assertNull(page.nextStart) // No more pages
            }
    }

    @Test
    @Order(70)
    fun `Task - Enumerate approved tasks sort by createdAt asc`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks")
                    .queryParam("space", spaceId)
                    .queryParam("sortBy", "createdAt")
                    .queryParam("sortOrder", "asc")
                    .queryParam("approved", ApproveTypeDTO.APPROVED.name)
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTasks200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.tasks)
                val tasks = response.data!!.tasks!!
                assertTrue(tasks.isNotEmpty())
                // First created and approved task is Task 1 (index 0)
                assertEquals(taskIds[0], tasks[0].id)
                assertEquals("$taskNamePrefix (1 - USER, Default Cat)", tasks[0].name)
            }
    }

    @Test
    @Order(72)
    fun `Task - Enumerate tasks filtered by keywords`() { // Renamed
        val keyword = "$taskNamePrefix (1 - USER, Default Cat)" // Exact name of task 1
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks")
                    .queryParam("space", spaceId)
                    .queryParam("keywords", keyword)
                    .queryParam("approved", ApproveTypeDTO.APPROVED.name) // Filter for approved
                    .queryParam("joined", "false") // Filter for not joined
                    .queryParam("queryJoined", "true") // Request joined status
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken") // Creator is checking
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTasks200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.tasks)
                val tasks = response.data!!.tasks!!
                assertTrue(
                    tasks.none { it.approved != ApproveTypeDTO.APPROVED },
                    "All tasks should be approved",
                )
                assertTrue(tasks.none { it.joined != false }, "All tasks should not be joined")
            }
    }

    @Test
    @Order(80)
    fun `Task - Enumerate approved tasks sort by deadline desc`() { // Renamed
        // Note: This test might be less meaningful if all tasks have the same deadline
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks")
                    .queryParam("space", spaceId)
                    .queryParam("sortBy", "deadline")
                    .queryParam("sortOrder", "desc")
                    .queryParam("approved", ApproveTypeDTO.APPROVED.name)
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTasks200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.tasks)
                // Further assertions depend on whether deadlines differ
                // e.g., check if tasks are ordered correctly based on taskDeadline
            }
    }

    // --- Participant Management Tests ---

    @Test
    @Order(85)
    fun `Participant - Add users to Task 1`() { // Renamed
        val taskId = taskIds[0] // Target Task 1 (USER, Default Cat)
        participantTaskMembershipId =
            addParticipantUser(participantToken, taskId, participant.userId) // P1 joins
        assertTrue(participantTaskMembershipId > 0)
        // Skip P2 for now as per original commented out code
        participant4TaskMembershipId =
            addParticipantUser(participantToken4, taskId, participant4.userId) // P4 joins
        assertTrue(participant4TaskMembershipId > 0)
    }

    @Test
    @Order(86)
    fun `Task - Enumerate tasks joined by participant 1`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks")
                    .queryParam("space", spaceId)
                    .queryParam("approved", ApproveTypeDTO.APPROVED.name)
                    .queryParam("joined", "true") // Filter for joined
                    .queryParam("querySpace", "true")
                    .queryParam("queryJoined", "true") // Request joined status
                    .build()
            }
            .header("Authorization", "Bearer $participantToken") // Participant 1 checks
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTasks200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.tasks)
                val tasks = response.data!!.tasks!!
                assertEquals(1, tasks.size, "Participant 1 should have joined 1 task")
                assertEquals(taskIds[0], tasks[0].id)
                // Assert joined status if returned in this DTO, otherwise rely on filter working
                // assertEquals(true, tasks[0].joined) // Depends on DTO structure
            }
    }

    @Test
    @Order(86) // Same order as original
    fun `Task - Enumerate tasks joined by participant 1 with keywords`() { // Renamed
        val keyword = taskNamePrefix // Use prefix to match multiple if needed, or full name
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks")
                    .queryParam("space", spaceId)
                    .queryParam("approved", ApproveTypeDTO.APPROVED.name)
                    .queryParam("joined", "true") // Filter joined
                    .queryParam("keywords", keyword) // Filter keywords
                    .build()
            }
            .header("Authorization", "Bearer $participantToken") // Participant 1 checks
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTasks200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.tasks)
                val tasks = response.data!!.tasks!!
                // Assuming Task 1 (joined by P1) matches the keyword prefix
                assertEquals(1, tasks.size)
                assertEquals(taskIds[0], tasks[0].id)
            }
    }

    @Test
    @Order(86) // Same order as original
    fun `Participant - Add user 3 to Task 1 by owner with deadline`() { // Renamed
        val taskId = taskIds[0]
        val requestDTO =
            PostTaskParticipantRequestDTO(
                deadline = taskMembershipDeadline, // Set deadline
                email = "test@example.com",
            )
        val response =
            webTestClient
                .post()
                .uri { builder ->
                    builder
                        .path("/tasks/$taskId/participants")
                        .queryParam("member", participant3.userId)
                        .build()
                }
                .header("Authorization", "Bearer $creatorToken") // Owner adds participant
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<PostTaskParticipant200ResponseDTO>()
                .returnResult()
                .responseBody

        assertNotNull(response?.data?.participant)
        participant3TaskMembershipId = response!!.data.participant!!.id
        assertTrue(participant3TaskMembershipId > 0)
        // Verify participant is automatically approved because owner added with deadline? Check
        // logic.
        // assertEquals(ApproveTypeDTO.APPROVED, response.data!!.participant!!.approved) // Verify
        // approval status
        // assertEquals(taskMembershipDeadline, response.data!!.participant!!.deadline) // Verify
        // deadline
    }

    @Test
    @Order(87)
    fun `Participant - Add user 2 with deadline fails for non-owner`() { // Renamed
        val taskId = taskIds[0]
        val requestDTO =
            PostTaskParticipantRequestDTO(
                deadline = taskMembershipDeadline,
                email = "test@example.com",
            )
        webTestClient
            .post()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants")
                    .queryParam("member", participant2.userId)
                    .build()
            }
            .header(
                "Authorization",
                "Bearer $participantToken2",
            ) // P2 tries to add self with deadline
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isForbidden // Expect 403
            .expectBody<GenericErrorResponse>()
            .value { error -> assertEquals("AccessDeniedError", error.error.name) }
    }

    @Test
    @Order(87) // Same order
    fun `Participant - Add user 2 without deadline fails for owner (if restricted)`() { // Renamed
        // This test assumes owner CANNOT add participant without deadline (verify this logic)
        val taskId = taskIds[0]
        val requestDTO = PostTaskParticipantRequestDTO(email = "test@example.com") // No deadline
        webTestClient
            .post()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants")
                    .queryParam("member", participant2.userId)
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken") // Owner tries
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isForbidden // Expect 403 based on original test name
            .expectBody<GenericErrorResponse>()
            .value { error ->
                assertEquals("AccessDeniedError", error.error.name)
            } // Or specific error
    }

    @Test
    @Order(93)
    fun `Participant - Add user 1 again fails (already participant)`() { // Renamed
        val taskId = taskIds[0]
        val requestDTO = PostTaskParticipantRequestDTO(email = "test@example.com")
        webTestClient
            .post()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants")
                    .queryParam("member", participant.userId)
                    .build()
            }
            .header("Authorization", "Bearer $participantToken") // P1 tries to join again
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT) // Expect 409
            .expectBody<GenericErrorResponse>()
            .value { error -> assertEquals("AlreadyBeTaskParticipantError", error.error.name) }
    }

    @Test
    @Order(95)
    fun `Participant - Add team to Task 2`() { // Renamed
        val taskId = taskIds[1] // Target TEAM Task 2
        teamTaskMembershipId =
            addParticipantTeam(teamCreatorToken, taskId, teamId) // Team creator adds team
        assertTrue(teamTaskMembershipId > 0)
    }

    @Test
    @Order(96)
    fun `Participant - Add team again fails (already participant)`() { // Renamed
        val taskId = taskIds[1]
        val requestDTO = PostTaskParticipantRequestDTO(email = "test@example.com")
        webTestClient
            .post()
            .uri { builder ->
                builder.path("/tasks/$taskId/participants").queryParam("member", teamId).build()
            }
            .header("Authorization", "Bearer $teamCreatorToken") // Team creator tries again
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT) // Expect 409
            .expectBody<GenericErrorResponse>()
            .value { error -> assertEquals("AlreadyBeTaskParticipantError", error.error.name) }
    }

    @Test
    @Order(100)
    fun `Participant - List participants for USER Task 1`() { // Renamed
        val taskId = taskIds[0]
        webTestClient
            .get()
            .uri("/tasks/$taskId/participants")
            .header("Authorization", "Bearer $creatorToken") // Creator views participants
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskParticipants200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.participants)
                val participants = response.data.participants
                // Users added: P1 (ID: participantTaskMembershipId), P3 (ID:
                // participant3TaskMembershipId), P4 (ID: participant4TaskMembershipId)
                assertEquals(3, participants.size)
                val participantIds = participants.mapNotNull { it.member.id }.toSet()
                assertTrue(participantIds.contains(participant.userId))
                assertTrue(participantIds.contains(participant3.userId))
                assertTrue(participantIds.contains(participant4.userId))
                // Optionally check roles, approval status if needed
            }
    }

    @Test
    @Order(101)
    fun `Participant - List participant for TEAM Task 2`() { // Renamed
        val taskId = taskIds[1]
        webTestClient
            .get()
            .uri("/tasks/$taskId/participants")
            .header("Authorization", "Bearer $creatorToken") // Creator views participants
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskParticipants200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.participants)
                val participants = response.data.participants
                // Only the team was added
                assertEquals(1, participants.size)
                assertEquals(teamId, participants[0].member.id)
                assertNotNull(participants[0].member.name) // Check team name exists
            }
    }

    @Test
    @Order(105)
    fun `Participant - Disapprove user 4 for Task 1`() { // Renamed
        val taskId = taskIds[0]
        val membershipId = participant4TaskMembershipId
        val requestDTO = PatchTaskMembershipRequestDTO(approved = ApproveTypeDTO.DISAPPROVED)
        webTestClient
            .patch()
            .uri("/tasks/$taskId/participants/$membershipId")
            .header("Authorization", "Bearer $creatorToken") // Creator disapproves
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskParticipant200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.taskMembership)
                assertEquals(ApproveTypeDTO.DISAPPROVED, response.data.taskMembership.approved)
            }
    }

    // --- Participant Listing by Approval Status ---
    private fun assertParticipantListByStatus(
        taskId: IdType,
        status: ApproveTypeDTO?,
        expectedCount: Int,
        expectedMemberIds: Set<IdType>,
    ) {
        webTestClient
            .get()
            .uri { builder ->
                builder.path("/tasks/$taskId/participants")
                status?.let {
                    builder.queryParam("approved", it.name)
                } // Add param only if status is not null
                builder.build()
            }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskParticipants200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.participants)
                val participants = response.data.participants
                assertEquals(expectedCount, participants.size, "Count mismatch for status $status")
                val actualMemberIds = participants.map { it.member.id }.toSet()
                assertEquals(
                    expectedMemberIds,
                    actualMemberIds,
                    "Member ID mismatch for status $status",
                )
            }
    }

    @Test
    @Order(106)
    fun `Participant - List participants by status NONE`() {
        // P1 joined, no explicit approval yet -> NONE
        assertParticipantListByStatus(taskIds[0], ApproveTypeDTO.NONE, 1, setOf(participant.userId))
    }

    @Test
    @Order(107)
    fun `Participant - List participants by status APPROVED`() {
        // P3 was added by owner with deadline, assuming auto-approval -> APPROVED
        assertParticipantListByStatus(
            taskIds[0],
            ApproveTypeDTO.APPROVED,
            1,
            setOf(participant3.userId),
        )
    }

    @Test
    @Order(108)
    fun `Participant - List participants by status DISAPPROVED`() {
        // P4 was explicitly disapproved in test 105
        assertParticipantListByStatus(
            taskIds[0],
            ApproveTypeDTO.DISAPPROVED,
            1,
            setOf(participant4.userId),
        )
    }

    // --- Further Participant Management ---

    @Test
    @Order(109)
    fun `Participant - Approve user 1 using PATCH by member ID`() { // Renamed
        val taskId = taskIds[0]
        val requestDTO = PatchTaskMembershipRequestDTO(approved = ApproveTypeDTO.APPROVED)
        webTestClient
            .patch()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants")
                    .queryParam("member", participant.userId)
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken") // Creator approves
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            // This endpoint returns list of participants according to Controller:
            // PatchTaskMembershipByMember200ResponseDTO
            .expectBody<PatchTaskMembershipByMember200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.participants)
                val p1Membership =
                    response.data.participants!!.find { it.member.id == participant.userId }
                assertNotNull(p1Membership, "Participant 1 not found in response")
                assertEquals(
                    ApproveTypeDTO.APPROVED,
                    p1Membership!!.approved,
                    "Participant 1 not approved",
                )
            }
    }

    @Test
    @Order(110)
    fun `Participant - Update deadline for user 1 using PATCH by member ID`() { // Renamed
        val taskId = taskIds[0]
        val newDeadline = taskMembershipDeadline + 100000
        val requestDTO = PatchTaskMembershipRequestDTO(deadline = newDeadline)
        webTestClient
            .patch()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants")
                    .queryParam("member", participant.userId)
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken") // Creator updates deadline
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTaskMembershipByMember200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.participants)
                val p1Membership =
                    response.data.participants!!.find { it.member.id == participant.userId }
                assertNotNull(p1Membership, "Participant 1 not found in response")
                assertEquals(
                    newDeadline,
                    p1Membership!!.deadline,
                    "Participant 1 deadline not updated",
                )
            }
    }

    @Test
    @Order(111)
    fun `Participant - Remove self (user 1) from Task 1`() { // Renamed
        val taskId = taskIds[0]
        webTestClient
            .delete()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants")
                    .queryParam("member", participant.userId)
                    .build()
            }
            .header("Authorization", "Bearer $participantToken") // P1 removes self
            .exchange()
            .expectStatus()
            .isNoContent // Expect 204
    }

    @Test
    @Order(112)
    fun `Participant - Remove self (user 3) from Task 1`() { // Renamed
        val taskId = taskIds[0]
        webTestClient
            .delete()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants")
                    .queryParam("member", participant3.userId)
                    .build()
            }
            .header("Authorization", "Bearer $participantToken3") // P3 removes self
            .exchange()
            .expectStatus()
            .isNoContent
    }

    @Test
    @Order(113)
    fun `Participant - Remove self (user 4) from Task 1`() { // Renamed
        val taskId = taskIds[0]
        webTestClient
            .delete()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants")
                    .queryParam("member", participant4.userId)
                    .build()
            }
            .header("Authorization", "Bearer $participantToken4") // P4 removes self
            .exchange()
            .expectStatus()
            .isNoContent
    }

    @Test
    @Order(115)
    fun `Participant - Remove team from Task 2 by team creator`() { // Renamed
        val taskId = taskIds[1]
        webTestClient
            .delete()
            .uri { builder ->
                builder.path("/tasks/$taskId/participants").queryParam("member", teamId).build()
            }
            .header("Authorization", "Bearer $teamCreatorToken") // Team creator removes team
            .exchange()
            .expectStatus()
            .isNoContent
    }

    @Test
    @Order(120)
    fun `Participant - List participants for Task 1 after removals (should be empty)`() { // Renamed
        val taskId = taskIds[0]
        webTestClient
            .get()
            .uri("/tasks/$taskId/participants")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskParticipants200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.participants)
                assertTrue(
                    response.data.participants.isEmpty(),
                    "Participant list for Task 1 should be empty",
                )
            }
    }

    @Test
    @Order(125)
    fun `Participant - List participant for Task 2 after removal (should be empty)`() { // Renamed
        val taskId = taskIds[1]
        webTestClient
            .get()
            .uri("/tasks/$taskId/participants")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskParticipants200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.participants)
                assertTrue(
                    response.data.participants.isEmpty(),
                    "Participant list for Task 2 should be empty",
                )
            }
    }

    // --- Category Deletion Tests ---

    @Test
    @Order(200)
    fun `Category - Try deleting default category fails (if tasks exist)`() { // Renamed
        // Task 3 is still in default category
        webTestClient
            .delete()
            .uri("/spaces/$spaceId/categories/$defaultCategoryId")
            .header("Authorization", "Bearer $spaceAdminToken")
            .exchange()
            // Expect Bad Request (cannot delete default) or Conflict (contains tasks)
            .expectStatus()
            .isBadRequest // Original test expected BadRequest for default deletion attempt
        // Optionally check error DTO
        // .expectBody<GenericErrorResponse>()
    }

    @Test
    @Order(201)
    fun `Category - Try deleting custom category fails (if tasks exist)`() { // Renamed
        // Task 4 is still in custom category
        webTestClient
            .delete()
            .uri("/spaces/$spaceId/categories/$customCategoryId")
            .header("Authorization", "Bearer $spaceAdminToken")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT) // Expect 409 Conflict (contains tasks)
        // Optionally check error DTO
        // .expectBody<GenericErrorResponse>()
    }

    @Test
    @Order(202)
    fun `Category - Delete an empty category successfully`() {
        val emptyCatId = createCategory(spaceAdminToken, spaceId, "Empty Category Final")
        assertTrue(emptyCatId > 0)

        webTestClient
            .delete()
            .uri("/spaces/$spaceId/categories/$emptyCatId")
            .header("Authorization", "Bearer $spaceAdminToken")
            .exchange()
            .expectStatus()
            .isNoContent // Expect 204

        // Verify deletion
        webTestClient
            .get()
            .uri("/spaces/$spaceId/categories/$emptyCatId")
            .header("Authorization", "Bearer $spaceAdminToken")
            .exchange()
            .expectStatus()
            .isNotFound // Expect 404
    }

    // --- Task Deletion Tests ---

    // Helper to ensure a task exists for deletion tests
    private fun ensureTaskExists(index: Int, nameSuffix: String): IdType {
        return taskIds.getOrNull(index)
            ?: run {
                logger.warn("Task at index $index not found, recreating...")
                createTask(
                        token = creatorToken,
                        name = "$taskNamePrefix ($nameSuffix Recreated)",
                        submitterType = TaskSubmitterType.USER,
                        deadline = null,
                        defaultDeadline = taskDefaultDeadline,
                        resubmittable = true,
                        editable = true,
                        intro = "recreate",
                        description = "recreate",
                        submissionSchema = emptyList(),
                        spaceId = spaceId,
                        categoryId = defaultCategoryId, // Use default category for simplicity
                    )!!
                    .also { taskIds.add(it) } // Add to list if recreated
            }
    }

    @Test
    @Order(230)
    fun `Task - Delete task fails for non-owner non-admin`() { // Renamed
        val taskIdToDelete = ensureTaskExists(2, "Task 3") // Use Task 3 (index 2)

        webTestClient
            .delete()
            .uri("/tasks/$taskIdToDelete")
            .header("Authorization", "Bearer $teamCreatorToken") // Use unrelated user token
            .exchange()
            .expectStatus()
            .isForbidden // Expect 403
            .expectBody<GenericErrorResponse>()
            .value { error -> assertEquals("AccessDeniedError", error.error.name) }
    }

    @Test
    @Order(240)
    fun `Task - Delete task success for owner`() { // Renamed
        val taskIdToDelete = ensureTaskExists(2, "Task 3") // Ensure Task 3 exists

        webTestClient
            .delete()
            .uri("/tasks/$taskIdToDelete")
            .header("Authorization", "Bearer $creatorToken") // Creator deletes
            .exchange()
            .expectStatus()
            .isNoContent

        taskIds.remove(taskIdToDelete) // Remove from local list

        // Verify deletion
        webTestClient
            .get()
            .uri("/tasks/$taskIdToDelete")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isNotFound // Expect 404
    }
}
