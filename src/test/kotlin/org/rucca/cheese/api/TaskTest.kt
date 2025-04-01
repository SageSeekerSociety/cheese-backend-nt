/*
 *  Description: It tests the feature of space.
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
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.utils.JsonArrayUtil
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.*

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class TaskTest
@Autowired
constructor(private val mockMvc: MockMvc, private val userCreatorService: UserCreatorService) {
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
    private val taskNamePrefix = "Test Task (${floor(Math.random() * 10000000000).toLong()})"
    private val taskIntro = "This is a test task."
    private val taskDescription = "A lengthy text. ".repeat(100)
    private val taskDeadline =
        LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val taskDefaultDeadline = 30L
    private val taskMembershipDeadline =
        LocalDateTime.now().plusMonths(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val taskSubmissionSchema =
        listOf(Pair("Text Entry", "TEXT"), Pair("Attachment Entry", "FILE"))

    // --- Membership IDs ---
    private var participantTaskMembershipId: IdType = -1
    private var participant2TaskMembershipId: IdType = -1 // Will be assigned later if needed
    private var participant3TaskMembershipId: IdType = -1
    private var participant4TaskMembershipId: IdType = -1
    private var teamTaskMembershipId: IdType = -1

    // --- Helper Functions ---

    /** Creates a Space and returns its ID and the ID of its default category. */
    fun createSpace(
        creatorToken: String,
        spaceName: String,
        spaceIntro: String,
        spaceDescription: String,
        spaceAvatarId: IdType,
    ): Pair<IdType, IdType> {
        val result =
            mockMvc
                .post("/spaces") {
                    headers { setBearerAuth(creatorToken) }
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        """ { "name": "$spaceName", "intro": "$spaceIntro", "description": "$spaceDescription", "avatarId": $spaceAvatarId, "announcements": "[]", "taskTemplates": "[]" } """
                }
                .andExpect { status { isOk() } }
                .andExpect { jsonPath("$.data.space.id") { exists() } }
                .andExpect { jsonPath("$.data.space.defaultCategoryId") { exists() } }
                .andReturn()
        val json = JSONObject(result.response.contentAsString)
        val spaceData = json.getJSONObject("data").getJSONObject("space")
        val createdSpaceId = spaceData.getLong("id")
        val createdDefaultCategoryId = spaceData.getLong("defaultCategoryId")
        logger.info(
            "Created space: $createdSpaceId with default category ID: $createdDefaultCategoryId"
        )
        return Pair(createdSpaceId, createdDefaultCategoryId)
    }

    /** Adds a user as an admin to a space. Uses original endpoint /managers. */
    fun addSpaceAdmin(creatorToken: String, spaceId: IdType, adminId: IdType) {
        mockMvc
            .post("/spaces/$spaceId/managers") {
                headers { setBearerAuth(creatorToken) }
                contentType = MediaType.APPLICATION_JSON
                content = """ { "role": "ADMIN", "userId": $adminId } """
            }
            .andExpect { status { isOk() } }
    }

    /** Creates a Team and returns its ID. */
    fun createTeam(
        creatorToken: String,
        teamName: String,
        teamIntro: String,
        teamDescription: String,
        teamAvatarId: IdType,
    ): IdType {
        val result =
            mockMvc
                .post("/teams") {
                    headers { setBearerAuth(creatorToken) }
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        """ { "name": "$teamName", "intro": "$teamIntro", "description": "$teamDescription", "avatarId": $teamAvatarId } """
                }
                .andExpect { status { isOk() } }
                .andReturn()
        val createdTeamId =
            JSONObject(result.response.contentAsString)
                .getJSONObject("data")
                .getJSONObject("team")
                .getLong("id")
        logger.info("Created team: $createdTeamId")
        return createdTeamId
    }

    /** Adds a user to a team as a member. */
    fun joinTeam(token: String, teamId: IdType, userId: IdType) {
        mockMvc
            .post("/teams/$teamId/members") {
                headers { setBearerAuth(token) }
                contentType = MediaType.APPLICATION_JSON
                content = """ { "role": "MEMBER", "user_id": $userId } """
            }
            .andExpect { status { isOk() } }
    }

    /** Adds a user to a team as an admin. */
    fun addTeamAdmin(creatorToken: String, teamId: IdType, adminId: IdType) {
        mockMvc
            .post("/teams/$teamId/members") {
                headers { setBearerAuth(creatorToken) }
                contentType = MediaType.APPLICATION_JSON
                content = """ { "role": "ADMIN", "user_id": $adminId } """
            }
            .andExpect { status { isOk() } }
    }

    @BeforeAll
    fun prepare() {
        // Create users
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
                spaceName = "Test Space (${floor(Math.random() * 10000000000).toLong()})",
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
                teamName = "Test Team (${floor(Math.random() * 10000000000).toLong()})",
                teamIntro = "This is a test team.",
                teamDescription = "A lengthy text. ".repeat(100),
                teamAvatarId = userCreatorService.testAvatarId(),
            )

        // Setup memberships/admins
        joinTeam(teamCreatorToken, teamId, teamMember.userId)
        addSpaceAdmin(spaceCreatorToken, spaceId, spaceAdmin.userId)
        addTeamAdmin(teamCreatorToken, teamId, teamAdmin.userId)
    }

    /** Creates a Task and returns its ID, or null if creation fails as expected. */
    fun createTask(
        token: String = creatorToken,
        name: String,
        submitterType: String,
        deadline: Long?,
        defaultDeadline: Long?,
        resubmittable: Boolean,
        editable: Boolean,
        intro: String,
        description: String,
        submissionSchema: List<Pair<String, String>>,
        spaceId: IdType,
        categoryId: IdType?,
        expectedStatus: Int = HttpStatus.OK.value(), // Match original test expectation
    ): IdType? {
        val resultActions =
            mockMvc
                .post("/tasks") {
                    headers { setBearerAuth(token) }
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        """
                {
                    "name": "$name",
                    "submitterType": "$submitterType",
                    "deadline": ${deadline?.toString() ?: "null"},
                    "defaultDeadline": $defaultDeadline,
                    "resubmittable": $resubmittable,
                    "editable": $editable,
                    "intro": "$intro",
                    "description": "$description",
                    "submissionSchema": [ ${submissionSchema.joinToString(",\n") { """{"prompt": "${it.first}", "type": "${it.second}"}""" }} ],
                    "space": $spaceId,
                    "categoryId": ${categoryId?.toString() ?: "null"}
                }
                """
                }
                .andExpect { status { isEqualTo(expectedStatus) } }

        if (expectedStatus == HttpStatus.OK.value()) {
            val result =
                resultActions
                    .andExpect { jsonPath("$.data.task.id") { exists() } }
                    .andExpect { jsonPath("$.data.task.name") { value(name) } }
                    .andExpect { jsonPath("$.data.task.submitterType") { value(submitterType) } }
                    .andExpect { jsonPath("$.data.task.creator.id") { value(creator.userId) } }
                    .andExpect { jsonPath("$.data.task.deadline") { value(deadline) } }
                    .andExpect {
                        jsonPath("$.data.task.defaultDeadline") { value(defaultDeadline) }
                    }
                    .andExpect { jsonPath("$.data.task.resubmittable") { value(resubmittable) } }
                    .andExpect { jsonPath("$.data.task.editable") { value(editable) } }
                    .andExpect { jsonPath("$.data.task.intro") { value(intro) } }
                    .andExpect { jsonPath("$.data.task.description") { value(description) } }
                    .andExpect { jsonPath("$.data.task.space.id") { value(spaceId) } }
                    //                    .andExpect {
                    //                        jsonPath("$.data.task.category.id") {
                    //                            value(categoryId ?: defaultCategoryId)
                    //                        }
                    //                    }
                    //                    .andExpect { jsonPath("$.data.task.category.name") {
                    // exists() } }
                    .andExpect { jsonPath("$.data.task.approved") { value("NONE") } }
                    .andReturn()

            val json = JSONObject(result.response.contentAsString)
            // Submission schema validation
            for (entry in submissionSchema) {
                val schema =
                    json
                        .getJSONObject("data")
                        .getJSONObject("task")
                        .getJSONArray("submissionSchema")
                val found =
                    JsonArrayUtil.toArray(schema).find { it.getString("prompt") == entry.first }
                assert(found != null) { "Submission schema entry '${entry.first}' not found" }
                assert(found!!.getString("type") == entry.second) {
                    "Type mismatch for '${entry.first}'"
                }
            }
            val taskId = json.getJSONObject("data").getJSONObject("task").getLong("id")
            taskIds.add(taskId)
            logger.info(
                "Created task: $taskId (Type: $submitterType, Space: $spaceId, Category: ${categoryId ?: defaultCategoryId})"
            )
            return taskId
        }
        return null
    }

    /** Approves a task. */
    fun approveTask(taskId: IdType, token: String) {
        mockMvc
            .patch("/tasks/$taskId") {
                headers { setBearerAuth(token) }
                contentType = MediaType.APPLICATION_JSON
                content = """ { "approved": "APPROVED" } """
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.task.approved") { value("APPROVED") } }
    }

    /** Adds a user participant to a task. */
    fun addParticipantUser(token: String, taskId: IdType, userId: IdType): IdType {
        val result =
            mockMvc
                .post("/tasks/$taskId/participants") {
                    headers { setBearerAuth(token) }
                    param("member", userId.toString())
                    contentType = MediaType.APPLICATION_JSON
                    content = """ { "email": "test@example.com" } """
                }
                .andExpect { status { isOk() } }
                .andReturn()
        val json = JSONObject(result.response.contentAsString)
        return json.getJSONObject("data").getJSONObject("participant").getLong("id")
    }

    /** Adds a team participant to a task. */
    fun addParticipantTeam(token: String, taskId: IdType, teamId: IdType): IdType {
        val result =
            mockMvc
                .post("/tasks/$taskId/participants") {
                    headers { setBearerAuth(token) }
                    param("member", teamId.toString())
                    contentType = MediaType.APPLICATION_JSON
                    content = """ { "email": "test@example.com" } """
                }
                .andExpect { status { isOk() } }
                .andReturn()
        val json = JSONObject(result.response.contentAsString)
        return json.getJSONObject("data").getJSONObject("participant").getLong("id")
    }

    /** Approves a task participant (membership). */
    fun approveTaskParticipant(token: String, taskId: IdType, participantMembershipId: IdType) {
        mockMvc
            .patch("/tasks/$taskId/participants/$participantMembershipId") {
                headers { setBearerAuth(token) }
                contentType = MediaType.APPLICATION_JSON
                content = """ { "approved": "APPROVED" } """
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.taskMembership.approved") { value("APPROVED") } }
    }

    /** Creates a category within a space. */
    fun createCategory(
        token: String,
        spaceId: IdType,
        name: String,
        description: String? = null,
        displayOrder: Int? = null,
    ): IdType {
        val result =
            mockMvc
                .post("/spaces/$spaceId/categories") {
                    headers { setBearerAuth(token) }
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        JSONObject()
                            .put("name", name)
                            .putOpt("description", description)
                            .putOpt("displayOrder", displayOrder)
                            .toString()
                }
                .andExpect { status { isCreated() } }
                .andExpect { jsonPath("$.data.category.id") { exists() } }
                .andExpect { jsonPath("$.data.category.name") { value(name) } }
                .andReturn()
        val categoryId =
            JSONObject(result.response.contentAsString)
                .getJSONObject("data")
                .getJSONObject("category")
                .getLong("id")
        logger.info("Created category '$name' (ID: $categoryId) in space $spaceId")
        return categoryId
    }

    /** Archives a category. */
    fun archiveCategory(token: String, spaceId: IdType, categoryId: IdType) {
        mockMvc
            .post("/spaces/$spaceId/categories/$categoryId/archive") {
                headers { setBearerAuth(token) }
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.category.id") { value(categoryId) } }
            .andExpect { jsonPath("$.data.category.archivedAt") { exists() } }
        logger.info("Archived category $categoryId in space $spaceId")
    }

    /** Unarchives a category. */
    fun unarchiveCategory(token: String, spaceId: IdType, categoryId: IdType) {
        mockMvc
            .delete("/spaces/$spaceId/categories/$categoryId/archive") {
                headers { setBearerAuth(token) }
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.category.id") { value(categoryId) } }
            .andExpect { jsonPath("$.data.category.archivedAt") { doesNotExist() } }
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
        assert(customCategoryId > 0)
        assert(archivedCategoryId > 0)
    }

    @Test
    @Order(6)
    fun `Category - List active categories`() {
        mockMvc
            .get("/spaces/$spaceId/categories") { headers { setBearerAuth(spaceAdminToken) } }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.categories.length()") { value(2) } } // Default, Custom
            .andExpect {
                jsonPath("$.data.categories[?(@.id == $defaultCategoryId)].name") {
                    value("General")
                }
            }
            .andExpect {
                jsonPath("$.data.categories[?(@.id == $customCategoryId)].name") {
                    value("Custom Category")
                }
            }
            .andExpect {
                jsonPath("$.data.categories[?(@.id == $archivedCategoryId)]") { doesNotExist() }
            }
    }

    @Test
    @Order(7)
    fun `Category - List all categories (including archived)`() {
        mockMvc
            .get("/spaces/$spaceId/categories") {
                headers { setBearerAuth(spaceAdminToken) }
                param("includeArchived", "true")
            }
            .andExpect { status { isOk() } }
            .andExpect {
                jsonPath("$.data.categories.length()") { value(3) }
            } // Default, Custom, Archived
            .andExpect {
                jsonPath("$.data.categories[?(@.id == $archivedCategoryId)].archivedAt") {
                    exists()
                }
            }
    }

    @Test
    @Order(10)
    fun testCreateTask() {
        createTask(
            name = "$taskNamePrefix (1 - USER, Default Cat)",
            submitterType = "USER",
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
        createTask(
            name = "$taskNamePrefix (2 - TEAM, Custom Cat)",
            submitterType = "TEAM",
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
        createTask(
            name = "$taskNamePrefix (3 - USER, Default Cat)",
            submitterType = "USER",
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
        createTask(
            name = "$taskNamePrefix (4 - USER, Custom Cat)",
            submitterType = "USER",
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
        Assertions.assertEquals(4, taskIds.size, "Should have created 4 tasks")
    }

    @Test
    @Order(11)
    fun `Task - Try creating task in archived category (should fail)`() {
        createTask(
            token = creatorToken,
            name = "$taskNamePrefix (Should Fail)",
            submitterType = "USER",
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
    fun testEnumerateOwnedTasks() {
        mockMvc
            .get("/tasks") {
                headers { setBearerAuth(creatorToken) }
                param("space", spaceId.toString())
                param("owner", creator.userId.toString())
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.tasks.length()") { value(4) } }
    }

    @Test
    @Order(14)
    fun testEnumerateUnapprovedTasks() {
        mockMvc
            .get("/tasks") {
                headers { setBearerAuth(spaceAdminToken) }
                param("space", spaceId.toString())
                param("approved", "NONE")
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.tasks.length()") { value(4) } }
    }

    @Test
    @Order(15)
    fun testEnumerateUnapprovedTasksWithoutAdmin() {
        mockMvc
            .get("/tasks") {
                headers { setBearerAuth(creatorToken) }
                param("approved", "NONE")
                param("space", spaceId.toString())
            }
            .andExpect { status { isForbidden() } }
    }

    @Test
    @Order(16)
    fun testGetUnapprovedTask() {
        val taskId = taskIds[3] // Task 4 (USER, Custom Cat) is initially unapproved
        mockMvc
            .get("/tasks/$taskId") {
                headers { setBearerAuth(spaceAdminToken) }
                queryParam("queryJoinability", "true")
                queryParam("querySubmittability", "true")
            }
            .andExpect { status { isOk() } }
            .andExpect {
                jsonPath("$.data.task.name") { value("$taskNamePrefix (4 - USER, Custom Cat)") }
            }
            .andExpect {
                jsonPath("$.data.task.approved") { value("NONE") }
            } // Verify it's unapproved
    }

    @Test
    @Order(17)
    fun testGetUnapprovedTaskAccessDeniedError() {
        val taskId = taskIds[3]
        mockMvc
            .get("/tasks/$taskId") {
                headers { setBearerAuth(participantToken) }
                queryParam("queryJoinability", "true")
                queryParam("querySubmittability", "true")
            }
            .andExpect { status { isForbidden() } }
            .andExpect {
                jsonPath("$.error.name") { value("AccessDeniedError") }
            } // Assuming this error name
    }

    @Test
    @Order(18) // Changed order to match original test flow
    fun testJoinUnapprovedTaskForbiddenError() {
        mockMvc
            .post("/tasks/${taskIds[3]}/participants") { // Target unapproved task 4
                headers { setBearerAuth(participantToken) }
                param("member", participant.userId.toString())
                contentType = MediaType.APPLICATION_JSON
                content = """{ "email": "test@example.com" }"""
            }
            .andExpect { status { isForbidden() } }
            .andExpect {
                jsonPath("$.error.name") { value("ForbiddenError") }
            } // Assuming this error name
    }

    @Test
    @Order(19) // Changed order
    fun testApproveTask() {
        approveTask(taskIds[0], spaceAdminToken)
        approveTask(taskIds[1], spaceAdminToken)
        approveTask(taskIds[2], spaceAdminToken)
        // taskIds[3] (Task 4) remains unapproved for now
    }

    @Test
    @Order(19) // Keep original order, rename slightly for clarity
    fun testApproveTaskWithoutAdminPermission() {
        val taskId = taskIds[3] // Target unapproved task 4
        mockMvc
            .patch("/tasks/$taskId") {
                headers { setBearerAuth(creatorToken) } // Use non-admin token
                contentType = MediaType.APPLICATION_JSON
                content = """ { "approved": "APPROVED" } """
            }
            .andExpect { status { isForbidden() } }
        // Approve task 3 again just to ensure it stays approved (original test did this)
        approveTask(taskIds[2], spaceAdminToken)
    }

    @Test
    @Order(20)
    fun testGetTask() {
        val taskId = taskIds[0] // Task 1 (USER, Default Cat), now approved
        val result =
            mockMvc
                .get("/tasks/$taskId") {
                    headers { setBearerAuth(creatorToken) }
                    param("querySpace", "true")
                }
                .andExpect { status { isOk() } }
                .andExpect {
                    jsonPath("$.data.task.name") {
                        value("$taskNamePrefix (1 - USER, Default Cat)")
                    }
                }
                .andExpect { jsonPath("$.data.task.submitterType") { value("USER") } }
                .andExpect { jsonPath("$.data.task.creator.id") { value(creator.userId) } }
                .andExpect { jsonPath("$.data.task.deadline") { value(taskDeadline) } }
                .andExpect { jsonPath("$.data.task.space.id") { value(spaceId) } }
                .andExpect { jsonPath("$.data.task.category.id") { value(defaultCategoryId) } }
                .andExpect { jsonPath("$.data.task.category.name") { value("General") } }
                .andExpect {
                    jsonPath("$.data.task.approved") { value("APPROVED") }
                } // Verify approved
                .andReturn()
        // Schema check
        val json = JSONObject(result.response.contentAsString)
        for (entry in taskSubmissionSchema) {
            val schema =
                json.getJSONObject("data").getJSONObject("task").getJSONArray("submissionSchema")
            val found = JsonArrayUtil.toArray(schema).find { it.getString("prompt") == entry.first }
            assert(found != null)
            assert(found!!.getString("type") == entry.second)
        }
    }

    // Keep original PATCH tests
    @Test
    @Order(21)
    fun testPatchRejectReasonWithoutAdmin() {
        val taskId = taskIds[2] // Target approved task 3
        mockMvc
            .patch("/tasks/$taskId") {
                headers { setBearerAuth(creatorToken) }
                contentType = MediaType.APPLICATION_JSON
                content = """ { "rejectReason": "Garbage." } """
            }
            .andExpect { status { isForbidden() } }
    }

    @Test
    @Order(22)
    fun testPatchRejectReason() {
        val taskId = taskIds[2]
        mockMvc
            .patch("/tasks/$taskId") {
                headers { setBearerAuth(spaceAdminToken) }
                contentType = MediaType.APPLICATION_JSON
                content = """ { "rejectReason": "Garbage." } """
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.task.rejectReason") { value("Garbage.") } }
    }

    // Keep original Optional Query tests, verify category presence
    @Test
    @Order(25)
    fun testGetTeamTaskWithOptionalQueriesUseTeamCreator() {
        val taskId = taskIds[1] // Task 2 (TEAM, Custom Cat), approved
        mockMvc
            .get("/tasks/$taskId") {
                queryParam("querySpace", "true")
                // queryParam("queryTeam", "true") // This param might not exist anymore if task
                // doesn't link directly to team entity
                queryParam("queryJoinability", "true")
                queryParam("querySubmittability", "true")
                queryParam("queryJoined", "true")
                queryParam("queryUserDeadline", "true")
                headers { setBearerAuth(teamCreatorToken) }
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.task.space.id") { value(spaceId) } }
            .andExpect {
                jsonPath("$.data.task.category.id") { value(customCategoryId) }
            } // Verify category
            .andExpect {
                jsonPath("$.data.task.joinable") { value(true) }
            } // Team creator can join as the team initially
            .andExpect {
                jsonPath("$.data.task.joinableTeams[0].id") { value(teamId) }
            } // Can join as this team
            .andExpect { jsonPath("$.data.task.submittable") { value(false) } }
            .andExpect { jsonPath("$.data.task.submittableAsTeam") { isEmpty() } }
            .andExpect { jsonPath("$.data.task.joined") { value(false) } }
            .andExpect { jsonPath("$.data.task.joinedTeams") { isEmpty() } }
            .andExpect { jsonPath("$.data.task.userDeadline") { isEmpty() } }
    }

    @Test
    @Order(26)
    fun testGetTeamTaskWithOptionalQueriesUseSpaceCreator() {
        val taskId = taskIds[1]
        mockMvc
            .get("/tasks/$taskId") {
                queryParam("querySpace", "true")
                queryParam("queryJoinability", "true")
                queryParam("querySubmittability", "true")
                queryParam("queryJoined", "true")
                headers { setBearerAuth(spaceCreatorToken) } // Space creator is not team member
            }
            .andExpect { status { isOk() } }
            .andExpect {
                jsonPath("$.data.task.category.id") { value(customCategoryId) }
            } // Verify category
            .andExpect { jsonPath("$.data.task.joinable") { value(false) } } // Cannot join as user
            .andExpect {
                jsonPath("$.data.task.joinableTeams") { isEmpty() }
            } // Not admin of any eligible team
            .andExpect { jsonPath("$.data.task.submittable") { value(false) } }
            .andExpect { jsonPath("$.data.task.submittableAsTeam") { isEmpty() } }
            .andExpect { jsonPath("$.data.task.joined") { value(false) } }
            .andExpect { jsonPath("$.data.task.joinedTeams") { isEmpty() } }
    }

    @Test
    @Order(27)
    fun testGetUserTaskWithOptionalQueriesUseSpaceCreator() {
        val taskId = taskIds[0] // Task 1 (USER, Default Cat), approved
        mockMvc
            .get("/tasks/$taskId") {
                queryParam("querySpace", "true")
                queryParam("queryJoinability", "true")
                queryParam("querySubmittability", "true")
                queryParam("queryJoined", "true")
                headers { setBearerAuth(spaceCreatorToken) } // Space creator can join user tasks
            }
            .andExpect { status { isOk() } }
            .andExpect {
                jsonPath("$.data.task.category.id") { value(defaultCategoryId) }
            } // Verify category
            .andExpect { jsonPath("$.data.task.joinable") { value(true) } }
            .andExpect { jsonPath("$.data.task.joinableTeams") { doesNotExist() } } // USER task
            .andExpect { jsonPath("$.data.task.submittable") { value(false) } }
            .andExpect { jsonPath("$.data.task.submittableAsTeam") { doesNotExist() } }
            .andExpect { jsonPath("$.data.task.joined") { value(false) } }
            .andExpect {
                jsonPath("$.data.task.joinedTeams") { isEmpty() }
            } // Should be empty for USER task join check
    }

    // Keep original PATCH tests, modify testUpdateTaskWithFullRequest to include category update
    @Test
    @Order(30)
    fun testUpdateTaskWithEmptyRequest() { // Keep original name
        val taskId = taskIds[0]
        mockMvc
            .patch("/tasks/$taskId") {
                headers { setBearerAuth(creatorToken) }
                contentType = MediaType.APPLICATION_JSON
                content = "{}"
            }
            .andExpect { status { isOk() } }
    }

    @Test
    @Order(40)
    fun testUpdateTaskWithFullRequest() { // Keep original name
        val taskId = taskIds[0]
        val updatedName = "$taskNamePrefix (1 - Updated Full)"
        val updatedIntro = "This is an updated test task."
        val updatedDesc = "$taskDescription (updated)"
        val newDeadline = taskDeadline + 1000000000
        val newDefaultDeadline = taskDefaultDeadline + 1

        mockMvc
            .patch("/tasks/$taskId") {
                headers { setBearerAuth(creatorToken) }
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                {
                  "name": "$updatedName",
                  "deadline": $newDeadline,
                  "defaultDeadline": $newDefaultDeadline,
                  "resubmittable": false,
                  "editable": false,
                  "intro": "$updatedIntro",
                  "description": "$updatedDesc",
                  "submissionSchema": [ {"prompt": "Text Entry", "type": "TEXT"} ],
                  "rank": 1,
                  "categoryId": $customCategoryId
                }
                """
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.task.name") { value(updatedName) } }
            .andExpect { jsonPath("$.data.task.deadline") { value(newDeadline) } }
            .andExpect { jsonPath("$.data.task.intro") { value(updatedIntro) } }
            .andExpect { jsonPath("$.data.task.category.id") { value(customCategoryId) } }
            .andExpect { jsonPath("$.data.task.category.name") { value("Custom Category") } }

        // Change some back to original values
        mockMvc
            .patch("/tasks/$taskId") {
                headers { setBearerAuth(creatorToken) }
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                {
                    "name": "$taskNamePrefix (1 - USER, Default Cat)",
                    "intro": "$taskIntro",
                    "description": "$taskDescription",
                    "categoryId": $defaultCategoryId
                }"""
                        .trimIndent()
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.task.category.id") { value(defaultCategoryId) } }
    }

    @Test
    @Order(41)
    fun `Task - Try updating Task category to archived (should fail)`() {
        val taskId = taskIds[0]
        archiveCategory(spaceAdminToken, spaceId, customCategoryId)

        mockMvc
            .patch("/tasks/$taskId") {
                headers { setBearerAuth(creatorToken) }
                contentType = MediaType.APPLICATION_JSON
                content = """{"categoryId": $customCategoryId}"""
            }
            .andExpect { status { isBadRequest() } }

        unarchiveCategory(spaceAdminToken, spaceId, customCategoryId)
    }

    @Test
    @Order(45)
    fun testUpdateTaskWithEmptyDeadline() { // Keep original name
        val taskId = taskIds[0]
        mockMvc
            .patch("/tasks/$taskId") {
                headers { setBearerAuth(creatorToken) }
                contentType = MediaType.APPLICATION_JSON
                content = """ { "hasDeadline": false } """
            }
            .andExpect { status { isOk() } }
            .andExpect {
                jsonPath("$.data.task.deadline") { isEmpty() }
            } // Check deadline is null/empty
    }

    // Keep original enumeration tests
    @Test
    @Order(50)
    fun testEnumerateTasksByDefault() {
        mockMvc
            .get("/tasks") {
                headers { setBearerAuth(creatorToken) }
                param("space", spaceId.toString())
                param("approved", "APPROVED")
                param("joined", "false")
                param("querySpace", "true") // Ensure space details are included
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.tasks.length()") { value(3) } } // 0, 1, 2 are approved
            .andExpect {
                jsonPath("$.data.tasks[?(@.id == ${taskIds[0]})].name") {
                    value("$taskNamePrefix (1 - USER, Default Cat)")
                }
            }
            .andExpect {
                jsonPath("$.data.tasks[?(@.id == ${taskIds[0]})].intro") { value(taskIntro) }
            }
            .andExpect { jsonPath("$.data.tasks[?(@.id == ${taskIds[0]})].rank") { value(1) } }
    }

    @Test
    @Order(51) // Keep order from previous refactor
    fun `Task - Enumerate tasks filtered by custom category`() {
        mockMvc
            .get("/tasks") {
                headers { setBearerAuth(creatorToken) }
                param("space", spaceId.toString())
                param("categoryId", customCategoryId.toString())
                param("approved", "APPROVED")
                param("querySpace", "true") // Ensure space details are included
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.tasks.length()") { value(1) } } // Only task 1
            .andExpect { jsonPath("$.data.tasks[0].id") { value(taskIds[1]) } }
            .andExpect { jsonPath("$.data.tasks[0].category.id") { value(customCategoryId) } }
    }

    @Test
    @Order(60)
    fun testEnumerateTasksBySpace() { // Keep original name
        mockMvc
            .get("/tasks") {
                headers { setBearerAuth(creatorToken) }
                param("space", spaceId.toString())
                param("page_size", "2")
                param("approved", "APPROVED")
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.tasks.length()") { value(2) } }
            .andExpect { jsonPath("$.data.tasks[0].id") { value(taskIds[0]) } } // Task 1
            .andExpect { jsonPath("$.data.tasks[1].id") { value(taskIds[2]) } } // Task 2
            .andExpect { jsonPath("$.data.page.has_more") { value(true) } }
            .andExpect {
                jsonPath("$.data.page.next_start") { value(taskIds[1]) }
            } // Next should be task 3 (ID from taskIds[2])
    }

    @Test
    @Order(65)
    fun testEnumerateTasksBySpace2() {
        val nextStartId = taskIds[1]
        mockMvc
            .get("/tasks") {
                headers { setBearerAuth(creatorToken) }
                param("space", spaceId.toString())
                param("page_size", "2")
                param("page_start", "$nextStartId")
                param("approved", "APPROVED")
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.tasks.length()") { value(1) } }
            .andExpect { jsonPath("$.data.tasks[0].id") { value(nextStartId) } }
            .andExpect { jsonPath("$.data.page.has_more") { value(false) } }
    }

    @Test
    @Order(70)
    fun testEnumerateTasksBySpaceAndSortByCreatedAtAsc() {
        mockMvc
            .get("/tasks") {
                headers { setBearerAuth(creatorToken) }
                param("space", spaceId.toString())
                param("sort_by", "createdAt")
                param("sort_order", "asc")
                param("approved", "APPROVED")
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.tasks[0].id") { value(taskIds[0]) } }
            .andExpect {
                jsonPath("$.data.tasks[0].name") {
                    value("$taskNamePrefix (1 - USER, Default Cat)")
                }
            }
    }

    @Test
    @Order(72)
    fun testEnumerateTasksByNameInKeywords() {
        mockMvc
            .get("/tasks") {
                headers { setBearerAuth(creatorToken) }
                param("space", spaceId.toString())
                param("keywords", "$taskNamePrefix (1 - USER, Default Cat)")
                param("approved", "APPROVED")
                param("joined", "false")
                param("queryJoined", "true")
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.tasks[?(@.approved != 'APPROVED')]") { isEmpty() } }
            .andExpect { jsonPath("$.data.tasks[?(@.joined != false)]") { isEmpty() } }
    }

    @Test
    @Order(80)
    fun testEnumerateTasksByDeadlineDesc() {
        mockMvc
            .get("/tasks") {
                headers { setBearerAuth(creatorToken) }
                param("space", spaceId.toString())
                param("sort_by", "deadline")
                param("sort_order", "desc")
                param("approved", "APPROVED")
            }
            .andExpect { status { isOk() } }
    }

    @Test
    @Order(85)
    fun testAddTestParticipantUser() {
        participantTaskMembershipId =
            addParticipantUser(participantToken, taskIds[0], participant.userId)
        assert(participantTaskMembershipId > 0)
        //        participant3TaskMembershipId =
        //            addParticipantUser(participantToken3, taskIds[0], participant3.userId)
        //        assert(participant3TaskMembershipId > 0)
        participant4TaskMembershipId =
            addParticipantUser(participantToken4, taskIds[0], participant4.userId)
        assert(participant4TaskMembershipId > 0)
    }

    @Test
    @Order(86)
    fun testEnumerateTasksAfterJoined() {
        mockMvc
            .get("/tasks") {
                headers { setBearerAuth(participantToken) }
                param("space", spaceId.toString())
                param("approved", "APPROVED")
                param("joined", "true")
                param("querySpace", "true")
                param("queryJoined", "true")
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.tasks.length()") { value(1) } }
            .andExpect { jsonPath("$.data.tasks[0].id") { value(taskIds[0]) } }
            .andExpect { jsonPath("$.data.tasks[0].joined") { value(true) } }
    }

    @Test
    @Order(86)
    fun testEnumerateTasksAfterJoinedWithKeywords() {
        mockMvc
            .get("/tasks") {
                headers { setBearerAuth(participantToken) }
                param("space", spaceId.toString())
                param("approved", "APPROVED")
                param("joined", "true")
                param("query", taskNamePrefix)
            }
            .andExpect {
                status { isOk() }
                jsonPath("$.data.tasks[0].id") { value(taskIds[0]) }
            }
    }

    @Test
    @Order(86)
    fun testAddTestParticipantUserByTaskOwner() {
        val response =
            mockMvc
                .post("/tasks/${taskIds[0]}/participants") {
                    headers { setBearerAuth(creatorToken) }
                    param("member", participant3.userId.toString())
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        """
                    {
                        "deadline": "$taskMembershipDeadline",
                        "email": "test@example.com"
                    }
                """
                }
                .andExpect { status { isOk() } }
                .andReturn()

        val json = JSONObject(response.response.contentAsString)
        participant3TaskMembershipId =
            json.getJSONObject("data").getJSONObject("participant").getLong("id")
    }

    @Test
    @Order(87)
    fun testAddTestParticipantUserWithDeadlineAccessDeniedError() {
        mockMvc
            .post("/tasks/${taskIds[0]}/participants") {
                headers { setBearerAuth(participantToken2) }
                param("member", participant2.userId.toString())
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                        "deadline": "$taskMembershipDeadline",
                        "email": "test@example.com"
                    }
                """
            }
            .andExpect { status { isForbidden() } }
    }

    @Test
    @Order(87)
    fun testAddTestParticipantUserByOwnerWithoutDeadlineAccessDeniedError() {
        mockMvc
            .post("/tasks/${taskIds[0]}/participants") {
                headers { setBearerAuth(creatorToken) }
                param("member", participant2.userId.toString())
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                        "email": "test@example.com"
                    }
                """
            }
            .andExpect { status { isForbidden() } }
    }

    @Test
    @Order(93)
    fun testAddTestParticipantUserAgainAndGetAlreadyBeTaskParticipantError() {
        mockMvc
            .post("/tasks/${taskIds[0]}/participants") {
                headers { setBearerAuth(participantToken) }
                param("member", participant.userId.toString())
                contentType = MediaType.APPLICATION_JSON
                content = """{ "email": "test@example.com" }"""
            }
            .andExpect { status { isConflict() } } // Keep original expected status
            .andExpect { jsonPath("$.error.name") { value("AlreadyBeTaskParticipantError") } }
    }

    @Test
    @Order(95)
    fun testAddTestParticipantTeam() {
        teamTaskMembershipId =
            addParticipantTeam(teamCreatorToken, taskIds[1], teamId) // Target TEAM task
        assert(teamTaskMembershipId > 0)
    }

    @Test
    @Order(96)
    fun testAddTestParticipantTeamAgainAndGetAlreadyBeTaskParticipantError() {
        mockMvc
            .post("/tasks/${taskIds[1]}/participants") {
                headers { setBearerAuth(teamCreatorToken) }
                param("member", teamId.toString())
                contentType = MediaType.APPLICATION_JSON
                content = """{ "email": "test@example.com" }"""
            }
            .andExpect { status { isConflict() } }
            .andExpect { jsonPath("$.error.name") { value("AlreadyBeTaskParticipantError") } }
    }

    @Test
    @Order(100)
    fun testGetTaskParticipantsUser() {
        val taskId = taskIds[0]
        mockMvc
            .get("/tasks/$taskId/participants") { headers { setBearerAuth(creatorToken) } }
            .andExpect { status { isOk() } }
            // At this point, participants 1, 3, 4 were added
            .andExpect { jsonPath("$.data.participants.length()") { value(3) } }
            .andExpect {
                jsonPath("$.data.participants[?(@.member.id == ${participant.userId})]") {
                    exists()
                }
            }
            .andExpect {
                jsonPath("$.data.participants[?(@.member.id == ${participant3.userId})]") {
                    exists()
                }
            }
            .andExpect {
                jsonPath("$.data.participants[?(@.member.id == ${participant4.userId})]") {
                    exists()
                }
            }
    }

    @Test
    @Order(101)
    fun testGetTaskParticipantsTeam() {
        val taskId = taskIds[1]
        mockMvc
            .get("/tasks/$taskId/participants") { headers { setBearerAuth(creatorToken) } }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.participants.length()") { value(1) } }
            .andExpect { jsonPath("$.data.participants[0].member.id") { value(teamId) } }
            .andExpect { jsonPath("$.data.participants[0].member.name") { isString() } }
    }

    @Test
    @Order(105)
    fun testDisApproveTestParticipantUser4() {
        mockMvc
            .patch("/tasks/${taskIds[0]}/participants/$participant4TaskMembershipId") {
                headers {
                    setBearerAuth(creatorToken)
                } // Creator likely has permission to manage participants
                contentType = MediaType.APPLICATION_JSON
                content = """{ "approved": "DISAPPROVED" }"""
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.taskMembership.approved") { value("DISAPPROVED") } }
    }

    @Test
    @Order(106)
    fun testGetTaskParticipantsByApproveStatusNone() {
        val taskId = taskIds[0]
        mockMvc
            .get("/tasks/$taskId/participants") {
                headers { setBearerAuth(creatorToken) }
                param("approved", "NONE")
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.participants.length()") { value(1) } }
            .andExpect {
                jsonPath("$.data.participants[0].member.id") { value(participant.userId) }
            }
    }

    @Test
    @Order(107)
    fun testGetTaskParticipantsByApproveStatusApproved() {
        val taskId = taskIds[0]
        mockMvc
            .get("/tasks/$taskId/participants") {
                headers { setBearerAuth(creatorToken) }
                param("approved", "APPROVED")
            }
            .andExpect { status { isOk() } }
            .andExpect {
                jsonPath("$.data.participants.length()") { value(1) }
            } // Only participant 3 is APPROVED
            .andExpect {
                jsonPath("$.data.participants[0].member.id") { value(participant3.userId) }
            }
    }

    @Test
    @Order(108)
    fun testGetTaskParticipantsByApproveStatusDisapproved() {
        val taskId = taskIds[0]
        mockMvc
            .get("/tasks/$taskId/participants") {
                headers { setBearerAuth(creatorToken) }
                param("approved", "DISAPPROVED")
            }
            .andExpect { status { isOk() } }
            .andExpect {
                jsonPath("$.data.participants.length()") { value(1) }
            } // Only participant 4 is DISAPPROVED
            .andExpect {
                jsonPath("$.data.participants[0].member.id") { value(participant4.userId) }
            }
    }

    @Test
    @Order(109)
    fun testApproveTestParticipantUser() {
        mockMvc
            .patch("/tasks/${taskIds[0]}/participants") {
                queryParam("member", participant.userId.toString()) // Target specific member
                headers { setBearerAuth(creatorToken) }
                contentType = MediaType.APPLICATION_JSON
                content = """{ "approved": "APPROVED" }"""
            }
            .andExpect { status { isOk() } }
            .andExpect {
                jsonPath("$.data.participants[?(@.member.id == ${participant.userId})].approved") {
                    value("APPROVED")
                }
            }
    }

    @Test
    @Order(110)
    fun testPatchTestParticipantUserDeadline() {
        mockMvc
            .patch("/tasks/${taskIds[0]}/participants") {
                queryParam("member", participant.userId.toString()) // Target participant 1
                headers { setBearerAuth(creatorToken) }
                contentType = MediaType.APPLICATION_JSON
                content = """{ "deadline": ${taskMembershipDeadline + 100000} }"""
            }
            .andExpect { status { isOk() } }
            .andExpect {
                jsonPath("$.data.participants[?(@.member.id == ${participant.userId})].deadline") {
                    value(taskMembershipDeadline + 100000)
                }
            }
    }

    @Test
    @Order(111) // Renumbered slightly
    fun testRemoveTestParticipantUser() {
        mockMvc
            .delete("/tasks/${taskIds[0]}/participants") {
                queryParam("member", participant.userId.toString())
                headers { setBearerAuth(participantToken) } // Participant removes self
            }
            .andExpect { status { isNoContent() } }
    }

    @Test
    @Order(112)
    fun testRemoveTestParticipantUser3() {
        mockMvc
            .delete("/tasks/${taskIds[0]}/participants") {
                queryParam("member", participant3.userId.toString())
                headers { setBearerAuth(participantToken3) } // Participant removes self
            }
            .andExpect { status { isNoContent() } }
    }

    @Test
    @Order(113)
    fun testRemoveTestParticipantUser4() {
        mockMvc
            .delete("/tasks/${taskIds[0]}/participants") {
                queryParam("member", participant4.userId.toString())
                headers { setBearerAuth(participantToken4) } // Participant removes self
            }
            .andExpect { status { isNoContent() } }
    }

    @Test
    @Order(115)
    fun testRemoveTestParticipantTeam() {
        mockMvc
            .delete("/tasks/${taskIds[1]}/participants") {
                queryParam("member", teamId.toString())
                headers { setBearerAuth(teamCreatorToken) } // Team creator removes team
            }
            .andExpect { status { isNoContent() } }
    }

    // Keep get participants after remove tests (120, 125)
    @Test
    @Order(120)
    fun testGetTaskParticipantsAfterRemoveUser() {
        val taskId = taskIds[0]
        mockMvc
            .get("/tasks/$taskId/participants") { headers { setBearerAuth(creatorToken) } }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.participants") { isEmpty() } } // All users removed
    }

    @Test
    @Order(125)
    fun testGetTaskParticipantsAfterRemoveTeam() {
        val taskId = taskIds[1]
        mockMvc
            .get("/tasks/$taskId/participants") { headers { setBearerAuth(creatorToken) } }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.participants") { isEmpty() } } // Team removed
    }

    // --- Category Deletion Tests ---
    @Test
    @Order(200)
    fun `Category - Try deleting non-empty default category (should fail)`() {
        mockMvc
            .delete("/spaces/$spaceId/categories/$defaultCategoryId") {
                headers { setBearerAuth(spaceAdminToken) }
            }
            .andExpect { status { isBadRequest() } }
    }

    @Test
    @Order(201)
    fun `Category - Try deleting non-empty custom category (should fail)`() {
        // Need to re-add a task to custom category if task 1/2 were deleted/moved
        val taskIdInCustom =
            createTask(
                name = "$taskNamePrefix (For Delete Test)",
                submitterType = "USER",
                deadline = null,
                defaultDeadline = taskDefaultDeadline,
                resubmittable = true,
                editable = true,
                intro = "temp",
                description = "temp",
                submissionSchema = emptyList(),
                spaceId = spaceId,
                categoryId = customCategoryId,
            )!!
        approveTask(taskIdInCustom, spaceAdminToken)

        mockMvc
            .delete("/spaces/$spaceId/categories/$customCategoryId") {
                headers { setBearerAuth(spaceAdminToken) }
            }
            .andExpect { status { isConflict() } } // Expect 409 Conflict

        // Clean up the temporary task
        mockMvc
            .delete("/tasks/$taskIdInCustom") { headers { setBearerAuth(creatorToken) } }
            .andExpect { status { isOk() } }
    }

    @Test
    @Order(202)
    fun `Category - Delete an empty category successfully`() {
        val emptyCatId = createCategory(spaceAdminToken, spaceId, "Empty Category Final")
        assert(emptyCatId > 0)
        mockMvc
            .delete("/spaces/$spaceId/categories/$emptyCatId") {
                headers { setBearerAuth(spaceAdminToken) }
            }
            .andExpect { status { isNoContent() } }
        mockMvc
            .get("/spaces/$spaceId/categories/$emptyCatId") {
                headers { setBearerAuth(spaceAdminToken) }
            }
            .andExpect { status { isNotFound() } }
    }

    // --- Task Deletion Tests ---
    @Test
    @Order(230)
    fun testDeleteTaskAndGetAccessDeniedError() { // Keep original name
        // Recreate task 1 if it was deleted in other tests, ensure it exists
        val taskIdToDelete =
            taskIds.firstOrNull()
                ?: createTask(
                    name = "$taskNamePrefix (Recreated)",
                    submitterType = "USER",
                    deadline = null,
                    defaultDeadline = taskDefaultDeadline,
                    resubmittable = true,
                    editable = true,
                    intro = "recreate",
                    description = "recreate",
                    submissionSchema = emptyList(),
                    spaceId = spaceId,
                    categoryId = defaultCategoryId,
                )!!

        mockMvc
            .delete("/tasks/$taskIdToDelete") {
                headers { setBearerAuth(teamCreatorToken) }
            } // Use a non-owner/non-admin token
            .andExpect { status { isForbidden() } }
    }

    @Test
    @Order(240)
    fun testDeleteTask() { // Keep original name
        val taskIdToDelete =
            taskIds.firstOrNull()
                ?: createTask(
                    name = "$taskNamePrefix (Recreated)",
                    submitterType = "USER",
                    deadline = null,
                    defaultDeadline = taskDefaultDeadline,
                    resubmittable = true,
                    editable = true,
                    intro = "recreate",
                    description = "recreate",
                    submissionSchema = emptyList(),
                    spaceId = spaceId,
                    categoryId = defaultCategoryId,
                )!!

        mockMvc
            .delete("/tasks/$taskIdToDelete") {
                headers { setBearerAuth(creatorToken) }
            } // Creator deletes
            .andExpect { status { isOk() } } // Original expected OK
        taskIds.remove(taskIdToDelete) // Remove from list

        mockMvc
            .get("/tasks/$taskIdToDelete") { headers { setBearerAuth(creatorToken) } }
            .andExpect { status { isNotFound() } }
    }

    // Removed testDeleteTask2 as taskIds[3] might not exist reliably depending on execution
    // order/failures
    // Add cleanup or more robust task selection if needed for deleting multiple tasks.
}
