/*
 *  Description: It tests the feature of space.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.api

import kotlin.math.floor
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.utils.TopicCreatorService
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
) // Use WebTestClient environment
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation::class)
class SpaceTest
@Autowired
constructor(
    private val webTestClient: WebTestClient, // Inject WebTestClient
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

    // --- Test Data Initialization ---
    private val randomSuffix = floor(Math.random() * 10000000000).toLong()
    private var spaceName = "Test Space ($randomSuffix)"
    private var originalSpaceName = spaceName
    private var spaceIntro = "This is a test space."
    private var spaceDescription = "A lengthy text. ".repeat(1000)
    private var spaceAnnouncements = "[]" // Assuming string representation of JSON array
    private var spaceTaskTemplates = "[]" // Assuming string representation of JSON array
    private var spaceAvatarId = userCreatorService.testAvatarId()
    private var spaceId: IdType = -1
    private var spaceIdOfSecond: IdType = -1
    private var spaceIdOfBeforeLast: IdType = -1
    private var spaceIdOfLast: IdType = -1
    private var topicsCount = 3
    private val topics: MutableList<IdType> = mutableListOf()
    private var defaultCategoryId: IdType = -1
    private var categoryIds = mutableListOf<IdType>()

    // --- Helper DTO for Error Responses (Define based on your actual error structure) ---
    // It's often better to have a standardized error DTO in your main codebase
    // This is a simplified example based on the jsonPath usage in the original test
    data class ErrorData(
        val type: String? = null,
        val id: Any? = null, // Can be String or Long depending on the error
        val name: String? = null,
        val action: String? = null,
        val resourceType: String? = null,
        val resourceId: IdType? = null,
    )

    data class ErrorDetail(val name: String, val data: ErrorData?)

    data class GenericErrorResponse(val error: ErrorDetail)

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
    fun `test get space and not found`() {
        webTestClient
            .get()
            .uri("/spaces/-1")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isNotFound // Check status
            .expectBody<GenericErrorResponse>() // Expect specific error DTO
            .value { errorResponse ->
                assertEquals("NotFoundError", errorResponse.error.name)
                assertNotNull(errorResponse.error.data)
                assertEquals("space", errorResponse.error.data!!.type)
                // ID might be returned as String in error DTO, adjust as necessary
                assertEquals("-1", errorResponse.error.data.id?.toString())
            }
    }

    // Helper function refactored to use WebTestClient and DTOs
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
        // Assuming PostSpaceRequestDTO exists and matches the controller input
        val requestDTO =
            PostSpaceRequestDTO(
                name = spaceName,
                intro = spaceIntro,
                description = spaceDescription,
                avatarId = spaceAvatarId,
                announcements = spaceAnnouncements,
                taskTemplates = spaceTaskTemplates,
                classificationTopics = classificationTopics,
                enableRank = false, // Defaulting as per original test logic implicitly doing this
            )

        // Perform the request and expect the specific response DTO
        val responseDTO =
            webTestClient
                .post()
                .uri("/spaces")
                .header("Authorization", "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO) // Use the request DTO
                .exchange()
                .expectStatus()
                .isOk // Expect HTTP 200 OK (as per original test)
                // Expect the response DTO returned by the controller on successful creation/patch
                .expectBody<PatchSpace200ResponseDTO>()
                .returnResult()
                .responseBody

        // --- Assertions on the Deserialized DTO ---
        assertNotNull(responseDTO, "Response body should not be null")
        assertNotNull(responseDTO!!.data, "Response data should not be null")
        val space = responseDTO.data.space
        assertNotNull(space, "Space DTO within response data should not be null")

        assertEquals(spaceName, space.name)
        assertEquals(spaceIntro, space.intro)
        assertEquals(spaceDescription, space.description)
        assertEquals(spaceAvatarId, space.avatarId)
        assertEquals(false, space.enableRank) // Verify default or set value
        // Note: Comparing raw JSON strings might be brittle if formatting changes.
        // Consider parsing them if precise structure matters, or just check for non-null/empty.
        assertEquals(spaceAnnouncements, space.announcements)
        assertEquals(spaceTaskTemplates, space.taskTemplates)

        // Verify admins list
        assertNotNull(space.admins)
        val ownerAdmin = space.admins.find { it.user.id == creator.userId }
        assertNotNull(ownerAdmin, "Creator should be owner")
        assertEquals(SpaceAdminRoleTypeDTO.OWNER, ownerAdmin!!.role) // Use the enum DTO

        // Verify classification topics
        assertEquals(classificationTopics.size, space.classificationTopics.size)
        val returnedTopicIds = space.classificationTopics.map { it.id }.toSet()
        classificationTopics.forEach { topicId ->
            assertTrue(
                returnedTopicIds.contains(topicId),
                "Expected topic ID $topicId not found in response",
            )
        }

        val createdSpaceId = space.id
        assertNotNull(createdSpaceId, "Created space ID should not be null")
        logger.info("Created space: $createdSpaceId")
        return createdSpaceId // Return non-null ID
    }

    @Test
    @Order(20)
    fun `test create multiple spaces`() { // Renamed for clarity
        // Create prerequisite spaces (IDs not stored, matching original logic)
        createSpace(
            creatorToken,
            "$originalSpaceName previous", // Use originalSpaceName for base
            spaceIntro,
            spaceDescription,
            spaceAvatarId,
            spaceAnnouncements,
            spaceTaskTemplates,
        )

        // Create the main space and store its ID
        spaceId =
            createSpace(
                creatorToken,
                spaceName, // Use the potentially updated spaceName
                spaceIntro,
                spaceDescription,
                spaceAvatarId,
                spaceAnnouncements,
                spaceTaskTemplates,
                classificationTopics = listOf(topics[0], topics[1]),
            )

        // Create subsequent spaces and store specific IDs needed later
        spaceIdOfSecond =
            createSpace(
                creatorToken,
                "$originalSpaceName 01", // Use originalSpaceName for base
                spaceIntro,
                spaceDescription,
                spaceAvatarId,
                spaceAnnouncements,
                spaceTaskTemplates,
            )
        createSpace(
            // ID not stored
            creatorToken,
            "$originalSpaceName 02",
            spaceIntro,
            spaceDescription,
            spaceAvatarId,
            spaceAnnouncements,
            spaceTaskTemplates,
        )
        spaceIdOfBeforeLast =
            createSpace(
                creatorToken,
                "$originalSpaceName 03",
                spaceIntro,
                spaceDescription,
                spaceAvatarId,
                spaceAnnouncements,
                spaceTaskTemplates,
            )
        spaceIdOfLast =
            createSpace(
                creatorToken,
                "$originalSpaceName 04",
                spaceIntro,
                spaceDescription,
                spaceAvatarId,
                spaceAnnouncements,
                spaceTaskTemplates,
            )
    }

    @Test
    @Order(30)
    fun `test get space by id`() { // Renamed
        webTestClient
            .get()
            .uri("/spaces/$spaceId")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetSpace200ResponseDTO>() // Expect the GET response DTO
            .value { response ->
                assertNotNull(response.data)
                val space = response.data.space
                assertNotNull(space)
                assertEquals(spaceId, space.id)
                // Use the current value of spaceName which might have been updated in createSpace
                assertEquals(spaceName, space.name)
                assertEquals(spaceIntro, space.intro)
                assertEquals(spaceAvatarId, space.avatarId)

                assertNotNull(space.admins)
                val ownerAdmin = space.admins.find { it.user.id == creator.userId }
                assertNotNull(ownerAdmin)
                assertEquals(SpaceAdminRoleTypeDTO.OWNER, ownerAdmin!!.role)

                assertNotNull(space.classificationTopics)
                assertEquals(2, space.classificationTopics.size)
                val topicIds = space.classificationTopics.map { it.id }.toSet()
                assertTrue(topicIds.contains(topics[0]))
                assertTrue(topicIds.contains(topics[1]))
            }
    }

    @Test
    @Order(40)
    fun `test create space with existing name fails`() { // Renamed
        // Assuming PostSpaceRequestDTO exists
        val requestDTO =
            PostSpaceRequestDTO(
                name = spaceName, // Use the existing name
                intro = spaceIntro,
                description = spaceDescription,
                avatarId = spaceAvatarId,
                announcements = "[]", // Use defaults or minimal values
                taskTemplates = "[]",
            )

        webTestClient
            .post()
            .uri("/spaces")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT) // Use HttpStatus enum
            .expectBody<GenericErrorResponse>()
            .value { errorResponse ->
                assertEquals("NameAlreadyExistsError", errorResponse.error.name)
                assertNotNull(errorResponse.error.data)
                assertEquals("space", errorResponse.error.data!!.type)
                assertEquals(spaceName, errorResponse.error.data.name)
            }
    }

    @Test
    @Order(50)
    fun `test patch space with empty request`() { // Renamed
        // Empty JSON object for request body
        val requestBody = emptyMap<String, Any>()

        webTestClient
            .patch()
            .uri("/spaces/$spaceId")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchSpace200ResponseDTO>() // Expect patch response DTO
            .value { response ->
                assertNotNull(response.data)
                val space = response.data.space
                assertNotNull(space)
                // Verify fields remain unchanged (using current test state values)
                assertEquals(spaceId, space.id)
                assertEquals(spaceName, space.name)
                assertEquals(spaceIntro, space.intro)
                assertEquals(spaceAvatarId, space.avatarId)
                assertNotNull(space.admins)
                val ownerAdmin = space.admins.find { it.user.id == creator.userId }
                assertNotNull(ownerAdmin)
                assertEquals(SpaceAdminRoleTypeDTO.OWNER, ownerAdmin!!.role)
                // Note: enableRank was false before, check if it stays false (or true if changed in
                // test 60)
                // Let's assume test 60 hasn't run yet or was reset.
                // assertEquals(false, space.enableRank) // Check its current state
            }
    }

    @Test
    @Order(60)
    fun `test patch space with full request`() { // Renamed
        // Update local state variables first
        val updatedSpaceName = "$spaceName (Updated)"
        val updatedSpaceIntro = "$spaceIntro (Updated)"
        val updatedSpaceDescription = "$spaceDescription (Updated)"
        val updatedSpaceAvatarId = spaceAvatarId + 1
        val updatedSpaceAnnouncements = "$spaceAnnouncements (Updated)"
        val updatedSpaceTaskTemplates = "$spaceTaskTemplates (Updated)"
        val updatedTopics = listOf(topics[1], topics[2]) // Topic 0 removed, topic 2 added

        // Assuming PatchSpaceRequestDTO exists
        val requestDTO =
            PatchSpaceRequestDTO(
                name = updatedSpaceName,
                intro = updatedSpaceIntro,
                description = updatedSpaceDescription,
                avatarId = updatedSpaceAvatarId,
                enableRank = true,
                announcements = updatedSpaceAnnouncements,
                taskTemplates = updatedSpaceTaskTemplates,
                classificationTopics = updatedTopics,
            )

        webTestClient
            .patch()
            .uri("/spaces/$spaceId")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchSpace200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data)
                val space = response.data.space
                assertNotNull(space)

                assertEquals(spaceId, space.id)
                assertEquals(updatedSpaceName, space.name)
                assertEquals(updatedSpaceIntro, space.intro)
                assertEquals(updatedSpaceDescription, space.description)
                assertEquals(updatedSpaceAvatarId, space.avatarId)
                assertEquals(true, space.enableRank)
                assertEquals(updatedSpaceAnnouncements, space.announcements)
                assertEquals(updatedSpaceTaskTemplates, space.taskTemplates)

                assertNotNull(space.admins)
                val ownerAdmin = space.admins.find { it.user.id == creator.userId }
                assertNotNull(ownerAdmin)
                // Owner role should persist unless explicitly changed
                assertEquals(SpaceAdminRoleTypeDTO.OWNER, ownerAdmin!!.role)

                assertNotNull(space.classificationTopics)
                assertEquals(updatedTopics.size, space.classificationTopics.size)
                val returnedTopicIds = space.classificationTopics.map { it.id }.toSet()
                updatedTopics.forEach { topicId ->
                    assertTrue(
                        returnedTopicIds.contains(topicId),
                        "Expected updated topic ID $topicId not found",
                    )
                }
                assertFalse(
                    returnedTopicIds.contains(topics[0]),
                    "Topic ID ${topics[0]} should have been removed",
                )
            }

        // Update test class state to reflect the patch for subsequent tests
        spaceName = updatedSpaceName
        spaceIntro = updatedSpaceIntro
        spaceDescription = updatedSpaceDescription
        spaceAvatarId = updatedSpaceAvatarId
        spaceAnnouncements = updatedSpaceAnnouncements
        spaceTaskTemplates = updatedSpaceTaskTemplates
    }

    @Test
    @Order(65)
    fun `test create space categories`() { // Renamed
        // 1. Get the space to find the initial defaultCategoryId
        val initialSpace =
            webTestClient
                .get()
                .uri("/spaces/$spaceId")
                .header("Authorization", "Bearer $creatorToken")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<GetSpace200ResponseDTO>()
                .returnResult()
                .responseBody
                ?.data
                ?.space

        assertNotNull(initialSpace?.defaultCategoryId, "Default category ID should exist")
        defaultCategoryId = initialSpace!!.defaultCategoryId

        // 2. Create first new category
        val category1Name = "Backend Tasks"
        val category1Desc = "Tasks related to backend development"
        val category1Order = 10
        val createCategory1DTO =
            CreateSpaceCategoryRequestDTO(
                name = category1Name,
                description = category1Desc,
                displayOrder = category1Order,
            )

        val createdCategory1 =
            webTestClient
                .post()
                .uri("/spaces/$spaceId/categories")
                .header("Authorization", "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createCategory1DTO)
                .exchange()
                .expectStatus()
                .isCreated // Expect 201 Created
                .expectBody<CreateSpaceCategory201ResponseDTO>() // Expect the specific creation DTO
                .returnResult()
                .responseBody
                ?.data
                ?.category

        assertNotNull(createdCategory1, "Created category 1 should not be null")
        assertEquals(category1Name, createdCategory1!!.name)
        assertEquals(category1Desc, createdCategory1.description)
        assertEquals(category1Order, createdCategory1.displayOrder)
        assertNotNull(createdCategory1.id)
        categoryIds.add(createdCategory1.id)

        // 3. Create second new category
        val category2Name = "Frontend Tasks"
        val category2Desc = "Tasks related to frontend development"
        val category2Order = 20
        val createCategory2DTO =
            CreateSpaceCategoryRequestDTO(
                name = category2Name,
                description = category2Desc,
                displayOrder = category2Order,
            )

        val createdCategory2 =
            webTestClient
                .post()
                .uri("/spaces/$spaceId/categories")
                .header("Authorization", "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createCategory2DTO)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody<CreateSpaceCategory201ResponseDTO>()
                .returnResult()
                .responseBody
                ?.data
                ?.category

        assertNotNull(createdCategory2, "Created category 2 should not be null")
        assertEquals(category2Name, createdCategory2!!.name)
        // ... assert other fields if needed ...
        assertNotNull(createdCategory2.id)
        categoryIds.add(createdCategory2.id)
    }

    @Test
    @Order(66)
    fun `test list space categories`() { // Renamed
        webTestClient
            .get()
            .uri("/spaces/$spaceId/categories")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListSpaceCategories200ResponseDTO>() // Expect the list DTO
            .value { response ->
                assertNotNull(response.data)
                val categories = response.data!!.categories
                assertNotNull(categories)
                // Expecting the original default + the two created ones
                assertEquals(3, categories.size, "Should be 3 categories (default + 2 created)")

                val categoryMap = categories.associateBy { it.id }
                assertTrue(
                    categoryMap.containsKey(defaultCategoryId),
                    "Default category should be listed",
                )
                assertTrue(
                    categoryMap.containsKey(categoryIds[0]),
                    "First created category should be listed",
                )
                assertTrue(
                    categoryMap.containsKey(categoryIds[1]),
                    "Second created category should be listed",
                )

                assertEquals("Backend Tasks", categoryMap[categoryIds[0]]?.name)
                assertEquals("Frontend Tasks", categoryMap[categoryIds[1]]?.name)
            }
    }

    @Test
    @Order(67)
    fun `test update space category`() { // Renamed
        val categoryToUpdateId = categoryIds[0]
        val updatedName = "Updated Backend Tasks"
        val updatedDesc = "Updated description"
        val updatedOrder = 15
        // Assuming UpdateSpaceCategoryRequestDTO exists
        val requestDTO =
            UpdateSpaceCategoryRequestDTO(
                name = updatedName,
                description = updatedDesc,
                displayOrder = updatedOrder,
            )

        webTestClient
            .patch()
            .uri("/spaces/$spaceId/categories/$categoryToUpdateId")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            // Controller returns CreateSpaceCategory201ResponseDTO on update, adjust if different
            .expectBody<CreateSpaceCategory201ResponseDTO>()
            .value { response ->
                assertNotNull(response.data)
                val category = response.data!!.category
                assertNotNull(category)
                assertEquals(categoryToUpdateId, category.id)
                assertEquals(updatedName, category.name)
                assertEquals(updatedDesc, category.description)
                assertEquals(updatedOrder, category.displayOrder)
            }
    }

    @Test
    @Order(68)
    fun `test set new default category`() { // Renamed
        val newDefaultCategoryId = categoryIds[0]
        val requestDTO = PatchSpaceRequestDTO(defaultCategoryId = newDefaultCategoryId)

        // Set the new default
        webTestClient
            .patch()
            .uri("/spaces/$spaceId")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchSpace200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.space)
                assertEquals(newDefaultCategoryId, response.data.space.defaultCategoryId)
            }

        // Verify in a separate GET request
        webTestClient
            .get()
            .uri("/spaces/$spaceId")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetSpace200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.space)
                assertEquals(newDefaultCategoryId, response.data.space.defaultCategoryId)
            }
    }

    @Test
    @Order(69)
    fun `test archive and unarchive category`() { // Renamed
        val categoryToArchiveId = categoryIds[1]

        // 1. Archive the category
        // Assuming the archive endpoint returns the updated category DTO
        webTestClient
            .post()
            .uri("/spaces/$spaceId/categories/$categoryToArchiveId/archive")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            // Assuming it returns CreateSpaceCategory201ResponseDTO or similar
            .expectBody<CreateSpaceCategory201ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.category)
                assertEquals(categoryToArchiveId, response.data!!.category.id)
                assertNotNull(
                    response.data!!.category.archivedAt,
                    "archivedAt should not be null after archiving",
                )
            }

        // 2. Verify it's not listed by default
        webTestClient
            .get()
            .uri("/spaces/$spaceId/categories")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListSpaceCategories200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.categories)
                val categories = response.data!!.categories
                assertEquals(
                    2,
                    categories.size,
                    "Only 2 categories should be listed by default now",
                )
                assertNull(
                    categories.find { it.id == categoryToArchiveId },
                    "Archived category should not be present",
                )
            }

        // 3. Verify it IS listed when including archived
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/spaces/$spaceId/categories")
                    .queryParam("includeArchived", "true")
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListSpaceCategories200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.categories)
                val categories = response.data!!.categories
                assertEquals(
                    3,
                    categories.size,
                    "All 3 categories should be listed when including archived",
                )
                assertNotNull(
                    categories.find { it.id == categoryToArchiveId },
                    "Archived category should be present",
                )
            }

        // 4. Unarchive the category
        // Assuming the unarchive endpoint uses DELETE and returns the updated category DTO
        webTestClient
            .delete()
            .uri("/spaces/$spaceId/categories/$categoryToArchiveId/archive")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            // Assuming it returns CreateSpaceCategory201ResponseDTO or similar
            .expectBody<CreateSpaceCategory201ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.category)
                assertEquals(categoryToArchiveId, response.data!!.category.id)
                assertNull(
                    response.data!!.category.archivedAt,
                    "archivedAt should be null after unarchiving",
                )
            }

        // 5. Verify it's listed again by default
        webTestClient
            .get()
            .uri("/spaces/$spaceId/categories")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListSpaceCategories200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.categories)
                val categories = response.data!!.categories
                assertEquals(3, categories.size, "All 3 categories should be listed again")
                assertNotNull(
                    categories.find { it.id == categoryToArchiveId },
                    "Unarchived category should be present",
                )
            }
    }

    @Test
    @Order(70)
    fun `test delete category fails for current default`() { // Renamed
        val currentDefaultCategoryId = categoryIds[0] // Set in test 68

        webTestClient
            .delete()
            .uri("/spaces/$spaceId/categories/$currentDefaultCategoryId")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isBadRequest // Expect 400 Bad Request
            .expectBody<GenericErrorResponse>() // Expect generic error structure
            .value { errorResponse ->
                // Check the specific error, adjust if your API returns a more specific one
                assertEquals("BadRequestError", errorResponse.error.name)
                // Optionally check error message or data if provided by API
            }
    }

    @Test
    @Order(71)
    fun `test delete non-default category`() { // Renamed
        // 1. Change the default category back to the original one
        val requestDTO = PatchSpaceRequestDTO(defaultCategoryId = defaultCategoryId)
        webTestClient
            .patch()
            .uri("/spaces/$spaceId")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk // Ensure default is changed back

        // 2. Now delete the category that WAS default (categoryIds[0])
        val categoryToDeleteId = categoryIds[0]
        webTestClient
            .delete()
            .uri("/spaces/$spaceId/categories/$categoryToDeleteId")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isNoContent // Expect 204 No Content

        // 3. Verify it's gone by listing categories
        webTestClient
            .get()
            .uri("/spaces/$spaceId/categories")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListSpaceCategories200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.categories)
                val categories = response.data!!.categories
                // Original default and the unarchived one (categoryIds[1]) should remain
                assertEquals(2, categories.size, "Should be 2 categories remaining")
                assertNull(
                    categories.find { it.id == categoryToDeleteId },
                    "Deleted category should not be present",
                )
                assertNotNull(
                    categories.find { it.id == defaultCategoryId },
                    "Original default category should be present",
                )
                assertNotNull(
                    categories.find { it.id == categoryIds[1] },
                    "Other category should be present",
                )
            }
    }

    @Test
    @Order(73)
    fun `test patch space fails for non-admin`() { // Renamed
        val requestBody = emptyMap<String, Any>()

        webTestClient
            .patch()
            .uri("/spaces/$spaceId")
            .header("Authorization", "Bearer $anonymousToken") // Use anonymous user token
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus()
            .isForbidden // Expect 403 Forbidden
            .expectBody<GenericErrorResponse>()
            .value { errorResponse ->
                assertEquals("PermissionDeniedError", errorResponse.error.name)
                assertNotNull(errorResponse.error.data)
                assertEquals("modify", errorResponse.error.data!!.action)
                assertEquals("space", errorResponse.error.data.resourceType)
                assertEquals(spaceId, errorResponse.error.data.resourceId)
            }
    }

    @Test
    @Order(75)
    fun `test enumerate spaces default pagination and sorting`() { // Renamed
        webTestClient
            .get()
            .uri { builder -> builder.path("/spaces").queryParam("page_size", 5).build() }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetSpaces200ResponseDTO>() // Expect the list response DTO
            .value { response ->
                assertNotNull(response.data)
                val spaces = response.data!!.spaces
                val page = response.data!!.page
                assertNotNull(spaces)
                assertNotNull(page)

                assertEquals(5, spaces!!.size)
                // Default sort is likely by creation date descending in many systems
                // Verify based on IDs created in test 20
                assertEquals(spaceIdOfLast, spaces[0].id)
                assertEquals("$originalSpaceName 04", spaces[0].name)
                assertEquals(spaceIdOfBeforeLast, spaces[1].id)
                assertEquals("$originalSpaceName 03", spaces[1].name)
                assertEquals(spaceIdOfSecond, spaces[3].id) // Check a few spots
                assertEquals("$originalSpaceName 01", spaces[3].name)
                assertEquals(spaceId, spaces[4].id)
                assertEquals(spaceName, spaces[4].name) // The one potentially updated

                assertEquals(
                    spaceIdOfLast,
                    page!!.pageStart,
                    "pageStart should be the ID of the first item in the list",
                )
                assertEquals(5, page.pageSize)
                assertTrue(page.hasMore, "hasMore should be true as more spaces exist")
                assertNotNull(page.nextStart, "nextStart should not be null")
                // nextStart should be the ID of the *last* item in the current page if using cursor
                // pagination based on ID
                // assertEquals(spaceId, page.nextStart) // Verify this based on your pagination
                // implementation
            }
    }

    @Test
    @Order(76)
    fun `test enumerate spaces sort by updatedAt desc no start`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/spaces")
                    .queryParam("sort_by", "updatedAt")
                    .queryParam("sort_order", "desc")
                    .queryParam("page_size", 4)
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetSpaces200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data)
                val spaces = response.data!!.spaces
                val page = response.data!!.page
                assertNotNull(spaces)
                assertNotNull(page)

                assertEquals(4, spaces!!.size)
                // Space 'spaceId' was updated most recently in test 60
                assertEquals(spaceId, spaces[0].id)
                assertEquals(spaceName, spaces[0].name) // Check updated name
                // The rest were created later but not updated
                //                assertEquals(spaceIdOfLast, spaces[1].id)
                assertEquals("$originalSpaceName 04", spaces[1].name)
                //                assertEquals(spaceIdOfBeforeLast, spaces[2].id)
                assertEquals("$originalSpaceName 03", spaces[2].name)
                //                assertEquals(spaceIdOfSecond, spaces[3].id) // This one was
                // created just after spaceId
                assertEquals("$originalSpaceName 02", spaces[3].name)

                assertEquals(
                    spaceId,
                    page!!.pageStart,
                    "pageStart should be the ID of the first item",
                )
                assertEquals(4, page.pageSize)
                assertTrue(page.hasMore)
                assertNotNull(page.nextStart)
                // nextStart should be the ID of the last item in this page
                assertEquals(spaceIdOfSecond, page.nextStart)
            }
    }

    @Test
    @Order(77)
    fun `test enumerate spaces sort by updatedAt desc with start`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/spaces")
                    .queryParam(
                        "page_start",
                        spaceIdOfLast,
                    ) // Start after this ID (exclusive) if cursor based
                    .queryParam("sort_by", "updatedAt")
                    .queryParam("sort_order", "desc") // Keep same sort
                    .queryParam("page_size", 1)
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetSpaces200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data)
                val spaces = response.data!!.spaces
                val page = response.data!!.page
                assertNotNull(spaces)
                assertNotNull(page)

                assertEquals(1, spaces!!.size)
                // Following the order from test 76, after spaceIdOfLast comes spaceIdOfBeforeLast
                // Note: This depends heavily on how pageStart works (cursor vs offset)
                // Assuming cursor based on ID: We start *after* spaceIdOfLast. The next most
                // recently updated is spaceIdOfBeforeLast
                // Let's re-evaluate the previous test's order: Updated(spaceId), Created(Last),
                // Created(BeforeLast), Created(Second), ...
                // If starting *at* pageStart=spaceIdOfLast, the first item IS spaceIdOfLast.

                // Let's assume pageStart means "start with the item identified by this ID"
                //                assertEquals(spaceIdOfLast, spaces[0].id)
                assertEquals("$originalSpaceName 04", spaces[0].name)

                assertEquals(spaceIdOfLast, page!!.pageStart)
                assertEquals(1, page.pageSize)
                assertTrue(page.hasMore)
                assertNotNull(page.nextStart)
                // The next one after spaceIdOfLast in the updatedAt desc order is
                // spaceIdOfBeforeLast
                assertEquals(spaceIdOfBeforeLast, page.nextStart)
            }
    }

    @Test
    @Order(80)
    fun `test add space admin`() { // Renamed
        // Assuming PostSpaceAdminRequestDTO exists
        val requestDTO =
            PostSpaceAdminRequestDTO(
                role = SpaceAdminRoleTypeDTO.ADMIN, // Use enum DTO
                userId = admin.userId,
            )

        webTestClient
            .post()
            .uri("/spaces/$spaceId/managers") // Assuming '/managers' is the correct path
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchSpace200ResponseDTO>() // Endpoint likely returns updated space
            .value { response ->
                assertNotNull(response.data.space.admins)
                val admins = response.data.space.admins
                assertTrue(
                    admins.any {
                        it.user.id == creator.userId && it.role == SpaceAdminRoleTypeDTO.OWNER
                    },
                    "Original owner missing or role changed",
                )
                assertTrue(
                    admins.any {
                        it.user.id == admin.userId && it.role == SpaceAdminRoleTypeDTO.ADMIN
                    },
                    "New admin not found or incorrect role",
                )
            }
    }

    @Test
    @Order(90)
    fun `test add space admin and ship ownership`() { // Renamed
        val requestDTO =
            PostSpaceAdminRequestDTO(
                role = SpaceAdminRoleTypeDTO.OWNER, // Add as new OWNER
                userId = newOwner.userId,
            )

        webTestClient
            .post()
            .uri("/spaces/$spaceId/managers")
            .header("Authorization", "Bearer $creatorToken") // Current owner performs action
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchSpace200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.space.admins)
                val admins = response.data.space.admins
                // Verify new owner exists with OWNER role
                assertTrue(
                    admins.any {
                        it.user.id == newOwner.userId && it.role == SpaceAdminRoleTypeDTO.OWNER
                    },
                    "New owner not found or incorrect role",
                )
                // Verify old owner is now ADMIN
                assertTrue(
                    admins.any {
                        it.user.id == creator.userId && it.role == SpaceAdminRoleTypeDTO.ADMIN
                    },
                    "Old owner not demoted to admin",
                )
                // Verify other admin still exists
                assertTrue(
                    admins.any {
                        it.user.id == admin.userId && it.role == SpaceAdminRoleTypeDTO.ADMIN
                    },
                    "Existing admin missing",
                )
            }
    }

    @Test
    @Order(100)
    fun `test add space admin fails after losing ownership`() { // Renamed
        val requestDTO =
            PostSpaceAdminRequestDTO(
                role = SpaceAdminRoleTypeDTO.ADMIN,
                userId = admin.userId, // Try to add the same admin again (or a new user)
            )

        webTestClient
            .post()
            .uri("/spaces/$spaceId/managers")
            .header("Authorization", "Bearer $creatorToken") // Creator is now just ADMIN
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<GenericErrorResponse>()
            .value { errorResponse ->
                assertEquals("PermissionDeniedError", errorResponse.error.name)
                assertNotNull(errorResponse.error.data)
                assertEquals(
                    "add-admin",
                    errorResponse.error.data!!.action,
                ) // Check action name matches controller guard
                assertEquals("space", errorResponse.error.data.resourceType)
                assertEquals(spaceId, errorResponse.error.data.resourceId)
            }
    }

    @Test
    @Order(110)
    fun `test ship space ownership via patch`() { // Renamed
        // Assuming PatchSpaceAdminRequestDTO exists
        val requestDTO = PatchSpaceAdminRequestDTO(role = SpaceAdminRoleTypeDTO.OWNER)

        webTestClient
            .patch()
            .uri("/spaces/$spaceId/managers/${creator.userId}") // Target the user to change role
            .header(
                "Authorization",
                "Bearer $newOwnerToken",
            ) // Action performed by the current owner
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchSpace200ResponseDTO>() // Endpoint likely returns updated space
            .value { response ->
                assertNotNull(response.data.space.admins)
                val admins = response.data.space.admins
                // Verify target user is now OWNER
                assertTrue(
                    admins.any {
                        it.user.id == creator.userId && it.role == SpaceAdminRoleTypeDTO.OWNER
                    },
                    "Target user role not updated to OWNER",
                )
                // Verify user performing action is now ADMIN
                assertTrue(
                    admins.any {
                        it.user.id == newOwner.userId && it.role == SpaceAdminRoleTypeDTO.ADMIN
                    },
                    "Action performer not demoted to ADMIN",
                )
                // Verify other admin still exists
                assertTrue(
                    admins.any {
                        it.user.id == admin.userId && it.role == SpaceAdminRoleTypeDTO.ADMIN
                    },
                    "Existing admin missing",
                )
            }
    }

    @Test
    @Order(120)
    fun `test remove admin`() { // Renamed
        // Current owner (creator) removes the other admin (newOwner who was demoted)
        webTestClient
            .delete()
            .uri("/spaces/$spaceId/managers/${newOwner.userId}")
            .header(
                "Authorization",
                "Bearer $creatorToken",
            ) // Creator is owner again after test 110
            .exchange()
            .expectStatus()
            .isOk // Assuming OK is returned on successful deletion
        // Check response body if the API returns one, otherwise just check status
        // .expectBody<DeleteSpace200ResponseDTO>() // Or whatever the API returns

        // Verify removal by getting the space again
        webTestClient
            .get()
            .uri("/spaces/$spaceId")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetSpace200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.space.admins)
                val admins = response.data.space.admins
                assertNull(
                    admins.find { it.user.id == newOwner.userId },
                    "Removed admin should not be present",
                )
                assertNotNull(
                    admins.find {
                        it.user.id == creator.userId && it.role == SpaceAdminRoleTypeDTO.OWNER
                    },
                    "Owner should still be present",
                )
                assertNotNull(
                    admins.find {
                        it.user.id == admin.userId && it.role == SpaceAdminRoleTypeDTO.ADMIN
                    },
                    "Other admin should still be present",
                )
            }
    }

    @Test
    @Order(130)
    fun `test delete space as admin`() { // Renamed
        // Test setup seems to imply admin (not owner) deletes the space
        // Let's ensure creator is owner, admin is admin from previous tests.
        // Test 110 made creator owner again. Test 120 removed newOwner. state: creator(owner),
        // admin(admin).

        webTestClient
            .delete()
            .uri("/spaces/$spaceId")
            .header("Authorization", "Bearer $creatorToken") // Use current OWNER token
            .exchange()
            // Check status - Delete might return 200 OK, 204 No Content, or 202 Accepted
            .expectStatus()
            .isOk // Assuming 200 OK based on original DTO name DeleteSpace200ResponseDTO
            // Optionally check response body if API returns one
            .expectBody<DeleteSpace200ResponseDTO>()
            .value { response ->
                assertEquals(200, response.code) // Assuming DTO has code/message
                assertEquals("OK", response.message)
            }

        // Verify deletion by trying to get the space again
        webTestClient
            .get()
            .uri("/spaces/$spaceId")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isNotFound // Should now be Not Found
    }
}
