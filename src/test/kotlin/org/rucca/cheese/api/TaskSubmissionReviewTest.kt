/*
 *  Description: It tests the feature of reviewing a task's submission.
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
import org.rucca.cheese.client.AttachmentClient
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

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class TaskSubmissionReviewTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    private val userClient: UserClient,
    private val attachmentClient: AttachmentClient,
    private val taskClient: TaskClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    lateinit var creator: UserClient.CreateUserResponse
    lateinit var creatorToken: String
    lateinit var participant: UserClient.CreateUserResponse
    lateinit var participantToken: String
    private var attachmentId: IdType = -1
    private var taskId: IdType = -1
    private var submissionId: IdType = -1

    fun joinTask(taskId: IdType, participantId: IdType, participantToken: String) {
        val request =
            MockMvcRequestBuilders.post("/tasks/$taskId/participants")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participantId.toString())
                .contentType("application/json")
                .content(
                    """
                    {}
                """
                )
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
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
                MockMvcResultMatchers.jsonPath(
                        "$.data.participants[?(@.member.id == $memberId)].approved"
                    )
                    .value("APPROVED")
            )
    }

    fun submitTask(taskId: IdType, participantId: IdType, participantToken: String): IdType {
        val request =
            MockMvcRequestBuilders.post("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .param("member", participantId.toString())
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
        val response = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
        val json = JSONObject(response.andReturn().response.contentAsString)
        val submissionId = json.getJSONObject("data").getJSONObject("submission").getLong("id")
        logger.info("Submitted task with submission: $submissionId")
        return submissionId
    }

    @BeforeAll
    fun prepare() {
        creator = userClient.createUser()
        creatorToken = userClient.login(creator.username, creator.password)
        participant = userClient.createUser()
        participantToken = userClient.login(participant.username, participant.password)
        attachmentId = attachmentClient.createAttachment(creatorToken)
        taskId =
            taskClient.createTask(
                creatorToken,
                submissionSchema =
                    listOf(Pair("Text Entry", "TEXT"), Pair("Attachment Entry", "FILE")),
            )
        joinTask(taskId, participant.userId, participantToken)
        approveTaskParticipant(creatorToken, taskId, participant.userId)
        submissionId = submitTask(taskId, participant.userId, participantToken)
    }

    @Test
    @Order(5)
    fun testGetSubmissionWithQueryReviewEnabledWhenNotReviewed() {
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .queryParam("queryReview", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.reviewed").value(false)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail").doesNotExist()
            )
    }

    @Test
    @Order(6)
    fun testGetReviewedSubmissionWithQueryReviewEnabledWhenNotReviewed() {
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .queryParam("queryReview", "true")
                .queryParam("reviewed", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions").isEmpty)
    }

    @Test
    @Order(7)
    fun testGetUnreviewedSubmissionWithQueryReviewEnabledWhenNotReviewed() {
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .queryParam("queryReview", "true")
                .queryParam("reviewed", "false")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.reviewed").value(false)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail").doesNotExist()
            )
    }

    @Test
    @Order(10)
    fun testCreateReview() {
        val request =
            MockMvcRequestBuilders.post("/tasks/submissions/$submissionId/review")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                        {
                          "accepted": true,
                          "score": 5,
                          "comment": "Good job!"
                        }
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.reviewed").value(true)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.detail.accepted")
                    .value(true)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.detail.score").value(5)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.detail.comment")
                    .value("Good job!")
            )
    }

    @Test
    @Order(20)
    fun testCreateReviewAndGetPermissionDeniedError() {
        val request =
            MockMvcRequestBuilders.post("/tasks/submissions/$submissionId/review")
                .header("Authorization", "Bearer $participantToken")
                .contentType("application/json")
                .content(
                    """
                        {
                          "accepted": true,
                          "score": 5,
                          "comment": "Good job!"
                        }
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError")
            )
    }

    @Test
    @Order(30)
    fun testCreateReviewAndGetTaskSubmissionAlreadyReviewedError() {
        val request =
            MockMvcRequestBuilders.post("/tasks/submissions/$submissionId/review")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                        {
                          "accepted": true,
                          "score": 5,
                          "comment": "Good job!"
                        }
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isConflict)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name")
                    .value("TaskSubmissionAlreadyReviewedError")
            )
    }

    @Test
    @Order(40)
    fun testUpdateReviewEmpty() {
        val request =
            MockMvcRequestBuilders.patch("/tasks/submissions/$submissionId/review")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content("{}")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.reviewed").value(true)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.detail.accepted")
                    .value(true)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.detail.score").value(5)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.detail.comment")
                    .value("Good job!")
            )
    }

    @Test
    @Order(50)
    fun testUpdateReview() {
        val request =
            MockMvcRequestBuilders.patch("/tasks/submissions/$submissionId/review")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                        {
                          "accepted": false,
                            "score": 4,
                            "comment": "Could be better."
                        }
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.detail.accepted")
                    .value(false)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.detail.score").value(4)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.detail.comment")
                    .value("Could be better.")
            )
    }

    @Test
    @Order(60)
    fun testUpdateReviewAndGetPermissionDeniedError() {
        val request =
            MockMvcRequestBuilders.patch("/tasks/submissions/$submissionId/review")
                .header("Authorization", "Bearer $participantToken")
                .contentType("application/json")
                .content(
                    """
                        {
                          "accepted": false,
                            "score": 4,
                            "comment": "Could be better."
                        }
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError")
            )
    }

    @Test
    @Order(70)
    fun testGetSubmissionWithQueryReviewEnabled() {
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .queryParam("queryReview", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.reviewed").value(true)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail.accepted")
                    .value(false)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail.score").value(4)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail.comment")
                    .value("Could be better.")
            )
    }

    @Test
    @Order(71)
    fun testGetReviewedSubmissionWithQueryReviewEnabled() {
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .queryParam("queryReview", "true")
                .queryParam("reviewed", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.reviewed").value(true)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail.accepted")
                    .value(false)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail.score").value(4)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail.comment")
                    .value("Could be better.")
            )
    }

    @Test
    @Order(72)
    fun testGetUnreviewedSubmissionWithQueryReviewEnabled() {
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .queryParam("queryReview", "true")
                .queryParam("reviewed", "false")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions").isEmpty)
    }

    @Test
    @Order(75)
    fun testDeleteReviewAndGetPermissionDeniedError() {
        val request =
            MockMvcRequestBuilders.delete("/tasks/submissions/$submissionId/review")
                .header("Authorization", "Bearer $participantToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError")
            )
    }

    @Test
    @Order(80)
    fun testDeleteReview() {
        val request =
            MockMvcRequestBuilders.delete("/tasks/submissions/$submissionId/review")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(90)
    fun testGetSubmissionWithQueryReviewEnabledAfterReviewDeleted() {
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .queryParam("queryReview", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.reviewed").value(false)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail").doesNotExist()
            )
    }

    @Test
    @Order(100)
    fun testDeleteAgainAndGetTaskSubmissionNotReviewedYetError() {
        val request =
            MockMvcRequestBuilders.delete("/tasks/submissions/$submissionId/review")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name")
                    .value("TaskSubmissionNotReviewedYetError")
            )
    }

    @Test
    @Order(110)
    fun testUpdateAndGetTaskSubmissionNotReviewedYetError() {
        val request =
            MockMvcRequestBuilders.patch("/tasks/submissions/$submissionId/review")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                        {
                          "accepted": false,
                            "score": 4,
                            "comment": "Could be better."
                        }
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name")
                    .value("TaskSubmissionNotReviewedYetError")
            )
    }

    @Test
    @Order(130)
    fun testCreateReviewAgain() {
        val request =
            MockMvcRequestBuilders.post("/tasks/submissions/$submissionId/review")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                        {
                          "accepted": true,
                          "score": 5,
                          "comment": "Good job!"
                        }
                    """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.reviewed").value(true)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.detail.accepted")
                    .value(true)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.detail.score").value(5)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submission.review.detail.comment")
                    .value("Good job!")
            )
    }

    @Test
    @Order(140)
    fun testGetSubmissionOnceAgain() {
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .queryParam("queryReview", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.reviewed").value(true)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail.accepted")
                    .value(true)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail.score").value(5)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail.comment")
                    .value("Good job!")
            )
    }

    @Test
    @Order(141)
    fun testGetReviewedSubmissionOnceAgain() {
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .queryParam("queryReview", "true")
                .queryParam("reviewed", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.reviewed").value(true)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail.accepted")
                    .value(true)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail.score").value(5)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.submissions[0].review.detail.comment")
                    .value("Good job!")
            )
    }

    @Test
    @Order(142)
    fun testGetUnreviewedSubmissionOnceAgain() {
        val request =
            MockMvcRequestBuilders.get("/tasks/$taskId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .queryParam("member", participant.userId.toString())
                .queryParam("queryReview", "true")
                .queryParam("reviewed", "false")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.submissions").isEmpty)
    }
}
