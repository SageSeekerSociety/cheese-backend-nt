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
import org.rucca.cheese.auth.UserCreatorService
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.utils.AttachmentCreatorService
import org.rucca.cheese.utils.JsonArrayUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class TaskTest
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
    lateinit var spaceCreator: UserCreatorService.CreateUserResponse
    lateinit var spaceCreatorToken: String
    lateinit var participant: UserCreatorService.CreateUserResponse
    lateinit var participantToken: String
    private var teamId: IdType = -1
    private var spaceId: IdType = -1
    private var attachmentId: IdType = -1
    private val taskIds = mutableListOf<IdType>()
    private val taskName = "Test Task (${floor(Math.random() * 10000000000).toLong()})"
    private val taskDescription = "This is a test task."
    private val taskDeadline = LocalDateTime.now().plusDays(7).toEpochMilli()
    private val taskSubmissionSchema =
            listOf(
                    Pair("Text Entry", "TEXT"),
                    Pair("Attachment Entry", "FILE"),
            )

    fun createSpace(creatorToken: String, spaceName: String, spaceIntro: String, spaceAvatarId: IdType): IdType {
        val request =
                MockMvcRequestBuilders.post("/spaces")
                        .header("Authorization", "Bearer $creatorToken")
                        .contentType("application/json")
                        .content(
                                """
                {
                    "name": "$spaceName",
                    "intro": "$spaceIntro",
                    "avatarId": $spaceAvatarId
                }
            """)
        val spaceId =
                JSONObject(mockMvc.perform(request).andReturn().response.contentAsString)
                        .getJSONObject("data")
                        .getJSONObject("space")
                        .getLong("id")
        logger.info("Created space: $spaceId")
        return spaceId
    }

    fun createTeam(creatorToken: String, teamName: String, teamIntro: String, teamAvatarId: IdType): IdType {
        val request =
                MockMvcRequestBuilders.post("/teams")
                        .header("Authorization", "Bearer $creatorToken")
                        .contentType("application/json")
                        .content(
                                """
                {
                  "name": "$teamName",
                  "intro": "$teamIntro",
                  "avatarId": $teamAvatarId
                }
            """)
        teamId =
                JSONObject(mockMvc.perform(request).andReturn().response.contentAsString)
                        .getJSONObject("data")
                        .getJSONObject("team")
                        .getLong("id")
        logger.info("Created team: $teamId")
        return teamId
    }

    @BeforeAll
    fun prepare() {
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
        teamCreator = userCreatorService.createUser()
        teamCreatorToken = userCreatorService.login(teamCreator.username, teamCreator.password)
        spaceCreator = userCreatorService.createUser()
        spaceCreatorToken = userCreatorService.login(spaceCreator.username, spaceCreator.password)
        participant = userCreatorService.createUser()
        participantToken = userCreatorService.login(participant.username, participant.password)
        spaceId =
                createSpace(
                        creatorToken = teamCreatorToken,
                        spaceName = "Test Space (${floor(Math.random() * 10000000000).toLong()})",
                        spaceIntro = "This is a test space.",
                        spaceAvatarId = userCreatorService.testAvatarId(),
                )
        teamId =
                createTeam(
                        creatorToken = teamCreatorToken,
                        teamName = "Test Team (${floor(Math.random() * 10000000000).toLong()})",
                        teamIntro = "This is a test team.",
                        teamAvatarId = userCreatorService.testAvatarId(),
                )
        attachmentId = attachmentCreatorService.createAttachment(creatorToken)
    }

    fun createTask(
            name: String,
            submitterType: String,
            deadline: Long,
            resubmittable: Boolean,
            editable: Boolean,
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
            """)
        val response =
                mockMvc.perform(request)
                        .andExpect(MockMvcResultMatchers.status().isOk)
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.name").value(name))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.submitterType").value(submitterType))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.creator.id").value(creator.userId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.deadline").value(deadline))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.resubmittable").value(resubmittable))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.editable").value(editable))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.description").value(description))
        val json = JSONObject(response.andReturn().response.contentAsString)
        for (entry in submissionSchema) {
            val schema = json.getJSONObject("data").getJSONObject("task").getJSONArray("submissionSchema")
            val found = JsonArrayUtil.toArray(schema).find { it.getString("prompt") == entry.first }
            assert(found != null)
            assert(found!!.getString("type") == entry.second)
        }
        val taskId = json.getJSONObject("data").getJSONObject("task").getLong("id")
        taskIds.add(taskId)
        logger.info("Created task: $taskId")
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
                description = taskDescription,
                submissionSchema = taskSubmissionSchema,
                team = teamId,
                space = spaceId,
        )
        createTask(
                name = "$taskName (2)",
                submitterType = "TEAM",
                deadline = taskDeadline,
                resubmittable = true,
                editable = true,
                description = taskDescription,
                submissionSchema = taskSubmissionSchema,
                team = teamId,
                space = spaceId,
        )
        createTask(
                name = "$taskName (3)",
                submitterType = "USER",
                deadline = taskDeadline,
                resubmittable = true,
                editable = true,
                description = taskDescription,
                submissionSchema = taskSubmissionSchema,
                team = null,
                space = spaceId,
        )
        createTask(
                name = "$taskName (4)",
                submitterType = "USER",
                deadline = taskDeadline,
                resubmittable = true,
                editable = true,
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
                description = taskDescription,
                submissionSchema = taskSubmissionSchema,
                team = null,
                space = null,
        )
    }

    @Test
    @Order(20)
    fun testGetTask() {
        val taskId = taskIds[0]
        val request = MockMvcRequestBuilders.get("/tasks/$taskId").header("Authorization", "Bearer $creatorToken")
        val response =
                mockMvc.perform(request)
                        .andExpect(MockMvcResultMatchers.status().isOk)
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.name").value("$taskName (1)"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.submitterType").value("USER"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.creator.id").value(creator.userId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.deadline").value(taskDeadline))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.resubmittable").value(true))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.editable").value(true))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.description").value(taskDescription))
        val json = JSONObject(response.andReturn().response.contentAsString)
        for (entry in taskSubmissionSchema) {
            val schema = json.getJSONObject("data").getJSONObject("task").getJSONArray("submissionSchema")
            val found = JsonArrayUtil.toArray(schema).find { it.getString("prompt") == entry.first }
            assert(found != null)
            assert(found!!.getString("type") == entry.second)
        }
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
                  "resubmittable": false,
                  "editable": false,
                  "description": "This is an updated test task.",
                  "submissionSchema": [
                    {
                      "prompt": "Text Entry",
                      "type": "TEXT"
                    }
                  ]
                }
            """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.name").value("$taskName (1) (updated)"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.creator.id").value(creator.userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.deadline").value(taskDeadline + 1000000000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.resubmittable").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.editable").value(false))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.task.description")
                                .value("This is an updated test task."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.submissionSchema[0].prompt").value("Text Entry"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.submissionSchema[0].type").value("TEXT"))
    }

    @Test
    @Order(50)
    fun testEnumerateTasksByDefault() {
        val request = MockMvcRequestBuilders.get("/tasks").header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tasks[0].id").value(taskIds[0]))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tasks[0].name").value("$taskName (1) (updated)"))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.tasks[0].description")
                                .value("This is an updated test task."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tasks[0].deadline").value(taskDeadline + 1000000000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tasks[0].submitters.total").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tasks[0].submitters.examples").isArray)
    }

    @Test
    @Order(55)
    fun testEnumerateTasksByTeam() {
        val request =
                MockMvcRequestBuilders.get("/tasks")
                        .header("Authorization", "Bearer $creatorToken")
                        .param("team", teamId.toString())
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tasks[0].name").value("$taskName (1) (updated)"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tasks[1].name").value("$taskName (4)"))
    }

    @Test
    @Order(60)
    fun testEnumerateTasksBySpace() {
        val request =
                MockMvcRequestBuilders.get("/tasks")
                        .header("Authorization", "Bearer $creatorToken")
                        .param("space", spaceId.toString())
                        .param("page_size", "2")
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tasks[0].id").value(taskIds[0]))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tasks[0].name").value("$taskName (1) (updated)"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tasks[1].name").value("$taskName (3)"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_start").value(taskIds[0]))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_size").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_prev").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.prev_start").value(null))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_more").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.next_start").value(taskIds[1]))
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
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tasks[0].name").value("$taskName (3)"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tasks[1].name").value("$taskName (2)"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_start").value(taskIds[2]))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_size").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_prev").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.prev_start").value(taskIds[0]))
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
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tasks[0].id").value(taskIds[0]))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tasks[0].name").value("$taskName (1) (updated)"))
    }

    @Test
    @Order(80)
    fun testEnumerateTasksByDeadlineDesc() {
        val request =
                MockMvcRequestBuilders.get("/tasks")
                        .header("Authorization", "Bearer $creatorToken")
                        .param("sort_by", "deadline")
                        .param("sort_order", "desc")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(90)
    fun testAddTestParticipantUser() {
        val request =
                MockMvcRequestBuilders.post("/tasks/${taskIds[0]}/participants")
                        .header("Authorization", "Bearer $participantToken")
                        .queryParam("member", participant.userId.toString())
                        .contentType("application/json")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(95)
    fun testAddTestParticipantTeam() {
        val request =
                MockMvcRequestBuilders.post("/tasks/${taskIds[1]}/participants")
                        .header("Authorization", "Bearer $teamCreatorToken")
                        .queryParam("member", teamId.toString())
                        .contentType("application/json")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(100)
    fun testGetTaskParticipantsUser() {
        val taskId = taskIds[0]
        val request =
                MockMvcRequestBuilders.get("/tasks/$taskId/participants")
                        .header("Authorization", "Bearer $participantToken")
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.participants[0].id").value(participant.userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.participants[0].intro").isString)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.participants[0].name").isString)
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.participants[0].avatarId")
                                .value(userCreatorService.testAvatarId()))
    }

    @Test
    @Order(105)
    fun testGetTaskParticipantsTeam() {
        val taskId = taskIds[1]
        val request =
                MockMvcRequestBuilders.get("/tasks/$taskId/participants")
                        .header("Authorization", "Bearer $teamCreatorToken")
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.participants[0].id").value(teamId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.participants[0].intro").isString)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.participants[0].name").isString)
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.participants[0].avatarId")
                                .value(userCreatorService.testAvatarId()))
    }

    @Test
    @Order(110)
    fun testRemoveTestParticipantUser() {
        val request =
                MockMvcRequestBuilders.delete("/tasks/${taskIds[0]}/participants")
                        .queryParam("member", participant.userId.toString())
                        .header("Authorization", "Bearer $participantToken")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(115)
    fun testRemoveTestParticipantTeam() {
        val request =
                MockMvcRequestBuilders.delete("/tasks/${taskIds[1]}/participants")
                        .queryParam("member", teamId.toString())
                        .header("Authorization", "Bearer $teamCreatorToken")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(120)
    fun testGetTaskParticipantsAfterRemoveUser() {
        val taskId = taskIds[0]
        val request =
                MockMvcRequestBuilders.get("/tasks/$taskId/participants")
                        .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.participants").isEmpty)
    }

    @Test
    @Order(125)
    fun testGetTaskParticipantsAfterRemoveTeam() {
        val taskId = taskIds[1]
        val request =
                MockMvcRequestBuilders.get("/tasks/$taskId/participants")
                        .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.participants").isEmpty)
    }

    @Test
    @Order(126)
    fun testAddTestParticipantUser2() {
        val request =
                MockMvcRequestBuilders.post("/tasks/${taskIds[0]}/participants")
                        .header("Authorization", "Bearer $participantToken")
                        .queryParam("member", participant.userId.toString())
                        .contentType("application/json")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(127)
    fun testAddTestParticipantTeam2() {
        val request =
                MockMvcRequestBuilders.post("/tasks/${taskIds[1]}/participants")
                        .header("Authorization", "Bearer $teamCreatorToken")
                        .queryParam("member", teamId.toString())
                        .contentType("application/json")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
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
                    """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[0].memberId").value(participant.userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[0].version").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[0].index").value(0))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.submission[0].contentText")
                                .value("This is a test submission."))
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
                    """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[0].memberId").value(teamId.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[0].version").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[0].index").value(0))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.submission[0].contentText")
                                .value("This is a test submission."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[1].memberId").value(teamId.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[1].version").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[1].index").value(1))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.submission[1].contentAttachment.id").value(attachmentId))
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
                    """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.name").value("TaskNotResubmittableError"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.taskId").value(taskId))
    }

    @Test
    @Order(160)
    fun updateToResubmittable() {
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
            """)
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
                    """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[0].memberId").value(participant.userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[0].version").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[0].index").value(0))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.submission[0].contentText")
                                .value("This is a test submission. (Version 2)"))
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
                    """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.name").value("TaskSubmissionNotEditableError"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.taskId").value(taskId))
    }

    @Test
    @Order(185)
    fun updateToEditable() {
        val taskId = taskIds[0]
        val request =
                MockMvcRequestBuilders.patch("/tasks/$taskId")
                        .header("Authorization", "Bearer $creatorToken")
                        .contentType("application/json")
                        .content(
                                """
                {
                  "editable": true
                }
            """)
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(190)
    fun testUpdateSubmission() {
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
                    """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[0].memberId").value(participant.userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[0].version").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submission[0].index").value(0))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.submission[0].contentText")
                                .value("This is a test submission. (Version 1) (edited)"))
    }

    @Test
    @Order(200)
    fun testGetSubmissionsByDefault() {
        val taskId = taskIds[0]
        val request =
                MockMvcRequestBuilders.get("/tasks/$taskId/submissions").header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.submissions[0][0].memberId").value(participant.userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[0][0].version").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[0][0].index").value(0))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.submissions[0][0].contentText")
                                .value("This is a test submission. (Version 2)"))
    }

    @Test
    @Order(210)
    fun testGetSubmissionsWithAllVersions() {
        val taskId = taskIds[0]
        val request =
                MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                        .param("allVersions", "true")
                        .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.submissions[0][0].memberId").value(participant.userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[0][0].version").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[0][0].index").value(0))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.submissions[0][0].contentText")
                                .value("This is a test submission. (Version 1) (edited)"))
    }

    @Test
    @Order(220)
    fun testGetSubmissionsWithMemberId() {
        val taskId = taskIds[0]
        val request =
                MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                        .param("member", participant.userId.toString())
                        .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.submissions[0][0].memberId").value(participant.userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[0][0].version").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions[0][0].index").value(0))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.data.submissions[0][0].contentText")
                                .value("This is a test submission. (Version 2)"))
    }

    @Test
    @Order(230)
    fun testDeleteTask() {
        val taskId = taskIds[1]
        val request = MockMvcRequestBuilders.delete("/tasks/$taskId").header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }
}
