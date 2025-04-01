/*
 *  Description: It tests the feature of space.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.api

import kotlin.math.floor
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.utils.TopicCreatorService
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class SpaceTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    private val userCreatorService: UserCreatorService,
    private val topicCreatorService: TopicCreatorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    lateinit var admin: UserCreatorService.CreateUserResponse
    lateinit var adminToken: String
    lateinit var newOwner: UserCreatorService.CreateUserResponse
    lateinit var newOwnerToken: String
    lateinit var anonymous: UserCreatorService.CreateUserResponse
    lateinit var anonymousToken: String
    private var spaceName = "Test Space (${floor(Math.random() * 10000000000).toLong()})"
    private var originalSpaceName = spaceName
    private var spaceIntro = "This is a test space."
    private var spaceDescription = "A lengthy text. ".repeat(1000)
    private var spaceAnnouncements = "[]"
    private var spaceTaskTemplates = "[]"
    private var spaceAvatarId = userCreatorService.testAvatarId()
    private var spaceId: IdType = -1
    private var spaceIdOfSecond: IdType = -1
    private var spaceIdOfBeforeLast: IdType = -1
    private var spaceIdOfLast: IdType = -1
    private var topicsCount = 3
    private val topics: MutableList<IdType> = mutableListOf()
    private var defaultCategoryId: IdType = -1
    private var categoryIds = mutableListOf<IdType>()

    @BeforeAll
    fun prepare() {
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
        admin = userCreatorService.createUser()
        adminToken = userCreatorService.login(admin.username, admin.password)
        newOwner = userCreatorService.createUser()
        newOwnerToken = userCreatorService.login(newOwner.username, newOwner.password)
        anonymous = userCreatorService.createUser()
        anonymousToken = userCreatorService.login(anonymous.username, anonymous.password)
        for (i in 1..topicsCount) {
            topics.add(
                topicCreatorService.createTopic(
                    creatorToken,
                    "Topic (${floor(Math.random() * 10000000000).toLong()}) ($i)",
                )
            )
        }
    }

    @Test
    @Order(10)
    fun testGetSpaceAndNotFound() {
        val request =
            MockMvcRequestBuilders.get("/spaces/-1").header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.name").value("NotFoundError"))
            .andExpect(jsonPath("$.error.data.type").value("space"))
            .andExpect(jsonPath("$.error.data.id").value("-1"))
    }

    fun createSpace(
        creatorToken: String,
        spaceName: String,
        spaceIntro: String,
        spaceDescription: String,
        spaceAvatarId: IdType,
        spaceAnnouncements: String,
        spaceTaskTemplates: String,
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
                .andExpect(jsonPath("$.data.space.admins[0].user.id").value(creator.userId))
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

    @Test
    @Order(20)
    fun testCreateSpace() {
        createSpace(
            creatorToken,
            "$spaceName previous",
            spaceIntro,
            spaceDescription,
            spaceAvatarId,
            spaceAnnouncements,
            spaceTaskTemplates,
        )
        spaceId =
            createSpace(
                creatorToken,
                spaceName,
                spaceIntro,
                spaceDescription,
                spaceAvatarId,
                spaceAnnouncements,
                spaceTaskTemplates,
                classificationTopics = listOf(topics[0], topics[1]),
            )
        spaceIdOfSecond =
            createSpace(
                creatorToken,
                "$spaceName 01",
                spaceIntro,
                spaceDescription,
                spaceAvatarId,
                spaceAnnouncements,
                spaceTaskTemplates,
            )
        createSpace(
            creatorToken,
            "$spaceName 02",
            spaceIntro,
            spaceDescription,
            spaceAvatarId,
            spaceAnnouncements,
            spaceTaskTemplates,
        )
        spaceIdOfBeforeLast =
            createSpace(
                creatorToken,
                "$spaceName 03",
                spaceIntro,
                spaceDescription,
                spaceAvatarId,
                spaceAnnouncements,
                spaceTaskTemplates,
            )
        spaceIdOfLast =
            createSpace(
                creatorToken,
                "$spaceName 04",
                spaceIntro,
                spaceDescription,
                spaceAvatarId,
                spaceAnnouncements,
                spaceTaskTemplates,
            )
    }

    @Test
    @Order(30)
    fun testGetSpace() {
        val request =
            MockMvcRequestBuilders.get("/spaces/$spaceId")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.space.id").value(spaceId))
            .andExpect(jsonPath("$.data.space.name").value(spaceName))
            .andExpect(jsonPath("$.data.space.intro").value(spaceIntro))
            .andExpect(jsonPath("$.data.space.avatarId").value(spaceAvatarId))
            .andExpect(jsonPath("$.data.space.admins[0].role").value("OWNER"))
            .andExpect(jsonPath("$.data.space.admins[0].user.id").value(creator.userId))
            .andExpect(jsonPath("$.data.space.classificationTopics.length()").value(2))
            .andExpect(
                jsonPath("$.data.space.classificationTopics[?(@.id == ${topics[0]})].name").exists()
            )
            .andExpect(
                jsonPath("$.data.space.classificationTopics[?(@.id == ${topics[1]})].name").exists()
            )
    }

    @Test
    @Order(40)
    fun testNameAlreadyExists() {
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
                    "announcements": "",
                    "taskTemplates": ""
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.name").value("NameAlreadyExistsError"))
            .andExpect(jsonPath("$.error.data.type").value("space"))
            .andExpect(jsonPath("$.error.data.name").value(spaceName))
    }

    @Test
    @Order(50)
    fun testPatchSpaceWithEmptyRequest() {
        val request =
            MockMvcRequestBuilders.patch("/spaces/$spaceId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content("{}")
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.space.id").value(spaceId))
            .andExpect(jsonPath("$.data.space.name").value(spaceName))
            .andExpect(jsonPath("$.data.space.intro").value(spaceIntro))
            .andExpect(jsonPath("$.data.space.avatarId").value(spaceAvatarId))
            .andExpect(jsonPath("$.data.space.admins[0].role").value("OWNER"))
            .andExpect(jsonPath("$.data.space.admins[0].user.id").value(creator.userId))
    }

    @Test
    @Order(60)
    fun testPatchSpaceWithFullRequest() {
        spaceName += " (Updated)"
        spaceIntro += " (Updated)"
        spaceDescription += " (Updated)"
        spaceAvatarId += 1
        spaceAnnouncements += " (Updated)"
        spaceTaskTemplates += " (Updated)"
        val request =
            MockMvcRequestBuilders.patch("/spaces/$spaceId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "name": "$spaceName",
                    "intro": "$spaceIntro",
                    "description": "$spaceDescription",
                    "avatarId": $spaceAvatarId,
                    "enableRank": true,
                    "announcements": "$spaceAnnouncements",
                    "taskTemplates": "$spaceTaskTemplates",
                    "classificationTopics": [${topics[1]}, ${topics[2]}]
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.space.id").value(spaceId))
            .andExpect(jsonPath("$.data.space.name").value(spaceName))
            .andExpect(jsonPath("$.data.space.intro").value(spaceIntro))
            .andExpect(jsonPath("$.data.space.description").value(spaceDescription))
            .andExpect(jsonPath("$.data.space.avatarId").value(spaceAvatarId))
            .andExpect(jsonPath("$.data.space.admins[0].role").value("OWNER"))
            .andExpect(jsonPath("$.data.space.admins[0].user.id").value(creator.userId))
            .andExpect(jsonPath("$.data.space.enableRank").value(true))
            .andExpect(jsonPath("$.data.space.announcements").value(spaceAnnouncements))
            .andExpect(jsonPath("$.data.space.taskTemplates").value(spaceTaskTemplates))
            .andExpect(jsonPath("$.data.space.classificationTopics.length()").value(2))
            .andExpect(
                jsonPath("$.data.space.classificationTopics[?(@.id == ${topics[1]})].name").exists()
            )
            .andExpect(
                jsonPath("$.data.space.classificationTopics[?(@.id == ${topics[2]})].name").exists()
            )
    }

    @Test
    @Order(65)
    fun testCreateCategory() {
        // First, get the default category ID that's created with the space
        val getSpaceRequest =
            MockMvcRequestBuilders.get("/spaces/$spaceId")
                .header("Authorization", "Bearer $creatorToken")
        val response = mockMvc.perform(getSpaceRequest).andExpect(status().isOk).andReturn()

        defaultCategoryId =
            JSONObject(response.response.contentAsString)
                .getJSONObject("data")
                .getJSONObject("space")
                .getLong("defaultCategoryId")

        // Create a new category
        val createRequest =
            MockMvcRequestBuilders.post("/spaces/$spaceId/categories")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "name": "Backend Tasks",
                    "description": "Tasks related to backend development",
                    "displayOrder": 10
                }
            """
                )

        val categoryId =
            JSONObject(
                    mockMvc
                        .perform(createRequest)
                        .andExpect(status().isCreated)
                        .andExpect(jsonPath("$.data.category.name").value("Backend Tasks"))
                        .andExpect(
                            jsonPath("$.data.category.description")
                                .value("Tasks related to backend development")
                        )
                        .andExpect(jsonPath("$.data.category.displayOrder").value(10))
                        .andReturn()
                        .response
                        .contentAsString
                )
                .getJSONObject("data")
                .getJSONObject("category")
                .getLong("id")

        categoryIds.add(categoryId)

        // Create another category
        val createRequest2 =
            MockMvcRequestBuilders.post("/spaces/$spaceId/categories")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "name": "Frontend Tasks",
                    "description": "Tasks related to frontend development",
                    "displayOrder": 20
                }
            """
                )

        val categoryId2 =
            JSONObject(
                    mockMvc
                        .perform(createRequest2)
                        .andExpect(status().isCreated)
                        .andExpect(jsonPath("$.data.category.name").value("Frontend Tasks"))
                        .andReturn()
                        .response
                        .contentAsString
                )
                .getJSONObject("data")
                .getJSONObject("category")
                .getLong("id")

        categoryIds.add(categoryId2)
    }

    @Test
    @Order(66)
    fun testListCategories() {
        val request =
            MockMvcRequestBuilders.get("/spaces/$spaceId/categories")
                .header("Authorization", "Bearer $creatorToken")

        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.categories.length()").value(3)) // Default + 2 created
            .andExpect(jsonPath("$.data.categories[?(@.id == $defaultCategoryId)].name").exists())
            .andExpect(
                jsonPath("$.data.categories[?(@.id == ${categoryIds[0]})].name")
                    .value("Backend Tasks")
            )
            .andExpect(
                jsonPath("$.data.categories[?(@.id == ${categoryIds[1]})].name")
                    .value("Frontend Tasks")
            )
    }

    @Test
    @Order(67)
    fun testUpdateCategory() {
        val request =
            MockMvcRequestBuilders.patch("/spaces/$spaceId/categories/${categoryIds[0]}")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "name": "Updated Backend Tasks",
                    "description": "Updated description",
                    "displayOrder": 15
                }
            """
                )

        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.category.name").value("Updated Backend Tasks"))
            .andExpect(jsonPath("$.data.category.description").value("Updated description"))
            .andExpect(jsonPath("$.data.category.displayOrder").value(15))
    }

    @Test
    @Order(68)
    fun testSetDefaultCategory() {
        // Set a new default category
        val request =
            MockMvcRequestBuilders.patch("/spaces/$spaceId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "defaultCategoryId": ${categoryIds[0]}
                }
            """
                )

        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.space.defaultCategoryId").value(categoryIds[0]))

        // Verify in a separate request
        val getRequest =
            MockMvcRequestBuilders.get("/spaces/$spaceId")
                .header("Authorization", "Bearer $creatorToken")

        mockMvc
            .perform(getRequest)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.space.defaultCategoryId").value(categoryIds[0]))
    }

    @Test
    @Order(69)
    fun testArchiveAndUnarchiveCategory() {
        // Archive the category
        val archiveRequest =
            MockMvcRequestBuilders.post("/spaces/$spaceId/categories/${categoryIds[1]}/archive")
                .header("Authorization", "Bearer $creatorToken")

        mockMvc
            .perform(archiveRequest)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.category.id").value(categoryIds[1]))
            .andExpect(jsonPath("$.data.category.archivedAt").isNotEmpty)

        // Verify that archived categories aren't listed by default
        val listRequest =
            MockMvcRequestBuilders.get("/spaces/$spaceId/categories")
                .header("Authorization", "Bearer $creatorToken")

        mockMvc
            .perform(listRequest)
            .andExpect(status().isOk)
            .andExpect(
                jsonPath("$.data.categories.length()").value(2)
            ) // Only default and the first one

        // Include archived categories
        val listWithArchivedRequest =
            MockMvcRequestBuilders.get("/spaces/$spaceId/categories")
                .param("includeArchived", "true")
                .header("Authorization", "Bearer $creatorToken")

        mockMvc
            .perform(listWithArchivedRequest)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.categories.length()").value(3)) // All three

        // Unarchive the category
        val unarchiveRequest =
            MockMvcRequestBuilders.delete("/spaces/$spaceId/categories/${categoryIds[1]}/archive")
                .header("Authorization", "Bearer $creatorToken")

        mockMvc
            .perform(unarchiveRequest)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.category.id").value(categoryIds[1]))
            .andExpect(jsonPath("$.data.category.archivedAt").isEmpty)
    }

    @Test
    @Order(70)
    fun testDeleteCategoryFailsForDefault() {
        // Try to delete the default category (should fail)
        val request =
            MockMvcRequestBuilders.delete("/spaces/$spaceId/categories/${categoryIds[0]}")
                .header("Authorization", "Bearer $creatorToken")

        mockMvc
            .perform(request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.name").value("BadRequestError"))
    }

    @Test
    @Order(71)
    fun testDeleteCategory() {
        // First, change the default category back to the original
        val setDefaultRequest =
            MockMvcRequestBuilders.patch("/spaces/$spaceId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "defaultCategoryId": $defaultCategoryId
                }
            """
                )

        mockMvc.perform(setDefaultRequest).andExpect(status().isOk)

        // Now delete the previously default category
        val deleteRequest =
            MockMvcRequestBuilders.delete("/spaces/$spaceId/categories/${categoryIds[0]}")
                .header("Authorization", "Bearer $creatorToken")

        mockMvc.perform(deleteRequest).andExpect(status().isNoContent)

        // Verify it's gone
        val listRequest =
            MockMvcRequestBuilders.get("/spaces/$spaceId/categories")
                .header("Authorization", "Bearer $creatorToken")

        mockMvc
            .perform(listRequest)
            .andExpect(status().isOk)
            .andExpect(
                jsonPath("$.data.categories.length()").value(2)
            ) // Original default and the unarchived one
            .andExpect(
                jsonPath("$.data.categories[?(@.id == ${categoryIds[0]})].name").doesNotExist()
            )
    }

    @Test
    @Order(73)
    fun testPatchSpaceWithAnonymous() {
        val request =
            MockMvcRequestBuilders.patch("/spaces/$spaceId")
                .header("Authorization", "Bearer $anonymousToken")
                .contentType("application/json")
                .content("{}")
        mockMvc
            .perform(request)
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.name").value("PermissionDeniedError"))
            .andExpect(jsonPath("$.error.data.action").value("modify"))
            .andExpect(jsonPath("$.error.data.resourceType").value("space"))
            .andExpect(jsonPath("$.error.data.resourceId").value(spaceId))
    }

    @Test
    @Order(75)
    fun testEnumerateSpacesByDefault() {
        val requestBuilders =
            MockMvcRequestBuilders.get("/spaces")
                .param("page_size", "5")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(requestBuilders)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.spaces[0].name").value("$originalSpaceName 04"))
            .andExpect(jsonPath("$.data.spaces[1].name").value("$originalSpaceName 03"))
            .andExpect(jsonPath("$.data.spaces[2].name").value("$originalSpaceName 02"))
            .andExpect(jsonPath("$.data.spaces[3].name").value("$originalSpaceName 01"))
            .andExpect(jsonPath("$.data.spaces[4].name").value(spaceName))
            .andExpect(jsonPath("$.data.page.page_start").value(spaceIdOfLast))
            .andExpect(jsonPath("$.data.page.page_size").value(5))
            .andExpect(jsonPath("$.data.page.has_more").value(true))
            .andExpect(jsonPath("$.data.page.next_start").isNotEmpty)
    }

    @Test
    @Order(76)
    fun testEnumerateSpacesByUpdatedAtDescWithoutStart() {
        val requestBuilders =
            MockMvcRequestBuilders.get("/spaces")
                .param("sort_by", "updatedAt")
                .param("sort_order", "desc")
                .param("page_size", "4")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(requestBuilders)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.spaces[0].name").value(spaceName))
            .andExpect(jsonPath("$.data.spaces[1].name").value("$originalSpaceName 04"))
            .andExpect(jsonPath("$.data.spaces[2].name").value("$originalSpaceName 03"))
            .andExpect(jsonPath("$.data.spaces[3].name").value("$originalSpaceName 02"))
            .andExpect(jsonPath("$.data.page.page_start").value(spaceId))
            .andExpect(jsonPath("$.data.page.page_size").value(4))
            .andExpect(jsonPath("$.data.page.has_more").value(true))
            .andExpect(jsonPath("$.data.page.next_start").value(spaceIdOfSecond))
    }

    @Test
    @Order(77)
    fun testEnumerateSpacesByUpdatedAtDescWithStart() {
        val requestBuilders =
            MockMvcRequestBuilders.get("/spaces")
                .param("page_start", spaceIdOfLast.toString())
                .param("sort_by", "updatedAt")
                .param("sort_order", "desc")
                .param("page_size", "1")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(requestBuilders)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.spaces[0].name").value("$originalSpaceName 04"))
            .andExpect(jsonPath("$.data.page.page_start").value(spaceIdOfLast))
            .andExpect(jsonPath("$.data.page.page_size").value(1))
            .andExpect(jsonPath("$.data.page.has_more").value(true))
            .andExpect(jsonPath("$.data.page.next_start").value(spaceIdOfBeforeLast))
    }

    @Test
    @Order(80)
    fun testAddSpaceAdmin() {
        val request =
            MockMvcRequestBuilders.post("/spaces/$spaceId/managers")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "role": "ADMIN",
                    "userId": ${admin.userId}
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.space.id").value(spaceId))
            .andExpect(jsonPath("$.data.space.name").value(spaceName))
            .andExpect(jsonPath("$.data.space.intro").value(spaceIntro))
            .andExpect(jsonPath("$.data.space.avatarId").value(spaceAvatarId))
            .andExpect(
                jsonPath(
                        "$.data.space.admins[?(@.user.id == '${admin.userId}' && @.role == 'ADMIN')])"
                    )
                    .exists()
            )
            .andExpect(
                jsonPath(
                        "$.data.space.admins[?(@.user.id == '${creator.userId}' && @.role == 'OWNER')])"
                    )
                    .exists()
            )
    }

    @Test
    @Order(90)
    fun testAddSpaceAdminAndShipOwnership() {
        val request =
            MockMvcRequestBuilders.post("/spaces/$spaceId/managers")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "role": "OWNER",
                    "userId": ${newOwner.userId}
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.space.id").value(spaceId))
            .andExpect(jsonPath("$.data.space.name").value(spaceName))
            .andExpect(jsonPath("$.data.space.intro").value(spaceIntro))
            .andExpect(jsonPath("$.data.space.avatarId").value(spaceAvatarId))
            .andExpect(
                jsonPath(
                        "$.data.space.admins[?(@.user.id == '${newOwner.userId}' && @.role == 'OWNER')])"
                    )
                    .exists()
            )
            .andExpect(
                jsonPath(
                        "$.data.space.admins[?(@.user.id == '${admin.userId}' && @.role == 'ADMIN')])"
                    )
                    .exists()
            )
            .andExpect(
                jsonPath(
                        "$.data.space.admins[?(@.user.id == '${creator.userId}' && @.role == 'ADMIN')])"
                    )
                    .exists()
            )
    }

    @Test
    @Order(100)
    fun testAddSpaceAdminAfterNotOwner() {
        val request =
            MockMvcRequestBuilders.post("/spaces/$spaceId/managers")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "role": "ADMIN",
                    "userId": ${admin.userId}
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.name").value("PermissionDeniedError"))
            .andExpect(jsonPath("$.error.data.action").value("add-admin"))
            .andExpect(jsonPath("$.error.data.resourceType").value("space"))
            .andExpect(jsonPath("$.error.data.resourceId").value(spaceId))
    }

    @Test
    @Order(110)
    fun testShipSpaceOwnership() {
        val request =
            MockMvcRequestBuilders.patch("/spaces/$spaceId/managers/${creator.userId}")
                .header("Authorization", "Bearer $newOwnerToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "role": "OWNER"
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.space.id").value(spaceId))
            .andExpect(jsonPath("$.data.space.name").value(spaceName))
            .andExpect(jsonPath("$.data.space.intro").value(spaceIntro))
            .andExpect(jsonPath("$.data.space.avatarId").value(spaceAvatarId))
            .andExpect(
                jsonPath(
                        "$.data.space.admins[?(@.user.id == '${creator.userId}' && @.role == 'OWNER')])"
                    )
                    .exists()
            )
            .andExpect(
                jsonPath(
                        "$.data.space.admins[?(@.user.id == '${admin.userId}' && @.role == 'ADMIN')])"
                    )
                    .exists()
            )
            .andExpect(
                jsonPath(
                        "$.data.space.admins[?(@.user.id == '${newOwner.userId}' && @.role == 'ADMIN')])"
                    )
                    .exists()
            )
    }

    @Test
    @Order(120)
    fun testRemoveAdmin() {
        val request =
            MockMvcRequestBuilders.delete("/spaces/$spaceId/managers/${newOwner.userId}")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request).andExpect(status().isOk)
    }

    @Test
    @Order(130)
    fun testDeleteSpace() {
        val request =
            MockMvcRequestBuilders.delete("/spaces/$spaceId")
                .header("Authorization", "Bearer $adminToken")
        mockMvc.perform(request).andExpect(status().isOk)
    }
}
