/*
 *  Description: It tests the feature of project.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.api

import kotlin.math.floor
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class ProjectTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    private val userCreatorService: UserCreatorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    lateinit var admin: UserCreatorService.CreateUserResponse
    lateinit var adminToken: String
    lateinit var newOwner: UserCreatorService.CreateUserResponse
    lateinit var newOwnerToken: String
    lateinit var member: UserCreatorService.CreateUserResponse
    lateinit var memberToken: String

//    private var projectLeadderId = 1001
//    private var projectAvatarId = userCreatorService.testAvatarId()
//    private var projectId: IdType = -1
    private var projectName = "前端开发"    // "Test Project (${floor(Math.random() * 10000000000).toLong()})"
    private var projectDescription = "RAG助教系统前端开发"
    private var projectColorCode = "#28a745"
    private var projectStartDate = 1706745600000    // System.currentTimeMillis()
    private var projectEndDate = 1709251200000      // System.currentTimeMillis() + 86400000 // 1 day later
    private var projectParentId = 2001
    private var projectLeaderId = 1001
    private var projectExternalTaskId = 123
    private var projectGithubRepo = "org/repo"
    private var projectContentRaw = "# 项目说明\\n\\n这是一个前端开发项目..."
/*    private var projectContentAttachments = listOf(
        mapOf(
            "id" to 4001,
            "type" to "file",
            "url" to "https://...",
            "meta" to mapOf(
                "name" to "架构设计.pdf",
                "size" to 1024576,
                "mime" to "application/pdf",
                "hash" to "..."
            )
        )
    )*/
    private var projectExternalCollaborators = listOf(
        mapOf("userId" to 1002)
    )
    private var projectId: IdType = -1

    @BeforeAll
    fun prepare() {
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
        admin = userCreatorService.createUser()
        adminToken = userCreatorService.login(admin.username, admin.password)
        newOwner = userCreatorService.createUser()
        newOwnerToken = userCreatorService.login(newOwner.username, newOwner.password)
        member = userCreatorService.createUser()
        memberToken = userCreatorService.login(member.username, member.password)
    }

    @Test
    @Order(10)
    fun testCreateProject() {
        val requestBody = """
            {
                "name": "$projectName",
                "description": "$projectDescription",
                "colorCode": "$projectColorCode",
                "startDate": $projectStartDate,
                "endDate": $projectEndDate,
                "parentId": $projectParentId,
                "leaderId": $projectLeaderId,
                "externalTaskId": $projectExternalTaskId,
                "githubRepo": "$projectGithubRepo",
                "content": {
                    "raw": "# 项目说明\n\n这是一个前端开发项目...",
                    "attachments": [{
                        "id": 4001,
                        "type": "file",
                        "url": "https://...",
                        "meta": {
                            "name": "架构设计.pdf",
                            "size": 1024576,
                            "mime": "application/pdf",
                            "hash": "..."
                        }
                    }]
                },
                "externalCollaborators": ${JSONArray(projectExternalCollaborators)}    
            }
        """
        println("Request JSON:\n$requestBody")
        val request = MockMvcRequestBuilders.post("/projects")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
        val response =
            mockMvc
                .perform(request)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.project.name").value(projectName))
                .andExpect(jsonPath("$.data.project.description").value(projectDescription))
                .andExpect(jsonPath("$.data.project.colorCode").value(projectColorCode))
                .andExpect(jsonPath("$.data.project.startDate").value(projectStartDate))
                .andExpect(jsonPath("$.data.project.endDate").value(projectEndDate))
                .andExpect(jsonPath("$.data.project.parentId").value(projectParentId))
                .andExpect(jsonPath("$.data.project.leaderId").value(projectLeaderId))
                .andExpect(jsonPath("$.data.project.externalTaskId").value(projectExternalTaskId))
                .andExpect(jsonPath("$.data.project.githubRepo").value(projectGithubRepo))
                .andExpect(jsonPath("$.data.project.content.raw").value(projectContentRaw))
                .andExpect(jsonPath("$.data.project.externalCollaborators[0].userId").value(1002))
        projectId =
            JSONObject(response.andReturn().response.contentAsString)
                .getJSONObject("data")
                .getJSONObject("project")
                .getLong("id")
        logger.info("Created project: $projectId")
    }

    @Test
    @Order(60)
    fun testEnumerateProjectsByDefault() {
        val requestBuilders =
            MockMvcRequestBuilders.get("/projects")
//                .param("parent_id")
//                .param("leader_id")
//                .param("member_id")
//                .param("status")
                .param("page_start", "1")
                .param("page_size", "5")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(requestBuilders)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.projects[0].name").value(projectName))
            .andExpect(jsonPath("$.data.page.page_size").value(5))
            .andExpect(jsonPath("$.data.page.has_prev").value(false))
            .andExpect(jsonPath("$.data.page.prev_start").isEmpty)
            .andExpect(jsonPath("$.data.page.has_more").value(false))
            .andExpect(jsonPath("$.data.page.next_start").isEmpty)
    }

    /*@Test
    @Order(70)
    fun testDeleteProject() {
        val request =
            MockMvcRequestBuilders.delete("/projects/$projectId")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request).andExpect(status().isOk)

        val getRequest =
            MockMvcRequestBuilders.get("/projects/$projectId")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(getRequest).andExpect(status().isNotFound)
    }*/
}