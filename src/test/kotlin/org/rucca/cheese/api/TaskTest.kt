package org.rucca.cheese.api

import io.mockk.InternalPlatformDsl.toArray
import kotlin.math.floor
import org.json.JSONObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.rucca.cheese.auth.UserCreatorService
import org.rucca.cheese.common.persistent.IdType
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
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    lateinit var teamCreator: UserCreatorService.CreateUserResponse
    lateinit var teamCreatorToken: String
    lateinit var spaceCreator: UserCreatorService.CreateUserResponse
    lateinit var spaceCreatorToken: String
    private var teamId: IdType = -1
    private var spaceId: IdType = -1
    private val taskIds = mutableListOf<IdType>()
    private val taskName = "Test Task (${floor(Math.random() * 10000000000).toLong()})"
    private val taskDescription = "This is a test task."
    private val taskDeadline = "2024-09-25"
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
    }

    fun createTask(
            name: String,
            submitterType: String,
            deadline: String,
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
                  "deadline": "2024-09-26",
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
        val response =
                mockMvc.perform(request)
                        .andExpect(MockMvcResultMatchers.status().isOk)
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.name").value("$taskName (1) (updated)"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.creator.id").value(creator.userId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.deadline").value("2024-09-26"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.resubmittable").value(false))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.editable").value(false))
                        .andExpect(
                                MockMvcResultMatchers.jsonPath("$.data.task.description")
                                        .value("This is an updated test task."))
                        .andExpect(
                                MockMvcResultMatchers.jsonPath("$.data.task.submissionSchema[0].prompt")
                                        .value("Text Entry"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.task.submissionSchema[0].type").value("TEXT"))
    }
}
