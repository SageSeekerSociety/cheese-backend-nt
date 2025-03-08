package org.rucca.cheese.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.json.JSONObject
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.CreateProjectRequestDTO
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Service
class ProjectClient(private val mockMvc: MockMvc, private val userClient: UserClient) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun createProject(
        token: String,
        name: String = "Test Project",
        description: String = "Test Description",
        colorCode: String = "#FFFFFF",
        startDate: Long = System.currentTimeMillis(),
        endDate: Long = System.currentTimeMillis() + 86400000,
        teamId: IdType,
        leaderId: IdType,
        content: String = "Test Content",
        parentId: IdType? = null,
        externalTaskId: IdType? = null,
        githubRepo: String? = null,
    ): IdType {
        val request =
            CreateProjectRequestDTO(
                name = name,
                description = description,
                colorCode = colorCode,
                startDate = startDate,
                endDate = endDate,
                teamId = teamId,
                leaderId = leaderId,
                content = content,
                parentId = parentId,
                externalTaskId = externalTaskId,
                githubRepo = githubRepo,
            )
        val result =
            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/projects")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ObjectMapper().writeValueAsString(request))
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.project.name").value(name))
                .andReturn()

        val response = result.response.contentAsString
        val jsonResponse = JSONObject(response)
        return jsonResponse.getJSONObject("data").getJSONObject("project").getLong("id")
    }
}
