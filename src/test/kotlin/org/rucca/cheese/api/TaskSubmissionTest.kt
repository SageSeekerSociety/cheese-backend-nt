/*
 *  Description: It tests the feature of task's submission.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      HuanCheng65
 *
 */

package org.rucca.cheese.api

import java.time.LocalDateTime
import kotlin.math.floor
import org.json.JSONObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.utils.AttachmentCreatorService
import org.rucca.cheese.utils.JsonArrayUtil
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class TaskSubmissionTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    private val userCreatorService: UserCreatorService,
    private val attachmentCreatorService: AttachmentCreatorService,
) {
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
    lateinit var irrelevantUser: UserCreatorService.CreateUserResponse
    lateinit var irrelevantUserToken: String
    private var teamId: IdType = -1
    private var spaceId: IdType = -1
    private var attachmentId: IdType = -1
    private val taskIds = mutableListOf<IdType>()
    private val taskName = "Test Task (${floor(Math.random() * 10000000000).toLong()})"
    private val taskIntro = "This is a test task."
    private val taskDescription = "A lengthy text. ".repeat(1000)
    private val taskDeadline = LocalDateTime.now().plusDays(7).toEpochMilli()
    private val taskSubmissionSchema =
        listOf(
            Pair("Text Entry", "TEXT"),
            Pair("Attachment Entry", "FILE"),
        )

    fun createSpace(
        creatorToken: String,
        spaceName: String,
        spaceIntro: String,
        spaceDescription: String,
        spaceAvatarId: IdType
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

    fun createTeam(
        creatorToken: String,
        teamName: String,
        teamIntro: String,
        teamDescription: String,
        teamAvatarId: IdType
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
        irrelevantUser = userCreatorService.createUser()
        irrelevantUserToken =
            userCreatorService.login(irrelevantUser.username, irrelevantUser.password)
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
        attachmentId = attachmentCreatorService.createAttachment(creatorToken)
    }

    fun createTask(
        name: String,
        submitterType: String,
        deadline: Long,
        resubmittable: Boolean,
        editable: Boolean,
        intro: String,
        description: String,
        submissionSchema: List<Pair<String, String>>,
        team: IdType?,
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
                  "resubmittable": $resubmittable,
                  "editable": $editable,
                  "intro": "$intro",
                  "description": "$description",
                  "submissionSchema": [
                    ${
                        submissionSchema
                            .map { """
                                {
                                  "prompt": "${it.first}",
                                  "type": "${it.second}"
                                }
                            """ }
                            .joinToString(",\n")
                    }
                  ],
                  "team": ${team?: "null"},
                  "space": ${space?: "null"}
                }
            """
                )
        val response =
            mockMvc
                .perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.name").value(name))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data.task.submitterType").value(submitterType)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data.task.creator.id").value(creator.userId)
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.deadline").value(deadline))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data.task.resubmittable").value(resubmittable)
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.editable").value(editable))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.intro").value(intro))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data.task.description").value(description)
                )
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

    fun approveTask(
        taskId: IdType,
        token: String,
    ) {
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

    fun approveTaskParticipant(token: String, taskId: IdType, memberId: IdType) {
        val request =
            MockMvcRequestBuilders.patch("/tasks/${taskId}/participants")
                .queryParam("member", memberId.toString())
                .header("Authorization", "Bearer ${token}")
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
                MockMvcResultMatchers.jsonPath("$.data.participant.approved").value("APPROVED")
            )
    }

    @Test
    @Order(10)
    fun testCreateTask() {
        createTask(
            name = "$taskName (1)",
            submitterType = "USER",
            deadline = taskDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            team = teamId,
            space = spaceId,
        )
        approveTask(taskIds[0], spaceCreatorToken)
        createTask(
            name = "$taskName (2)",
            submitterType = "TEAM",
            deadline = taskDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            team = teamId,
            space = spaceId,
        )
        approveTask(taskIds[1], spaceCreatorToken)
        createTask(
            name = "$taskName (3)",
            submitterType = "USER",
            deadline = taskDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            team = null,
            space = spaceId,
        )
        approveTask(taskIds[2], spaceCreatorToken)
        createTask(
            name = "$taskName (4)",
            submitterType = "USER",
            deadline = taskDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            team = teamId,
            space = null,
        )
        createTask(
            name = "$taskName (5)",
            submitterType = "USER",
            deadline = taskDeadline,
            resubmittable = true,
            editable = true,
            intro = taskIntro,
            description = taskDescription,
            submissionSchema = taskSubmissionSchema,
            team = null,
            space = null,
        )
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
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(100)
    fun testAddTestParticipantUser2() {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskIds[0]}/participants")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .contentType("application/json")
                .content("""
                    {}
                """)
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(105)
    fun testAddTestParticipantUser3() {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskIds[0]}/participants")
                .header("Authorization", "Bearer $participantToken2")
                .queryParam("member", participant2.userId.toString())
                .contentType("application/json")
                .content("""
                    {}
                """)
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(106)
    fun testGetTeamTaskWithJoinabilityAndSubmittability1() {
        val taskId = taskIds[1]
        val request =
            MockMvcRequestBuilders.get("/tasks/${taskIds[0]}")
                .queryParam("queryJoinability", "true")
                .queryParam("querySubmittability", "true")
                .header("Authorization", "Bearer $participantToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.joinable").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.submittable").value(false))
    }

    @Test
    @Order(110)
    fun testAddTestParticipantTeam2() {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskIds[1]}/participants")
                .header("Authorization", "Bearer $teamCreatorToken")
                .queryParam("member", teamId.toString())
                .contentType("application/json")
                .content("""
                    {}
                """)
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(111)
    fun testGetTeamTaskWithJoinabilityAndSubmittability2() {
        val taskId = taskIds[1]
        val request =
            MockMvcRequestBuilders.get("/tasks/${taskIds[1]}")
                .queryParam("queryJoinability", "true")
                .queryParam("querySubmittability", "true")
                .header("Authorization", "Bearer $teamCreatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.joinable").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.submittable").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.submittableAsTeam").isEmpty)
    }

    @Test
    @Order(115)
    fun testSubmitTaskUserPermissionDeniedError() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.post("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .param("member", participant.userId.toString())
                .contentType("application/json")
                .content(
                    """
                        [
                          {
                            "contentText": "This is a test submission."
                          }
                        ]
                    """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @Order(120)
    fun testApproveTaskParticipantUser2() {
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
                MockMvcResultMatchers.jsonPath(
                        "$.data.participants[?(@.member.id == ${participant.userId})].approved"
                    )
                    .value("APPROVED")
            )
    }

    @Test
    @Order(121)
    fun testGetTeamTaskWithJoinabilityAndSubmittability3() {
        val taskId = taskIds[1]
        val request =
            MockMvcRequestBuilders.get("/tasks/${taskIds[0]}")
                .queryParam("queryJoinability", "true")
                .queryParam("querySubmittability", "true")
                .header("Authorization", "Bearer $participantToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.joinable").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.submittable").value(true))
    }

    @Test
    @Order(125)
    fun testApproveTaskParticipantUser3() {
        val request =
            MockMvcRequestBuilders.patch("/tasks/${taskIds[0]}/participants")
                .queryParam("member", participant2.userId.toString())
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
                MockMvcResultMatchers.jsonPath(
                        "$.data.participants[?(@.member.id == ${participant2.userId})].approved"
                    )
                    .value("APPROVED")
            )
    }

    @Test
    @Order(128)
    fun testApproveTaskParticipantTeam2() {
        val request =
            MockMvcRequestBuilders.patch("/tasks/${taskIds[1]}/participants")
                .queryParam("member", teamId.toString())
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
                MockMvcResultMatchers.jsonPath(
                        "$.data.participants[?(@.member.id == $teamId)].approved"
                    )
                    .value("APPROVED")
            )
    }

    @Test
    @Order(129)
    fun testGetTeamTaskWithJoinabilityAndSubmittability4() {
        val request =
            MockMvcRequestBuilders.get("/tasks/${taskIds[1]}")
                .queryParam("queryJoinability", "true")
                .queryParam("querySubmittability", "true")
                .header("Authorization", "Bearer $teamCreatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.joinable").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.submittable").value(true))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.task.submittableAsTeam[0].id").value(teamId)
            )
    }

    @Test
    @Order(130)
    fun testSubmitTaskUser() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.post("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .param("member", participant.userId.toString())
                .contentType("application/json")
                .content(
                    """
                        [
                          {
                            "contentText": "This is a test submission."
                          }
                        ]
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission.id").exists())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.member.id")
                    .value(participant.userId)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.submitter.id")
                    .value(participant.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission.version").value(1))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[0].title")
                    .value("Text Entry")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[0].type").value("TEXT")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[0].contentText")
                    .value("This is a test submission.")
            )
    }

    @Test
    @Order(131)
    fun testSubmitTaskUser2() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.post("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken2")
                .param("member", participant2.userId.toString())
                .contentType("application/json")
                .content(
                    """
                        [
                          {
                            "contentText": "This is a test submission."
                          }
                        ]
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.member.id")
                    .value(participant2.userId)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.submitter.id")
                    .value(participant2.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission.version").value(1))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[0].title")
                    .value("Text Entry")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[0].type").value("TEXT")
            )
    }

    @Test
    @Order(140)
    fun testSubmitTaskTeam() {
        val taskId = taskIds[1]
        val request =
            MockMvcRequestBuilders.post("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $teamCreatorToken")
                .param("member", teamId.toString())
                .contentType("application/json")
                .content(
                    """
                        [
                          {
                            "contentText": "This is a test submission."
                          },
                          {
                            "contentAttachmentId": $attachmentId
                          }
                        ]
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission.id").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission.member.id").value(teamId))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.submitter.id")
                    .value(teamCreator.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission.version").value(1))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[0].title")
                    .value("Text Entry")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[0].type").value("TEXT")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[0].contentText")
                    .value("This is a test submission.")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[1].title")
                    .value("Attachment Entry")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[1].type").value("FILE")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[1].contentAttachment.id")
                    .value(attachmentId)
            )
    }

    @Test
    @Order(150)
    fun testSubmitAgainAndGetNotResubmittableError() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.post("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .param("member", participant.userId.toString())
                .contentType("application/json")
                .content(
                    """
                        [
                          {
                            "contentText": "This is a test submission."
                          }
                        ]
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name").value("TaskNotResubmittableError")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.taskId").value(taskId))
    }

    @Test
    @Order(160)
    fun updateToResubmittableAndGetPermissionDeniedError() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.patch("/tasks/$taskId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "resubmittable": true
                }
            """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @Order(161)
    fun updateToResubmittable() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.patch("/tasks/$taskId")
                .header("Authorization", "Bearer $spaceCreatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "resubmittable": true
                }
            """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(170)
    fun testResubmitTaskTeam() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.post("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .param("member", participant.userId.toString())
                .contentType("application/json")
                .content(
                    """
                        [
                          {
                            "contentText": "This is a test submission. (Version 2)"
                          }
                        ]
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission.id").exists())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.member.id")
                    .value(participant.userId)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.submitter.id")
                    .value(participant.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission.version").value(2))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[0].title")
                    .value("Text Entry")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[0].type").value("TEXT")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[0].contentText")
                    .value("This is a test submission. (Version 2)")
            )
    }

    @Test
    @Order(180)
    fun testUpdateSubmissionAndGetNotEditableError() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.patch("/tasks/$taskId/submissions/1")
                .header("Authorization", "Bearer $participantToken")
                .param("member", participant.userId.toString())
                .contentType("application/json")
                .content(
                    """
                      [
                        {
                          "contentText": "This is a test submission. (Version 1) (edited)"
                        }
                      ]
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name")
                    .value("TaskSubmissionNotEditableError")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.taskId").value(taskId))
    }

    @Test
    @Order(185)
    fun updateToEditable() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.patch("/tasks/$taskId")
                .header("Authorization", "Bearer $spaceCreatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "editable": true
                }
            """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(190)
    fun testUpdateSubmission() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.patch("/tasks/$taskId/submissions/2")
                .header("Authorization", "Bearer $participantToken")
                .param("member", participant.userId.toString())
                .contentType("application/json")
                .content(
                    """
                      [
                        {
                          "contentText": "This is a test submission. (Version 2) (edited)"
                        }
                      ]
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission.id").exists())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.member.id")
                    .value(participant.userId)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.submitter.id")
                    .value(participant.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission.version").value(2))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[0].title")
                    .value("Text Entry")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[0].type").value("TEXT")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.content[0].contentText")
                    .value("This is a test submission. (Version 2) (edited)")
            )
    }

    @Test
    @Order(198)
    fun testGetSubmissionsUseIrrelevantUserAndGetPermissionDeniedError() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $irrelevantUserToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError")
            )
    }

    @Test
    @Order(199)
    fun testGetSubmissionsUseParticipantAndGetPermissionDeniedError() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError")
            )
    }

    @Test
    @Order(200)
    fun testGetSubmissionsByDefault() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[0].id").exists())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].member.id")
                    .value(participant.userId)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].submitter.id")
                    .value(participant.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[0].version").value(2))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].content[0].title")
                    .value("Text Entry")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].content[0].type")
                    .value("TEXT")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].content[0].contentText")
                    .value("This is a test submission. (Version 2) (edited)")
            )
    }

    @Test
    @Order(210)
    fun testGetSubmissionsWithAllVersions() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .param("allVersions", "true")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[0].id").exists())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].member.id")
                    .value(participant.userId)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].submitter.id")
                    .value(participant.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[0].version").value(2))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].content[0].title")
                    .value("Text Entry")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].content[0].type")
                    .value("TEXT")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].content[0].contentText")
                    .value("This is a test submission. (Version 2) (edited)")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[1].version").value(1))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[1].content[0].contentText")
                    .value("This is a test submission.")
            )
    }

    @Test
    @Order(218)
    fun testGetSubmissionsUseParticipant2WithMemberIdAndGetPermissionDeniedError() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .param("member", participant.userId.toString())
                .header("Authorization", "Bearer $participantToken2")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError")
            )
    }

    @Test
    @Order(219)
    fun testGetSubmissionsUseParticipantWithMemberId() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .param("member", participant.userId.toString())
                .header("Authorization", "Bearer $participantToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[0].id").exists())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].member.id")
                    .value(participant.userId)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].submitter.id")
                    .value(participant.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[0].version").value(2))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].content[0].title")
                    .value("Text Entry")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].content[0].type")
                    .value("TEXT")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].content[0].contentText")
                    .value("This is a test submission. (Version 2) (edited)")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                        "$.data.submissions[?(@[0].memberId == ${participant2.userId})][0]"
                    )
                    .doesNotExist()
            )
    }

    @Test
    @Order(220)
    fun testGetSubmissionsWithMemberId() {
        val taskId = taskIds[0]
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .param("member", participant.userId.toString())
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[0].id").exists())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].member.id")
                    .value(participant.userId)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].submitter.id")
                    .value(participant.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[0].version").value(2))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].content[0].title")
                    .value("Text Entry")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].content[0].type")
                    .value("TEXT")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].content[0].contentText")
                    .value("This is a test submission. (Version 2) (edited)")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                        "$.data.submissions[?(@[0].memberId == ${participant2.userId})][0]"
                    )
                    .doesNotExist()
            )
    }

    @Test
    @Order(230)
    fun testDeleteTaskAndGetPermissionDeniedError() {
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
}
