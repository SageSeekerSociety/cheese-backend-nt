/*
 *  Description: It tests the feature of a user's rank in a space.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      nameisyui
 *
 */

package org.rucca.cheese.api

import org.json.JSONObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.rucca.cheese.client.SpaceClient
import org.rucca.cheese.client.TaskClient
import org.rucca.cheese.client.UserClient
import org.rucca.cheese.common.persistent.IdType
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
class RankTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    private val userClient: UserClient,
    private val spaceClient: SpaceClient,
    private val taskClient: TaskClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    lateinit var creator: UserClient.CreateUserResponse
    lateinit var creatorToken: String
    lateinit var participant: UserClient.CreateUserResponse
    lateinit var participantToken: String
    private var spaceId: IdType = -1
    private var taskId: IdType = -1
    private var taskId2: IdType = -1
    private var taskId3: IdType = -1
    private var taskId4: IdType = -1
    private var submissionId: IdType = -1
    private var submissionId2: IdType = -1
    private var submissionId3: IdType = -1
    private val submission = """
                        [
                          {
                            "contentText": "This is a test submission."
                          }
                        ]
                    """

    @BeforeAll
    fun prepare() {
        creator = userClient.createUser()
        creatorToken = userClient.login(creator.username, creator.password)
        participant = userClient.createUser()
        participantToken = userClient.login(participant.username, participant.password)
        spaceId = spaceClient.createSpace(creatorToken)
        taskId =
            taskClient.createTask(
                creatorToken,
                submitterType = "USER",
                resubmittable = false,
                editable = false,
                team = null,
                space = spaceId,
                rank = 1,
            )
        taskClient.approveTask(taskId, creatorToken)
        taskClient.addParticipantUser(participantToken, taskId, participant.userId)
        taskClient.approveTaskParticipant(creatorToken, taskId, participant.userId)
        submissionId = taskClient.submitTaskUser(participantToken, taskId, participant.userId,submission)
        taskId2 =
            taskClient.createTask(
                creatorToken,
                submitterType = "USER",
                resubmittable = false,
                editable = false,
                team = null,
                space = spaceId,
                rank = 2,
            )
        taskClient.approveTask(taskId2, creatorToken)
        taskClient.addParticipantUser(participantToken, taskId2, participant.userId)
        taskClient.approveTaskParticipant(creatorToken, taskId2, participant.userId)
        submissionId2 = taskClient.submitTaskUser(participantToken, taskId2, participant.userId,submission)
        taskId3 =
            taskClient.createTask(
                creatorToken,
                submitterType = "USER",
                resubmittable = false,
                editable = false,
                team = null,
                space = spaceId,
                rank = 1,
            )
        taskClient.approveTask(taskId3, creatorToken)
        taskClient.addParticipantUser(participantToken, taskId3, participant.userId)
        taskClient.approveTaskParticipant(creatorToken, taskId3, participant.userId)
        submissionId3 = taskClient.submitTaskUser(participantToken, taskId3, participant.userId,submission)
        taskId4 =
            taskClient.createTask(
                creatorToken,
                submitterType = "USER",
                resubmittable = false,
                editable = false,
                team = null,
                space = spaceId,
                rank = 2,
            )
        taskClient.approveTask(taskId4, creatorToken)
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
            .andExpect(jsonPath("$.data.space.id").value(spaceId))
            .andExpect(jsonPath("$.data.space.myRank").isEmpty)
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
            .andExpect(jsonPath("$.data.spaces[0].id").value(spaceId))
            .andExpect(jsonPath("$.data.spaces[0].myRank").isEmpty)
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
            .andExpect(jsonPath("$.data.space.enableRank").value(true))
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
            .andExpect(jsonPath("$.data.space.id").value(spaceId))
            .andExpect(jsonPath("$.data.space.myRank").value(0))
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
            .andExpect(jsonPath("$.data.spaces[0].id").value(spaceId))
            .andExpect(jsonPath("$.data.spaces[0].myRank").value(0))
    }

    @Test
    @Order(55)
    fun testJoinRank2TaskAndGetYourRankIsNotHighEnoughError() {
        val request =
            MockMvcRequestBuilders.post("/tasks/${taskId4}/participants")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .contentType("application/json")
                .content(
                    """
                    {}
                """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(jsonPath("$.error.name").value("YourRankIsNotHighEnoughError"))
            .andExpect(jsonPath("$.error.data.yourRank").value(0))
            .andExpect(jsonPath("$.error.data.requiredRank").value(1))
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
            .andExpect(jsonPath("$.data.hasUpgradedParticipantRank").value("false"))
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
            .andExpect(jsonPath("$.data.space.id").value(spaceId))
            .andExpect(jsonPath("$.data.space.myRank").value(0))
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
                .content(
                    """
                    {}
                """
                )
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
            .andExpect(jsonPath("$.data.space.id").value(spaceId))
            .andExpect(jsonPath("$.data.space.myRank").value(1))
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
            .andExpect(jsonPath("$.data.hasUpgradedParticipantRank").value("true"))
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
            .andExpect(jsonPath("$.data.space.id").value(spaceId))
            .andExpect(jsonPath("$.data.space.myRank").value(2))
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
            .andExpect(jsonPath("$.data.hasUpgradedParticipantRank").value("false"))
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
            .andExpect(jsonPath("$.data.space.id").value(spaceId))
            .andExpect(jsonPath("$.data.space.myRank").value(2))
    }
}
