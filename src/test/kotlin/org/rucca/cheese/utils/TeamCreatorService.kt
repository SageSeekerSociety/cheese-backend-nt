package org.rucca.cheese.utils

import kotlin.math.floor
import org.json.JSONObject
import org.rucca.cheese.common.persistent.IdType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

@Service
class TeamCreatorService(
    private val mockMvc: MockMvc,
    private val userCreatorService: UserCreatorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun testTeamName(): String {
        return "Test Team (${floor(Math.random() * 10000000000).toLong()})"
    }

    fun createTeam(
        creatorToken: String,
        teamName: String = testTeamName(),
        teamIntro: String = "This is a test team.",
        teamDescription: String = "A lengthy text. ".repeat(1000),
        teamAvatarId: IdType = userCreatorService.testAvatarId(),
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
        val teamId =
            JSONObject(mockMvc.perform(request).andReturn().response.contentAsString)
                .getJSONObject("data")
                .getJSONObject("team")
                .getLong("id")
        logger.info("Created team: $teamId")
        return teamId
    }
}
