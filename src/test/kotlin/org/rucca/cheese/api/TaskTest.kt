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
import kotlin.math.floor
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.utils.JsonArrayUtil
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class TaskTest
@Autowired
constructor(private val mockMvc: MockMvc, private val userCreatorService: UserCreatorService) {
    private val logger = LoggerFactory.getLogger(javaClass)
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
    lateinit var participant3: UserCreatorService.CreateUserResponse
    lateinit var participantToken3: String
    lateinit var participant4: UserCreatorService.CreateUserResponse
    lateinit var participantToken4: String
    lateinit var spaceAdmin: UserCreatorService.CreateUserResponse
    lateinit var spaceAdminToken: String
    lateinit var teamAdmin: UserCreatorService.CreateUserResponse
    lateinit var teamAdminToken: String
    private var teamId: IdType = -1
    private var spaceId: IdType = -1
    private val taskIds = mutableListOf<IdType>()
    private val taskName = "Test Task (${floor(Math.random() * 10000000000).toLong()})"
    private val taskIntro = "This is a test task."
    private val taskDescription = "A lengthy text. ".repeat(1000)
    private val taskDeadline = LocalDateTime.now().plusDays(7).toEpochMilli()
    private val taskDefaultDeadline = 30L
    private val taskMembershipDeadline = LocalDateTime.now().plusMonths(1).toEpochMilli()
    private val taskSubmissionSchema =
        listOf(Pair("Text Entry", "TEXT"), Pair("Attachment Entry", "FILE"))
    private var participantTaskMembershipId: IdType = -1
    private var participant2TaskMembershipId: IdType = -1
    private var participant3TaskMembershipId: IdType = -1
    private var participant4TaskMembershipId: IdType = -1
    private var teamTaskMembershipId: IdType = -1

    fun createSpace(
        creatorToken: String,
        spaceName: String,
        spaceIntro: String,
        spaceDescription: String,
        spaceAvatarId: IdType,
    ): IdType {
        val request =
            MockMvcRequestBuilders.post("/spaces")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "name": "$spaceName",
                    "intro": "$spaceIntro",
                    "description": "$spaceDescription",
                    "avatarId": $spaceAvatarId,
                    "announcements": "[]",
                    "taskTemplates": "[]"
                }
            """
                )
        val spaceId =
            JSONObject(mockMvc.perform(request).andReturn().response.contentAsString)
                .getJSONObject("data")
                .getJSONObject("space")
                .getLong("id")
        logger.info("Created space: $spaceId")
        return spaceId
    }

    fun addSpaceAdmin(creatorToken: String, spaceId: IdType, adminId: IdType) {
        val request =
            MockMvcRequestBuilders.post("/spaces/$spaceId/managers")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "role": "ADMIN",
                    "userId": ${adminId}
                }
            """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    fun createTeam(
        creatorToken: String,
        teamName: String,
        teamIntro: String,
        teamDescription: String,
        teamAvatarId: IdType,
    ): IdType {
        val request =
            MockMvcRequestBuilders.post("/teams")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "name": "$teamName",
                  "intro": "$teamIntro",
                  "description": "$teamDescription",
                  "avatarId": $teamAvatarId
                }
            """
                )
        teamId =
            JSONObject(mockMvc.perform(request).andReturn().response.contentAsString)
                .getJSONObject("data")
                .getJSONObject("team")
                .getLong("id")
        logger.info("Created team: $teamId")
        return teamId
    }

    fun joinTeam(token: String, teamId: IdType, userId: IdType) {
        val request =
            MockMvcRequestBuilders.post("/teams/$teamId/members")
                .header("Authorization", "Bearer $token")
                .contentType("application/json")
                .content(
                    """
                {
                  "role": "MEMBER",
                  "user_id": ${userId}
                }
            """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    fun addTeamAdmin(creatorToken: String, teamId: IdType, adminId: IdType) {
        val request =
            MockMvcRequestBuilders.post("/teams/$teamId/members")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "role": "ADMIN",
                  "user_id": ${adminId}
                }
            """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
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
        spaceId =
            createSpace(
                creatorToken = spaceCreatorToken,
                spaceName = "Test Space (${floor(Math.random() * 10000000000).toLong()})",
                spaceIntro = "This is a test space.",
                spaceDescription = "A lengthy text. ".repeat(1000),
                spaceAvatarId = userCreatorService.testAvatarId(),
            )
        teamId =
            createTeam(
                creatorToken = teamCreatorToken,
                teamName = "Test Team (${floor(Math.random() * 10000000000).toLong()})",
                teamIntro = "This is a test team.",
                teamDescription = "A lengthy text. ".repeat(1000),
                teamAvatarId = userCreatorService.testAvatarId(),
            )
        joinTeam(teamCreatorToken, teamId, teamMember.userId)
        addSpaceAdmin(spaceCreatorToken, spaceId, spaceAdmin.userId)
        addTeamAdmin(teamCreatorToken, teamId, teamAdmin.userId)
    }

    fun createTask(
        name: String,
        submitterType: String,
        deadline: Long?,
        defaultDeadline: Long?,
        resubmittable: Boolean,
        editable: Boolean,
        intro: String,
        description: String,
        submissionSchema: List<Pair<String, String>>,
        space: IdType?,
    ) {
        val request =
            MockMvcRequestBuilders.post("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "name": "$name",
                  "submitterType": "$submitterType",
                  "deadline": "$deadline",
                  "defaultDeadline": $defaultDeadline,
                  "resubmittable": $resubmittable,
                  "editable": $editable,
                  "intro": "$intro",
                  "description": "$description",
                  "submissionSchema": [
                    ${
                        submissionSchema.map {
                            """
                                {
                                  "prompt": "${it.first}",
                                  "type": "${it.second}"
                                }
                            """
                        }.joinToString(",\n")
                    }
                  ],
                  "space": ${space ?: "null"}
                }
            """
                )
        val response =
            mockMvc
                .perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(jsonPath("$.data.task.name").value(name))
                .andExpect(jsonPath("$.data.task.submitterType").value(submitterType))
                .andExpect(jsonPath("$.data.task.creator.id").value(creator.userId))
                .andExpect(jsonPath("$.data.task.deadline").value(deadline))
                .andExpect(jsonPath("$.data.task.defaultDeadline").value(defaultDeadline))
                .andExpect(jsonPath("$.data.task.resubmittable").value(resubmittable))
                .andExpect(jsonPath("$.data.task.editable").value(editable))
                .andExpect(jsonPath("$.data.task.intro").value(intro))
                .andExpect(jsonPath("$.data.task.description").value(description))
        val json = JSONObject(response.andReturn().response.contentAsString)
        for (entry in submissionSchema) {
            val schema =
                json.getJSONObject("data").getJSONObject("task").getJSONArray("submissionSchema")
            val found = JsonArrayUtil.toArray(schema).find { it.getString("prompt") == entry.first }
            assert(found != null)
            assert(found!!.getString("type") == entry.second)
        }
        val taskId = json.getJSONObject("data").getJSONObject("task").getLong("id")
        taskIds.add(taskId)
        logger.info("Created task: $taskId")
    }

    fun approveTask(taskId: IdType, token: String) {
        val request =
            MockMvcRequestBuilders.patch("/tasks/$taskId")
                .header("Authorization", "Bearer $token")
                .contentType("application/json")
                .content(
                    """
                {
                  "approved": "APPROVED"
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.task.approved").value("APPROVED"))
    }

    fun addParticipantUser(token: String, taskId: IdType, userId: IdType): IdType {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskId}/participants")
                .header("Authorization", "Bearer $token")
                .queryParam("member", userId.toString())
                .contentType("application/json")
                .content(
                    """
                    {
                        "email": "test@example.com"
                    }
                """
                )
        val response = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
        val json = JSONObject(response.andReturn().response.contentAsString)
        val participantId = json.getJSONObject("data").getJSONObject("participant").getLong("id")
        return participantId
    }

    fun addParticipantTeam(token: String, taskId: IdType, teamId: IdType): IdType {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskId}/participants")
                .header("Authorization", "Bearer $token")
                .queryParam("member", teamId.toString())
                .contentType("application/json")
                .content(
                    """
                    {
                        "email": "test@example.com"
                    }
                """
                )
        val response = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
        val json = JSONObject(response.andReturn().response.contentAsString)
        val participantId = json.getJSONObject("data").getJSONObject("participant").getLong("id")
        return participantId
    }

    fun approveTaskParticipant(token: String, taskId: IdType, participantId: IdType) {
        val request =
            MockMvcRequestBuilders.patch("/tasks/${taskId}/participants/${participantId}")
                .header("Authorization", "Bearer $token")
                .contentType("application/json")
                .content(
                    """
                {
                  "approved": "APPROVED"
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.taskMembership.approved").value("APPROVED"))
    }

    @Test
    @Order(10)
    fun testCreateTask() {
        createTask(
            name = "$taskName (1)",
            submitterType = "USER",
            deadline = taskDeadline,
            defaultDeadline = taskDefaultDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            space = spaceId,
        )
        createTask(
            name = "$taskName (2)",
            submitterType = "TEAM",
            deadline = taskDeadline,
            defaultDeadline = taskDefaultDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            space = spaceId,
        )
        createTask(
            name = "$taskName (3)",
            submitterType = "USER",
            deadline = taskDeadline,
            defaultDeadline = taskDefaultDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            space = spaceId,
        )
        createTask(
            name = "$taskName (4)",
            submitterType = "USER",
            deadline = taskDeadline,
            defaultDeadline = taskDefaultDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            space = spaceId,
        )
    }

    @Test
    @Order(13)
    fun testEnumerateOwnedTasks() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .param("owner", creator.userId.toString())
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.tasks.length()").value(4))
    }

    @Test
    @Order(14)
    fun testEnumerateUnapprovedTasks() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $spaceAdminToken")
                .param("approved", "NONE")
                .param("space", spaceId.toString())
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.tasks.length()").value(4))
    }

    @Test
    @Order(15)
    fun testEnumerateUnapprovedTasksWithoutAdmin() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .param("approved", "NONE")
                .param("space", spaceId.toString())
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @Order(16)
    fun testGetUnapprovedTask() {
        val taskId = taskIds[3]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId")
                .queryParam("queryJoinability", "true")
                .queryParam("querySubmittability", "true")
                .header("Authorization", "Bearer $spaceAdminToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.task.name").value("$taskName (4)"))
    }

    @Test
    @Order(17)
    fun testGetUnapprovedTaskAccessDeniedError() {
        val taskId = taskIds[3]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId")
                .queryParam("queryJoinability", "true")
                .queryParam("querySubmittability", "true")
                .header("Authorization", "Bearer $participantToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(jsonPath("$.error.name").value("AccessDeniedError"))
    }

    @Test
    @Order(17)
    fun testJoinUnapprovedTaskForbiddenError() {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskIds[0]}/participants")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .contentType("application/json")
                .content(
                    """
                    {
                      "email": "test@example.com"
                    }
                """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(jsonPath("$.error.name").value("ForbiddenError"))
    }

    @Test
    @Order(18)
    fun testApproveTask() {
        approveTask(taskIds[0], spaceAdminToken)
        approveTask(taskIds[1], spaceAdminToken)
    }

    @Test
    @Order(19)
    fun testApproveTaskWithoutAdmin() {
        val taskId = taskIds[2]
        val request =
            MockMvcRequestBuilders.patch("/tasks/$taskId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "approved": "APPROVED"
                }
            """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isForbidden)
        approveTask(taskIds[2], spaceAdminToken)
    }

    @Test
    @Order(20)
    fun testGetTask() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId")
                .header("Authorization", "Bearer $creatorToken")
        val response =
            mockMvc
                .perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(jsonPath("$.data.task.name").value("$taskName (1)"))
                .andExpect(jsonPath("$.data.task.submitterType").value("USER"))
                .andExpect(jsonPath("$.data.task.creator.id").value(creator.userId))
                .andExpect(jsonPath("$.data.task.deadline").value(taskDeadline))
                .andExpect(jsonPath("$.data.task.resubmittable").value(true))
                .andExpect(jsonPath("$.data.task.editable").value(true))
                .andExpect(jsonPath("$.data.task.description").value(taskDescription))
        val json = JSONObject(response.andReturn().response.contentAsString)
        for (entry in taskSubmissionSchema) {
            val schema =
                json.getJSONObject("data").getJSONObject("task").getJSONArray("submissionSchema")
            val found = JsonArrayUtil.toArray(schema).find { it.getString("prompt") == entry.first }
            assert(found != null)
            assert(found!!.getString("type") == entry.second)
        }
    }

    @Test
    @Order(21)
    fun testPatchRejectReasonWithoutAdmin() {
        val taskId = taskIds[2]
        val request =
            MockMvcRequestBuilders.patch("/tasks/$taskId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "rejectReason": "Garbage."
                }
            """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @Order(22)
    fun testPatchRejectReason() {
        val taskId = taskIds[2]
        val request =
            MockMvcRequestBuilders.patch("/tasks/$taskId")
                .header("Authorization", "Bearer $spaceAdminToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "rejectReason": "Garbage."
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.task.rejectReason").value("Garbage."))
    }

    @Test
    @Order(25)
    fun testGetTeamTaskWithOptionalQueriesUseTeamCreator() {
        val taskId = taskIds[1]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId")
                .queryParam("querySpace", "true")
                .queryParam("queryTeam", "true")
                .queryParam("queryJoinability", "true")
                .queryParam("querySubmittability", "true")
                .queryParam("queryJoined", "true")
                .queryParam("queryUserDeadline", "true")
                .header("Authorization", "Bearer $teamCreatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.task.space.id").value(spaceId))
            .andExpect(jsonPath("$.data.task.joinable").value(true))
            .andExpect(jsonPath("$.data.task.joinableAsTeam[0].id").value(teamId))
            .andExpect(jsonPath("$.data.task.submittable").value(false))
            .andExpect(jsonPath("$.data.task.submittableAsTeam").isEmpty)
            .andExpect(jsonPath("$.data.task.joined").value(false))
            .andExpect(jsonPath("$.data.task.joinedTeams").isEmpty)
            .andExpect(jsonPath("$.data.task.userDeadline").isEmpty)
    }

    @Test
    @Order(26)
    fun testGetTeamTaskWithOptionalQueriesUseSpaceCreator() {
        val taskId = taskIds[1]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId")
                .queryParam("queryJoinability", "true")
                .queryParam("querySubmittability", "true")
                .queryParam("queryJoined", "true")
                .header("Authorization", "Bearer $spaceCreatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.task.joinable").value(false))
            .andExpect(jsonPath("$.data.task.joinableAsTeam").isEmpty)
            .andExpect(jsonPath("$.data.task.submittable").value(false))
            .andExpect(jsonPath("$.data.task.submittableAsTeam").isEmpty)
            .andExpect(jsonPath("$.data.task.joined").value(false))
            .andExpect(jsonPath("$.data.task.joinedTeams").isEmpty)
    }

    @Test
    @Order(27)
    fun testGetUserTaskWithOptionalQueriesUseSpaceCreator() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId")
                .queryParam("queryJoinability", "true")
                .queryParam("querySubmittability", "true")
                .queryParam("queryJoined", "true")
                .header("Authorization", "Bearer $spaceCreatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.task.joinable").value(true))
            .andExpect(jsonPath("$.data.task.joinableAsTeam").doesNotExist())
            .andExpect(jsonPath("$.data.task.submittable").value(false))
            .andExpect(jsonPath("$.data.task.submittableAsTeam").doesNotExist())
            .andExpect(jsonPath("$.data.task.joined").value(false))
            .andExpect(jsonPath("$.data.task.joinedTeams").isEmpty)
    }

    @Test
    @Order(30)
    fun testUpdateTaskWithEmptyRequest() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.patch("/tasks/$taskId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content("{}")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(40)
    fun testUpdateTaskWithFullRequest() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.patch("/tasks/$taskId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "name": "$taskName (1) (updated)",
                  "deadline": ${taskDeadline + 1000000000},
                  "defaultDeadline": ${taskDefaultDeadline + 1},
                  "resubmittable": false,
                  "editable": false,
                  "intro": "This is an updated test task.",
                  "description": "${taskDescription} (updated)",
                  "submissionSchema": [
                    {
                      "prompt": "Text Entry",
                      "type": "TEXT"
                    }
                  ],
                  "rank": 1
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.task.name").value("$taskName (1) (updated)"))
            .andExpect(jsonPath("$.data.task.creator.id").value(creator.userId))
            .andExpect(jsonPath("$.data.task.deadline").value(taskDeadline + 1000000000))
            .andExpect(jsonPath("$.data.task.defaultDeadline").value(taskDefaultDeadline + 1))
            .andExpect(jsonPath("$.data.task.resubmittable").value(false))
            .andExpect(jsonPath("$.data.task.editable").value(false))
            .andExpect(jsonPath("$.data.task.intro").value("This is an updated test task."))
            .andExpect(jsonPath("$.data.task.description").value("${taskDescription} (updated)"))
            .andExpect(jsonPath("$.data.task.submissionSchema[0].prompt").value("Text Entry"))
            .andExpect(jsonPath("$.data.task.submissionSchema[0].type").value("TEXT"))
            .andExpect(jsonPath("$.data.task.rank").value(1))
    }

    @Test
    @Order(45)
    fun testUpdateTaskWithEmptyDeadline() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.patch("/tasks/$taskId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "hasDeadline": false
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.task.deadline").isEmpty)
    }

    @Test
    @Order(50)
    fun testEnumerateTasksByDefault() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .param("approved", "APPROVED")
                .param("joined", "false")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.tasks[0].id").value(taskIds[0]))
            .andExpect(jsonPath("$.data.tasks[0].name").value("$taskName (1) (updated)"))
            .andExpect(jsonPath("$.data.tasks[0].intro").value("This is an updated test task."))
            .andExpect(jsonPath("$.data.tasks[0].description").value("$taskDescription (updated)"))
            .andExpect(jsonPath("$.data.tasks[0].deadline").isEmpty)
            .andExpect(jsonPath("$.data.tasks[0].submitters.total").value(0))
            .andExpect(jsonPath("$.data.tasks[0].submitters.examples").isArray)
            .andExpect(jsonPath("$.data.tasks[0].rank").value(1))
    }

    @Test
    @Order(50)
    fun testEnumerateTasksNotJoined() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .param("approved", "APPROVED")
                .param("joined", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.tasks").isEmpty)
    }

    @Test
    @Order(60)
    fun testEnumerateTasksBySpace() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .param("space", spaceId.toString())
                .param("page_size", "2")
                .param("approved", "APPROVED")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.tasks[0].id").value(taskIds[0]))
            .andExpect(jsonPath("$.data.tasks[0].name").value("$taskName (1) (updated)"))
            .andExpect(jsonPath("$.data.tasks[1].name").value("$taskName (3)"))
            .andExpect(jsonPath("$.data.page.page_start").value(taskIds[0]))
            .andExpect(jsonPath("$.data.page.page_size").value(2))
            .andExpect(jsonPath("$.data.page.has_more").value(true))
            .andExpect(jsonPath("$.data.page.next_start").value(taskIds[1]))
    }

    @Test
    @Order(65)
    fun testEnumerateTasksBySpace2() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .param("space", spaceId.toString())
                .param("page_size", "2")
                .param("page_start", "${taskIds[2]}")
                .param("approved", "APPROVED")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.tasks[0].name").value("$taskName (3)"))
            .andExpect(jsonPath("$.data.tasks[1].name").value("$taskName (2)"))
            .andExpect(jsonPath("$.data.page.page_start").value(taskIds[2]))
            .andExpect(jsonPath("$.data.page.page_size").value(2))
    }

    @Test
    @Order(70)
    fun testEnumerateTasksBySpaceAndSortByCreatedAtAsc() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .param("space", spaceId.toString())
                .param("sort_by", "createdAt")
                .param("sort_order", "asc")
                .param("approved", "APPROVED")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.tasks[0].id").value(taskIds[0]))
            .andExpect(jsonPath("$.data.tasks[0].name").value("$taskName (1) (updated)"))
    }

    @Test
    @Order(72)
    fun testEnumerateTasksByNameInKeywords() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .param("keywords", "$taskName (1) (updated)")
                .param("approved", "APPROVED")
                .param("joined", "false")
                .param("queryJoined", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.tasks[?(@.approved != 'APPROVED')]").isEmpty)
            .andExpect(jsonPath("$.data.tasks[?(@.joined != false)]").isEmpty)
    }

    @Test
    @Order(80)
    fun testEnumerateTasksByDeadlineDesc() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .param("sort_by", "deadline")
                .param("sort_order", "desc")
                .param("approved", "APPROVED")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(85)
    fun testAddTestParticipantUser() {
        participantTaskMembershipId =
            addParticipantUser(participantToken, taskIds[0], participant.userId)
    }

    @Test
    @Order(86)
    fun testEnumerateTasksAfterJoined() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $participantToken")
                .param("approved", "APPROVED")
                .param("joined", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.tasks[0].id").value(taskIds[0]))
    }

    @Test
    @Order(86)
    fun testEnumerateTasksAfterJoinedWithKeywords() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $participantToken")
                .param("approved", "APPROVED")
                .param("joined", "true")
                .param("query", taskName)
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.tasks[0].id").value(taskIds[0]))
    }

    @Test
    @Order(86)
    fun testAddTestParticipantUserByTaskOwner() {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskIds[0]}/participants")
                .header("Authorization", "Bearer $creatorToken")
                .queryParam("member", participant3.userId.toString())
                .contentType("application/json")
                .content(
                    """
                    {
                        "deadline": "$taskMembershipDeadline",
                        "email": "test@example.com"
                    }
                """
                )
        val response = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
        val json = JSONObject(response.andReturn().response.contentAsString)
        participant3TaskMembershipId =
            json.getJSONObject("data").getJSONObject("participant").getLong("id")
    }

    @Test
    @Order(87)
    fun testAddTestParticipantUserWithDeadlineAccessDeniedError() {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskIds[0]}/participants")
                .header("Authorization", "Bearer $participantToken2")
                .queryParam("member", participant2.userId.toString())
                .contentType("application/json")
                .content(
                    """
                    {
                        "deadline": "$taskMembershipDeadline",
                        "email": "test@example.com"
                    }
                """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @Order(87)
    fun testAddTestParticipantUserByOwnerWithoutDeadlineAccessDeniedError() {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskIds[0]}/participants")
                .header("Authorization", "Bearer $creatorToken")
                .queryParam("member", participant2.userId.toString())
                .contentType("application/json")
                .content(
                    """
                    {
                        "email": "test@example.com"
                    }
                """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @Order(93)
    fun testAddTestParticipantUserAgainAndGetAlreadyBeTaskParticipantError() {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskIds[0]}/participants")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .contentType("application/json")
                .content(
                    """
                    {
                      "email": "test@example.com"
                    }
                """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isConflict)
            .andExpect(jsonPath("$.error.name").value("AlreadyBeTaskParticipantError"))
    }

    @Test
    @Order(94)
    fun testAddTestParticipantTeamAndGetAccessDeniedError() {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskIds[1]}/participants")
                .header("Authorization", "Bearer $teamMemberToken")
                .queryParam("member", teamId.toString())
                .contentType("application/json")
                .content(
                    """
                    {
                        "deadline": "$taskMembershipDeadline",
                        "email": "test@example.com"
                    }
                """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(jsonPath("$.error.name").value("AccessDeniedError"))
    }

    @Test
    @Order(95)
    fun testAddTestParticipantTeam() {
        teamTaskMembershipId = addParticipantTeam(teamCreatorToken, taskIds[1], teamId)
    }

    @Test
    @Order(96)
    fun testAddTestParticipantTeamAgainAndGetAlreadyBeTaskParticipantError() {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskIds[1]}/participants")
                .header("Authorization", "Bearer $teamCreatorToken")
                .queryParam("member", teamId.toString())
                .contentType("application/json")
                .content(
                    """
                    {
                        "email": "test@example.com"
                    }
                """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isConflict)
            .andExpect(jsonPath("$.error.name").value("AlreadyBeTaskParticipantError"))
    }

    @Test
    @Order(100)
    fun testGetTaskParticipantsUser() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/participants")
                .header("Authorization", "Bearer $creatorToken")
        // .param("approved","APPROVED")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                jsonPath("$.data.participants[?(@.member.id == ${participant.userId})]").exists()
            )
    }

    @Test
    @Order(101)
    fun testGetTaskParticipantsTeam() {
        val taskId = taskIds[1]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/participants")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.participants[0].member.id").value(teamId))
            .andExpect(jsonPath("$.data.participants[0].member.intro").isString)
            .andExpect(jsonPath("$.data.participants[0].member.name").isString)
            .andExpect(
                jsonPath("$.data.participants[0].member.avatarId")
                    .value(userCreatorService.testAvatarId())
            )
    }

    @Test
    @Order(102)
    fun testGetTeamTaskWithOptionalQueriesUseTeamCreatorAfterJoin() {
        val taskId = taskIds[1]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId")
                .queryParam("queryJoinability", "true")
                .queryParam("querySubmittability", "true")
                .queryParam("queryJoined", "true")
                .queryParam("queryUserDeadline", "true")
                .header("Authorization", "Bearer $teamCreatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.task.joinable").value(false))
            .andExpect(jsonPath("$.data.task.joinableAsTeam").isEmpty)
            .andExpect(jsonPath("$.data.task.submittable").value(false))
            .andExpect(jsonPath("$.data.task.submittableAsTeam").isEmpty())
            .andExpect(jsonPath("$.data.task.joined").value(true))
            .andExpect(jsonPath("$.data.task.joinedTeams[0].id").value(teamId))
            .andExpect(jsonPath("$.data.task.userDeadline").isEmpty)
    }

    @Test
    @Order(104)
    fun testAddTestParticipantUser4() {
        participant4TaskMembershipId =
            addParticipantUser(participantToken4, taskIds[0], participant4.userId)
    }

    @Test
    @Order(105)
    fun testDisApproveTestParticipantUser4() {
        val request =
            MockMvcRequestBuilders.patch(
                    "/tasks/${taskIds[0]}/participants/$participant4TaskMembershipId"
                )
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "approved": "DISAPPROVED"
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.taskMembership.approved").value("DISAPPROVED"))
    }

    @Test
    @Order(106)
    fun testGetTaskParticipantsByApproveStatusNone() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/participants")
                .header("Authorization", "Bearer $creatorToken")
                .param("approved", "NONE")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.participants.length()").value(1))
            .andExpect(jsonPath("$.data.participants[0].member.id").value(participant.userId))
            .andExpect(jsonPath("$.data.participants[0].member.intro").isString)
            .andExpect(jsonPath("$.data.participants[0].member.name").isString)
            .andExpect(
                jsonPath("$.data.participants[0].member.avatarId")
                    .value(userCreatorService.testAvatarId())
            )
    }

    @Test
    @Order(106)
    fun testGetTaskParticipantsByApproveStatusApproved() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/participants")
                .header("Authorization", "Bearer $creatorToken")
                .param("approved", "APPROVED")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.participants.length()").value(1))
            .andExpect(jsonPath("$.data.participants[0].member.id").value(participant3.userId))
            .andExpect(jsonPath("$.data.participants[0].member.intro").isString)
            .andExpect(jsonPath("$.data.participants[0].member.name").isString)
            .andExpect(
                jsonPath("$.data.participants[0].member.avatarId")
                    .value(userCreatorService.testAvatarId())
            )
    }

    @Test
    @Order(106)
    fun testGetTaskParticipantsByApproveStatusDisapproved() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/participants")
                .header("Authorization", "Bearer $creatorToken")
                .param("approved", "DISAPPROVED")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.participants.length()").value(1))
            .andExpect(jsonPath("$.data.participants[0].member.id").value(participant4.userId))
            .andExpect(jsonPath("$.data.participants[0].member.intro").isString)
            .andExpect(jsonPath("$.data.participants[0].member.name").isString)
            .andExpect(
                jsonPath("$.data.participants[0].member.avatarId")
                    .value(userCreatorService.testAvatarId())
            )
    }

    @Test
    @Order(107)
    fun testApproveTaskParticipantUserAccessDeniedError() {
        val request =
            MockMvcRequestBuilders.patch("/tasks/${taskIds[0]}/participants")
                .queryParam("member", participant.userId.toString())
                .header("Authorization", "Bearer $participantToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "approved": "APPROVED"
                }
            """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @Order(107)
    fun testTaskParticipantNotApprovedError() {
        val request =
            MockMvcRequestBuilders.patch(
                    "/tasks/${taskIds[0]}/participants/$participantTaskMembershipId"
                )
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "deadline": ${taskMembershipDeadline + 100000}
                }
            """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @Order(108)
    fun testApproveTestParticipantUser() {
        val request =
            MockMvcRequestBuilders.patch("/tasks/${taskIds[0]}/participants")
                .queryParam("member", participant.userId.toString())
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "approved": "APPROVED"
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                jsonPath("$.data.participants[?(@.member.id == ${participant.userId})].approved")
                    .value("APPROVED")
            )
    }

    @Test
    @Order(109)
    fun testPatchTestParticipantUserDeadline() {
        val request =
            MockMvcRequestBuilders.patch("/tasks/${taskIds[0]}/participants")
                .queryParam("member", participant.userId.toString())
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "deadline": ${taskMembershipDeadline + 100000}
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                jsonPath("$.data.participants[?(@.member.id == ${participant.userId})].approved")
                    .value("APPROVED")
            )
            .andExpect(
                jsonPath("$.data.participants[?(@.member.id == ${participant.userId})].deadline")
                    .value(taskMembershipDeadline + 100000)
            )
    }

    @Test
    @Order(110)
    fun testRemoveTestParticipantUser() {
        val request =
            MockMvcRequestBuilders.delete("/tasks/${taskIds[0]}/participants")
                .queryParam("member", participant.userId.toString())
                .header("Authorization", "Bearer $participantToken")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    @Test
    @Order(110)
    fun testRemoveTestParticipantUser3() {
        val request =
            MockMvcRequestBuilders.delete("/tasks/${taskIds[0]}/participants")
                .queryParam("member", participant3.userId.toString())
                .header("Authorization", "Bearer $participantToken3")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    @Test
    @Order(110)
    fun testRemoveTestParticipantUser4() {
        val request =
            MockMvcRequestBuilders.delete("/tasks/${taskIds[0]}/participants")
                .queryParam("member", participant4.userId.toString())
                .header("Authorization", "Bearer $participantToken4")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    @Test
    @Order(115)
    fun testRemoveTestParticipantTeam() {
        val request =
            MockMvcRequestBuilders.delete("/tasks/${taskIds[1]}/participants")
                .queryParam("member", teamId.toString())
                .header("Authorization", "Bearer $teamCreatorToken")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    @Test
    @Order(25)
    fun testGetTeamTaskWithOptionalQueriesUseTeamCreatorAfterExit() {
        val taskId = taskIds[1]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId")
                .queryParam("queryJoinability", "true")
                .queryParam("querySubmittability", "true")
                .queryParam("queryJoined", "true")
                .header("Authorization", "Bearer $teamCreatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.task.joinable").value(true))
            .andExpect(jsonPath("$.data.task.joinableAsTeam[0].id").value(teamId))
            .andExpect(jsonPath("$.data.task.submittable").value(false))
            .andExpect(jsonPath("$.data.task.submittableAsTeam").isEmpty)
            .andExpect(jsonPath("$.data.task.joined").value(false))
            .andExpect(jsonPath("$.data.task.joinedTeams").isEmpty())
    }

    @Test
    @Order(120)
    fun testGetTaskParticipantsAfterRemoveUser() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/participants")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.participants").isEmpty)
    }

    @Test
    @Order(125)
    fun testGetTaskParticipantsAfterRemoveTeam() {
        val taskId = taskIds[1]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/participants")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.participants").isEmpty)
    }

    @Test
    @Order(126)
    fun testAddTestParticipantUser2() {
        participantTaskMembershipId =
            addParticipantUser(participantToken, taskIds[0], participant.userId)
    }

    @Test
    @Order(127)
    fun testAddTestParticipantUser3() {
        participant2TaskMembershipId =
            addParticipantUser(participantToken2, taskIds[0], participant2.userId)
    }

    @Test
    @Order(127)
    fun testAddTestParticipantTeam2() {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskIds[1]}/participants")
                .header("Authorization", "Bearer $teamCreatorToken")
                .queryParam("member", teamId.toString())
                .contentType("application/json")
                .content(
                    """
                    {
                        "email": "test@example.com"
                    }
                """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(230)
    fun testDeleteTaskAndGetAccessDeniedError() {
        val taskId = taskIds[1]
        val request =
            MockMvcRequestBuilders.delete("/tasks/$taskId")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @Order(240)
    fun testDeleteTask() {
        val taskId = taskIds[1]
        val request =
            MockMvcRequestBuilders.delete("/tasks/$taskId")
                .header("Authorization", "Bearer $spaceCreatorToken")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(250)
    fun testDeleteTask2() {
        val taskId = taskIds[3]
        val request =
            MockMvcRequestBuilders.delete("/tasks/$taskId")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }
}
