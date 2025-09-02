package org.rucca.cheese.client

import kotlin.math.floor
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

/**
 * Client for creating and managing test spaces. Provides reusable methods for space-related
 * operations in tests.
 */
@Service
class SpaceClient(private val userClient: UserClient) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun testSpaceName(): String {
        return "Test Space (${floor(Math.random() * 10000000000).toLong()})"
    }

    fun testSpaceIntro(): String {
        return "This is a test space."
    }

    fun testSpaceDescription(): String {
        return "A lengthy text. ".repeat(100)
    }

    /** Creates a space and returns its ID and default category ID. */
    fun createSpace(
        webTestClient: WebTestClient,
        creatorToken: String,
        spaceName: String = testSpaceName(),
        spaceIntro: String = testSpaceIntro(),
        spaceDescription: String = testSpaceDescription(),
        spaceAvatarId: IdType = userClient.testAvatarId(),
        spaceAnnouncements: String = "[]",
        spaceTaskTemplates: String = "[]",
        classificationTopics: List<IdType> = emptyList(),
        enableRank: Boolean = false,
    ): Pair<IdType, IdType> {
        val requestDTO =
            PostSpaceRequestDTO(
                name = spaceName,
                intro = spaceIntro,
                description = spaceDescription,
                avatarId = spaceAvatarId,
                announcements = spaceAnnouncements,
                taskTemplates = spaceTaskTemplates,
                classificationTopics = classificationTopics,
                enableRank = enableRank,
            )

        val responseDTO =
            webTestClient
                .post()
                .uri("/spaces")
                .header("Authorization", "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<PatchSpace200ResponseDTO>()
                .returnResult()
                .responseBody!!

        val spaceId = responseDTO.data.space.id
        val defaultCategoryId = responseDTO.data.space.defaultCategoryId!!

        logger.info("Created space: $spaceId with default category: $defaultCategoryId")
        return Pair(spaceId, defaultCategoryId)
    }

    /** Creates a category in a space. */
    fun createCategory(
        webTestClient: WebTestClient,
        token: String,
        spaceId: IdType,
        categoryName: String,
    ): IdType {
        val requestDTO = CreateSpaceCategoryRequestDTO(name = categoryName)

        val responseDTO =
            webTestClient
                .post()
                .uri("/spaces/$spaceId/categories")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<CreateSpaceCategory201ResponseDTO>()
                .returnResult()
                .responseBody!!

        val categoryId =
            responseDTO.data?.category?.id ?: throw RuntimeException("Category creation failed")
        logger.info("Created category: $categoryId in space: $spaceId")
        return categoryId
    }

    /** Archives a category in a space. */
    fun archiveCategory(
        webTestClient: WebTestClient,
        token: String,
        spaceId: IdType,
        categoryId: IdType,
    ) {
        webTestClient
            .post()
            .uri("/spaces/$spaceId/categories/$categoryId/archive")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus()
            .isOk()
    }

    /** Unarchives a category in a space. */
    fun unarchiveCategory(
        webTestClient: WebTestClient,
        token: String,
        spaceId: IdType,
        categoryId: IdType,
    ) {
        webTestClient
            .delete()
            .uri("/spaces/$spaceId/categories/$categoryId/archive")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus()
            .isOk()
    }
}
