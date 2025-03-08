package org.rucca.cheese.client

import kotlin.math.floor
import org.json.JSONObject
import org.rucca.cheese.common.persistent.IdType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Service
class SpaceClient(private val mockMvc: MockMvc, private val userClient: UserClient) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun testSpaceName(): String {
        return "Test Space (${floor(Math.random() * 10000000000).toLong()})"
    }

    fun createSpace(
        creatorToken: String,
        spaceName: String = testSpaceName(),
        spaceIntro: String = "This is a test space.",
        spaceDescription: String = "Description of space",
        spaceAvatarId: IdType = userClient.testAvatarId(),
        spaceAnnouncements: String = "[]",
        spaceTaskTemplates: String = "[]",
        classificationTopics: List<IdType> = emptyList(),
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
                    "announcements": "$spaceAnnouncements",
                    "taskTemplates": "$spaceTaskTemplates",
                    "classificationTopics": [${classificationTopics.joinToString(",")}]
                }
            """
                )
        val response =
            mockMvc
                .perform(request)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.space.name").value(spaceName))
                .andExpect(jsonPath("$.data.space.intro").value(spaceIntro))
                .andExpect(jsonPath("$.data.space.description").value(spaceDescription))
                .andExpect(jsonPath("$.data.space.avatarId").value(spaceAvatarId))
                .andExpect(jsonPath("$.data.space.admins[0].role").value("OWNER"))
                .andExpect(jsonPath("$.data.space.enableRank").value(false))
                .andExpect(jsonPath("$.data.space.announcements").value(spaceAnnouncements))
                .andExpect(jsonPath("$.data.space.taskTemplates").value(spaceTaskTemplates))
                .andExpect(
                    jsonPath("$.data.space.classificationTopics.length()")
                        .value(classificationTopics.size)
                )
        for (topic in classificationTopics) response.andExpect(
            jsonPath("$.data.space.classificationTopics[?(@.id == $topic)].name").exists()
        )
        val spaceId =
            JSONObject(response.andReturn().response.contentAsString)
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
}
