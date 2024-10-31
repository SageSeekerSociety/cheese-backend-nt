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
class RankTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    private val userCreatorService: UserCreatorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    lateinit var participant: UserCreatorService.CreateUserResponse
    lateinit var participantToken: String
    private var spaceName = "Test Space (${floor(Math.random() * 10000000000).toLong()})"
    private var spaceIntro = "This is a test space."
    private var spaceDescription = "Description of space"
    private var spaceAvatarId = userCreatorService.testAvatarId()
    private var spaceId: IdType = -1
    private val taskName = "Test Task (${floor(Math.random() * 10000000000).toLong()})"
    private val taskIntro = "This is a test task."
    private val taskDescription = "Description of task"
    private val taskDeadline = LocalDateTime.now().plusDays(7).toEpochMilli()
    private val taskMembershipDeadline = LocalDateTime.now().plusMonths(1).toEpochMilli()
    private val taskSubmissionSchema =
        listOf(
            Pair("Text Entry", "TEXT"),
        )
    private var taskId: IdType = -1
    private var taskId2: IdType = -1
    private var taskId3: IdType = -1
    private var taskId4: IdType = -1
    private var submissionId: IdType = -1
    private var submissionId2: IdType = -1
    private var submissionId3: IdType = -1

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
                    "enableRank": false,
                    "announcements": "",
                    "taskTemplates": ""
                }
            """
                )
        val response = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
        val spaceId =
            JSONObject(response.andReturn().response.contentAsString)
                .getJSONObject("data")
                .getJSONObject("space")
                .getLong("id")
        logger.info("Created space: $spaceId")
        return spaceId
    }

    fun createTask(
        creatorToken: String,
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
        rank: Int?,
    ): IdType {
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
                  "space": ${space?: "null"},
                  "rank": ${rank?: "null"}
                }
            """
                )
        val response = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
        val json = JSONObject(response.andReturn().response.contentAsString)
        val taskId = json.getJSONObject("data").getJSONObject("task").getLong("id")
        logger.info("Created task: $taskId")
        return taskId
    }

    fun addParticipantUser(token: String, taskId: IdType, userId: IdType) {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskId}/participants")
                .header("Authorization", "Bearer $token")
                .queryParam("member", userId.toString())
                .contentType("application/json")
                .content("""
                {}
            """)
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    fun approveTaskParticipant(token: String, taskId: IdType, memberId: IdType) {
        val request =
            MockMvcRequestBuilders.patch("/tasks/${taskId}/participants")
                .queryParam("member", memberId.toString())
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
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.participant.approved").value("APPROVED")
            )
    }

    fun submitTaskUser(token: String, taskId: IdType, userId: IdType): IdType {
        val request =
            MockMvcRequestBuilders.post("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $token")
                .param("member", userId.toString())
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
        val response = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
        val json = JSONObject(response.andReturn().response.contentAsString)
        val submissionId = json.getJSONObject("data").getJSONObject("submission").getLong("id")
        logger.info("Created submission: $submissionId")
        return submissionId
    }

    @BeforeAll
    fun prepare() {
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
        participant = userCreatorService.createUser()
        participantToken = userCreatorService.login(participant.username, participant.password)
        spaceId = createSpace(creatorToken, spaceName, spaceIntro, spaceDescription, spaceAvatarId)
        taskId =
            createTask(
                creatorToken,
                taskName,
                "USER",
                taskDeadline,
                false,
                false,
                taskIntro,
                taskDescription,
                taskSubmissionSchema,
                null,
                spaceId,
                1
            )
        addParticipantUser(participantToken, taskId, participant.userId)
        approveTaskParticipant(creatorToken, taskId, participant.userId)
        submissionId = submitTaskUser(participantToken, taskId, participant.userId)
        taskId2 =
            createTask(
                creatorToken,
                taskName,
                "USER",
                taskDeadline,
                false,
                false,
                taskIntro,
                taskDescription,
                taskSubmissionSchema,
                null,
                spaceId,
                2
            )
        addParticipantUser(participantToken, taskId2, participant.userId)
        approveTaskParticipant(creatorToken, taskId2, participant.userId)
        submissionId2 = submitTaskUser(participantToken, taskId2, participant.userId)
        taskId3 =
            createTask(
                creatorToken,
                taskName,
                "USER",
                taskDeadline,
                false,
                false,
                taskIntro,
                taskDescription,
                taskSubmissionSchema,
                null,
                spaceId,
                1
            )
        addParticipantUser(participantToken, taskId3, participant.userId)
        approveTaskParticipant(creatorToken, taskId3, participant.userId)
        submissionId3 = submitTaskUser(participantToken, taskId3, participant.userId)
        taskId4 =
            createTask(
                creatorToken,
                taskName,
                "USER",
                taskDeadline,
                false,
                false,
                taskIntro,
                taskDescription,
                taskSubmissionSchema,
                null,
                spaceId,
                2
            )
    }

    @Test
    @Order(10)
    fun testGetSpaceWithMyRankWithoutRank() {
        val request =
            MockMvcRequestBuilders.get("/spaces/$spaceId")
                .header("Authorization", "Bearer $participantToken")
                .param("queryMyRank", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.myRank").isEmpty)
    }

    @Test
    @Order(20)
    fun testEnumerateSpacesWithMyRankWithoutRank() {
        val requestBuilders =
            MockMvcRequestBuilders.get("/spaces")
                .header("Authorization", "Bearer $participantToken")
                .param("page_start", spaceId.toString())
                .param("queryMyRank", "true")
        mockMvc
            .perform(requestBuilders)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[0].id").value(spaceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[0].myRank").isEmpty)
    }

    @Test
    @Order(30)
    fun testPatchSpaceWithFullRequest() {
        val request =
            MockMvcRequestBuilders.patch("/spaces/$spaceId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content("""{ "enableRank": true }""")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.enableRank").value(true))
    }

    @Test
    @Order(40)
    fun testGetSpaceWithMyRank() {
        val request =
            MockMvcRequestBuilders.get("/spaces/$spaceId")
                .header("Authorization", "Bearer $participantToken")
                .param("queryMyRank", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.myRank").value(0))
    }

    @Test
    @Order(50)
    fun testEnumerateSpacesWithMyRank() {
        val requestBuilders =
            MockMvcRequestBuilders.get("/spaces")
                .header("Authorization", "Bearer $participantToken")
                .param("page_start", spaceId.toString())
                .param("queryMyRank", "true")
        mockMvc
            .perform(requestBuilders)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[0].id").value(spaceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[0].myRank").value(0))
    }

    @Test
    @Order(55)
    fun testJoinRank2TaskAndGetYourRankIsNotHighEnoughError() {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskId4}/participants")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .contentType("application/json")
                .content("""
                    {}
                """)
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name").value("YourRankIsNotHighEnoughError")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.yourRank").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.requiredRank").value(1))
    }

    @Test
    @Order(60)
    fun testCreateReview() {
        val request =
            MockMvcRequestBuilders.post("/tasks/submissions/$submissionId/review")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                        {
                          "accepted": false,
                          "score": 0,
                          "comment": "Holly shit!"
                        }
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.hasUpgradedParticipantRank").value("false")
            )
    }

    @Test
    @Order(65)
    fun testGetSpaceWithMyRank2() {
        val request =
            MockMvcRequestBuilders.get("/spaces/$spaceId")
                .header("Authorization", "Bearer $participantToken")
                .param("queryMyRank", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.myRank").value(0))
    }

    @Test
    @Order(70)
    fun testUpdateReview() {
        val request =
            MockMvcRequestBuilders.patch("/tasks/submissions/$submissionId/review")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                        {
                          "accepted": true,
                          "score": 4,
                          "comment": "Could be better."
                        }
                    """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(75)
    fun testJoinRank2Task() {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskId4}/participants")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .contentType("application/json")
                .content("""
                    {}
                """)
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(80)
    fun testGetSpaceWithMyRank3() {
        val request =
            MockMvcRequestBuilders.get("/spaces/$spaceId")
                .header("Authorization", "Bearer $participantToken")
                .param("queryMyRank", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.myRank").value(1))
    }

    @Test
    @Order(90)
    fun testCreateReview2() {
        val request =
            MockMvcRequestBuilders.post("/tasks/submissions/$submissionId2/review")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                        {
                          "accepted": true,
                          "score": 5,
                          "comment": "That's amazing!"
                        }
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.hasUpgradedParticipantRank").value("true")
            )
    }

    @Test
    @Order(100)
    fun testGetSpaceWithMyRank4() {
        val request =
            MockMvcRequestBuilders.get("/spaces/$spaceId")
                .header("Authorization", "Bearer $participantToken")
                .param("queryMyRank", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.myRank").value(2))
    }

    @Test
    @Order(110)
    fun testCreateReview3() {
        val request =
            MockMvcRequestBuilders.post("/tasks/submissions/$submissionId3/review")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                        {
                          "accepted": true,
                          "score": 5,
                          "comment": "That's amazing!"
                        }
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.hasUpgradedParticipantRank").value("false")
            )
    }

    @Test
    @Order(120)
    fun testGetSpaceWithMyRank5() {
        val request =
            MockMvcRequestBuilders.get("/spaces/$spaceId")
                .header("Authorization", "Bearer $participantToken")
                .param("queryMyRank", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.myRank").value(2))
    }
}
