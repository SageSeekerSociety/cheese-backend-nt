/*
 *  Description: This file implements the SpaceService class.
 *               It is responsible for CRUD of a space.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.space

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import org.hibernate.query.SortDirection
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.common.error.BadRequestError
import org.rucca.cheese.common.error.ConflictError
import org.rucca.cheese.common.error.NameAlreadyExistsError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.EntityPatcher
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.pagination.model.toPageDTO
import org.rucca.cheese.common.pagination.repository.findAllWithIdCursor
import org.rucca.cheese.common.pagination.repository.idSeekSpec
import org.rucca.cheese.common.pagination.util.toJpaDirection
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.space.error.AlreadyBeSpaceAdminError
import org.rucca.cheese.space.error.NotSpaceAdminYetError
import org.rucca.cheese.space.models.Space
import org.rucca.cheese.space.models.SpaceAdminRelation
import org.rucca.cheese.space.models.SpaceAdminRole
import org.rucca.cheese.space.models.SpaceCategory
import org.rucca.cheese.space.option.SpaceQueryOptions
import org.rucca.cheese.space.repositories.SpaceAdminRelationRepository
import org.rucca.cheese.space.repositories.SpaceCategoryRepository
import org.rucca.cheese.space.repositories.SpaceRepository
import org.rucca.cheese.task.TaskRepository
import org.rucca.cheese.topic.TopicService
import org.rucca.cheese.user.*
import org.rucca.cheese.user.services.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SpaceService(
    private val spaceRepository: SpaceRepository,
    private val spaceAdminRelationRepository: SpaceAdminRelationRepository,
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val spaceUserRankService: SpaceUserRankService,
    private val jwtService: JwtService,
    private val topicService: TopicService,
    private val spaceClassificationTopicsService: SpaceClassificationTopicsService,
    private val avatarRepository: AvatarRepository,
    private val entityPatcher: EntityPatcher,
    private val spaceCategoryRepository: SpaceCategoryRepository,
    private val taskRepository: TaskRepository,
) {
    fun SpaceCategory.toDTO(): SpaceCategoryDTO {
        return SpaceCategoryDTO(
            id = this.id!!.toLong(),
            name = this.name,
            description = this.description,
            displayOrder = this.displayOrder,
            createdAt = this.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            updatedAt = this.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            archivedAt = this.archivedAt?.toEpochMilli(),
        )
    }

    fun SpaceAdminRelation.toDTO(): SpaceAdminDTO {
        val userId = this.user!!.id!!.toLong()
        val userDto = userService.getUserDto(userId)

        return SpaceAdminDTO(
            role = convertAdminRole(this.role!!),
            user = userDto,
            createdAt = this.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            updatedAt = this.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
    }

    fun Space.toSpaceDTO(
        options: SpaceQueryOptions,
        admins: List<SpaceAdminDTO>? = null,
        topics: List<TopicDTO>? = null,
        rank: Int? = null,
    ): SpaceDTO {
        val myRank =
            rank
                ?: if (options.queryMyRank && this.enableRank!!) {
                    val userId = jwtService.getCurrentUserId()
                    spaceUserRankService.getRank(this.id!!, userId)
                } else null

        val classificationTopics =
            topics ?: spaceClassificationTopicsService.getClassificationTopicDTOs(this.id!!)

        val adminDTOs =
            admins ?: spaceAdminRelationRepository.findAllBySpaceId(this.id!!).map { it.toDTO() }

        return SpaceDTO(
            id = this.id!!,
            intro = this.intro!!,
            description = this.description!!,
            name = this.name!!,
            avatarId = this.avatar!!.id!!.toLong(),
            admins = adminDTOs,
            updatedAt = this.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            createdAt = this.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            enableRank = this.enableRank!!,
            announcements = this.announcements!!,
            taskTemplates = this.taskTemplates!!,
            myRank = myRank,
            classificationTopics = classificationTopics,
            defaultCategoryId = defaultCategory!!.id!!,
        )
    }

    fun getSpaceDto(
        spaceId: IdType,
        queryOptions: SpaceQueryOptions = SpaceQueryOptions.MINIMUM,
    ): SpaceDTO {
        val space = getSpace(spaceId)

        val admins = spaceAdminRelationRepository.findAllBySpaceIdFetchUser(spaceId)
        val classificationTopics =
            spaceClassificationTopicsService.getClassificationTopicDTOs(spaceId)

        val myRank =
            if (queryOptions.queryMyRank && space.enableRank!!) {
                val userId = jwtService.getCurrentUserId()
                spaceUserRankService.getRank(spaceId, userId)
            } else null

        return SpaceDTO(
            id = space.id!!,
            intro = space.intro!!,
            description = space.description!!,
            name = space.name!!,
            avatarId = space.avatar!!.id!!.toLong(),
            admins = admins.map { it.toDTO() },
            updatedAt = space.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            createdAt = space.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            enableRank = space.enableRank!!,
            announcements = space.announcements!!,
            taskTemplates = space.taskTemplates!!,
            myRank = myRank,
            classificationTopics = classificationTopics,
            defaultCategoryId = space.defaultCategory!!.id!!,
        )
    }

    fun getSpaceOwner(spaceId: IdType): IdType {
        val spaceAdminRelation =
            spaceAdminRelationRepository
                .findBySpaceIdAndRole(spaceId, SpaceAdminRole.OWNER)
                .orElseThrow { NotFoundError("space", spaceId) }
        return spaceAdminRelation.user!!.id!!.toLong()
    }

    fun isSpaceAdmin(spaceId: IdType, userId: IdType): Boolean {
        return spaceAdminRelationRepository.existsBySpaceIdAndUserId(spaceId, userId)
    }

    fun convertAdminRole(role: SpaceAdminRole): SpaceAdminRoleTypeDTO {
        return when (role) {
            SpaceAdminRole.OWNER -> SpaceAdminRoleTypeDTO.OWNER
            SpaceAdminRole.ADMIN -> SpaceAdminRoleTypeDTO.ADMIN
        }
    }

    fun ensureSpaceNameNotExists(name: String) {
        if (spaceRepository.existsByName(name)) {
            throw NameAlreadyExistsError("space", name)
        }
    }

    fun createSpace(
        name: String,
        intro: String,
        description: String,
        avatarId: IdType,
        ownerId: IdType,
        enableRank: Boolean,
        announcements: String,
        taskTemplates: String,
        classificationTopics: List<IdType>,
    ): SpaceDTO {
        ensureSpaceNameNotExists(name)
        for (topic in classificationTopics) topicService.ensureTopicExists(topic)
        val space =
            spaceRepository.save(
                Space(
                    name = name,
                    intro = intro,
                    description = description,
                    avatar = Avatar().apply { id = avatarId.toInt() },
                    enableRank = enableRank,
                    announcements = announcements,
                    taskTemplates = taskTemplates,
                )
            )
        val newSpaceId = space.id!!

        val defaultCategory =
            SpaceCategory(
                    name = "General",
                    space = space,
                    description = "Default category for general items.",
                    displayOrder = 0,
                )
                .let { spaceCategoryRepository.save(it) }

        space.defaultCategory = defaultCategory
        spaceRepository.save(space) // Save the space again to set the default category

        val ownerRelation =
            spaceAdminRelationRepository.save(
                SpaceAdminRelation(
                    space = space,
                    role = SpaceAdminRole.OWNER,
                    user = userRepository.getReferenceById(ownerId.toInt()),
                )
            )

        spaceClassificationTopicsService.updateClassificationTopics(
            newSpaceId,
            classificationTopics,
        )

        return space.toSpaceDTO(
            options = SpaceQueryOptions.MAXIMUM,
            admins = listOf(ownerRelation.toDTO()), // Only include the owner in the admins list
        )
    }

    private fun getSpace(spaceId: IdType): Space {
        return spaceRepository.findById(spaceId).orElseThrow { NotFoundError("space", spaceId) }
    }

    /**
     * PATCH updates a Space entity with non-null values from the request DTO.
     *
     * NOTE ON HIBERNATE FLUSHING BEHAVIOR: When using the patch service with JPA/Hibernate, be
     * careful with database operations in handlers. Any database query (including validation
     * queries) can trigger Hibernate's flush mechanism, causing partial updates to be committed
     * before the method completes.
     *
     * Example problem:
     * - If you update multiple fields but perform DB queries in handlers
     * - Hibernate may generate multiple UPDATE statements (one per flush)
     * - This is inefficient and can lead to transaction isolation issues
     *
     * Best practice:
     * 1. Perform ALL validations and entity lookups BEFORE starting the patch operation
     * 2. Keep handlers simple - they should only set values, not trigger DB operations
     * 3. Save the entity ONCE after all changes are applied
     *
     * This pattern ensures a single UPDATE statement regardless of how many fields are changed.
     *
     * @param spaceId The ID of the Space to update
     * @param patchDto The DTO containing fields to update
     * @return The updated Space DTO
     */
    @Transactional
    fun patchSpace(spaceId: IdType, patchDto: PatchSpaceRequestDTO): SpaceDTO {
        val space = getSpace(spaceId)

        if (patchDto.name != null && patchDto.name != space.name) {
            ensureSpaceNameNotExists(patchDto.name)
        }

        if (patchDto.classificationTopics != null) {
            for (topic in patchDto.classificationTopics) {
                topicService.ensureTopicExists(topic)
            }
        }

        val newDefaultCategory =
            if (patchDto.defaultCategoryId != null) {
                val newDefaultCategory = getCategoryForSpace(spaceId, patchDto.defaultCategoryId)
                if (newDefaultCategory.isArchived) {
                    throw BadRequestError("Cannot set an archived category as default.")
                }
                newDefaultCategory
            } else {
                space.defaultCategory
            }

        val updatedSpace =
            entityPatcher.patch(space, patchDto) {
                handle(PatchSpaceRequestDTO::avatarId) { entity, value ->
                    entity.avatar = avatarRepository.getReferenceById(value.toInt())
                }

                handle(PatchSpaceRequestDTO::classificationTopics) { _, value ->
                    spaceClassificationTopicsService.updateClassificationTopics(spaceId, value)
                }

                handle(PatchSpaceRequestDTO::defaultCategoryId) { entity, _ ->
                    entity.defaultCategory = newDefaultCategory
                }
            }

        // IMPORTANT: Save the entity ONCE after all changes are applied
        return spaceRepository.save(updatedSpace).toSpaceDTO(options = SpaceQueryOptions.MAXIMUM)
    }

    private fun getCategoryForSpace(spaceId: IdType, categoryId: IdType): SpaceCategory {
        val category =
            spaceCategoryRepository.findByIdAndSpaceId(categoryId, spaceId)
                ?: throw NotFoundError("category", categoryId)

        return category
    }

    fun getCategoryDTO(spaceId: IdType, categoryId: IdType): SpaceCategoryDTO {
        val category = getCategoryForSpace(spaceId, categoryId)
        return category.toDTO()
    }

    fun listCategories(spaceId: IdType, includeArchived: Boolean = false): List<SpaceCategoryDTO> {
        val categories =
            if (includeArchived) {
                spaceCategoryRepository.findBySpaceIdOrderByDisplayOrderAscNameAsc(spaceId)
            } else {
                spaceCategoryRepository
                    .findBySpaceIdAndArchivedAtIsNullOrderByDisplayOrderAscNameAsc(spaceId)
            }
        return categories.map { it.toDTO() }
    }

    fun createCategory(
        spaceId: IdType,
        name: String,
        description: String?,
        displayOrder: Int?,
    ): SpaceCategoryDTO {
        val space = getSpace(spaceId) // Ensures space exists

        if (spaceCategoryRepository.existsBySpaceIdAndNameAndArchivedAtIsNull(spaceId, name)) {
            throw NameAlreadyExistsError("active space category", name)
        }

        val category =
            SpaceCategory(
                name = name,
                space = space,
                description = description,
                displayOrder = displayOrder ?: 0,
                archivedAt = null,
            )
        val savedCategory = spaceCategoryRepository.save(category)
        return savedCategory.toDTO()
    }

    fun updateCategory(
        spaceId: IdType,
        categoryId: IdType,
        patch: UpdateSpaceCategoryRequestDTO,
    ): SpaceCategoryDTO {
        val category = getCategoryForSpace(spaceId, categoryId)

        // Prevent updating archived categories directly
        if (category.isArchived) {
            throw BadRequestError("Cannot update an archived category. Unarchive it first.")
        }

        // Check for name conflict against other *active* categories if name is changing
        if (
            patch.name != null &&
                patch.name != category.name &&
                spaceCategoryRepository.existsBySpaceIdAndNameAndArchivedAtIsNull(
                    spaceId,
                    patch.name,
                )
        ) {
            // Check if the conflict is with itself (if it's active) - this shouldn't happen if
            // exists check excludes self
            val conflictingCategory =
                spaceCategoryRepository.findBySpaceIdAndNameAndArchivedAtIsNull(spaceId, patch.name)
            if (conflictingCategory != null && conflictingCategory.id != categoryId) {
                throw NameAlreadyExistsError("active space category", patch.name)
            }
        }

        val updatedCategory =
            entityPatcher.patch(category, patch).let { spaceCategoryRepository.save(it) }

        return updatedCategory.toDTO()
    }

    fun deleteCategory(spaceId: IdType, categoryId: IdType) {
        val category = getCategoryForSpace(spaceId, categoryId)
        val space = category.space

        // 1. Prevent deleting the default category
        if (space.defaultCategory?.id == categoryId) {
            throw BadRequestError(
                "The default category cannot be deleted. Consider changing the default first."
            )
        }

        // 2. Check if tasks are associated with this category
        if (taskRepository.existsByCategoryId(categoryId)) {
            throw ConflictError(
                "Cannot delete category (id=$categoryId) because it contains tasks. Please archive it instead, or reassign the tasks."
            )
        }

        // 3. Proceed with soft delete if no tasks exist
        category.deletedAt = LocalDateTime.now()
        category.archivedAt = null
        spaceCategoryRepository.save(category)
    }

    fun setDefaultCategory(spaceId: IdType, newDefaultCategoryId: IdType) {
        val space = getSpace(spaceId)
        val newDefaultCategory = getCategoryForSpace(spaceId, newDefaultCategoryId)

        if (newDefaultCategory.isArchived) {
            throw BadRequestError("Cannot set an archived category as default.")
        }

        if (space.defaultCategory?.id != newDefaultCategory.id) {
            space.defaultCategory = newDefaultCategory
            spaceRepository.save(space)
        }
    }

    /**
     * Archives a category. Does not affect associated tasks.
     *
     * @throws BadRequestError if trying to archive the default category.
     */
    fun archiveCategory(spaceId: IdType, categoryId: IdType): SpaceCategoryDTO {
        val category = getCategoryForSpace(spaceId, categoryId)
        val space = category.space

        // 1. Prevent archiving the default category
        if (space.defaultCategory?.id == categoryId) {
            throw BadRequestError("The default category cannot be archived.")
        }

        // 2. Archive if not already archived
        if (!category.isArchived) {
            category.archivedAt = OffsetDateTime.now()
            spaceCategoryRepository.save(category)
        }
        return category.toDTO()
    }

    /** Unarchives a category. */
    fun unarchiveCategory(spaceId: IdType, categoryId: IdType): SpaceCategoryDTO {
        val category = getCategoryForSpace(spaceId, categoryId)

        if (category.isArchived) {
            category.archivedAt = null
            spaceCategoryRepository.save(category)
        }
        return category.toDTO()
    }

    fun deleteSpace(spaceId: IdType) {
        val relations = spaceAdminRelationRepository.findAllBySpaceId(spaceId)
        relations.forEach { it.deletedAt = LocalDateTime.now() }
        val space =
            spaceRepository.findById(spaceId).orElseThrow { NotFoundError("space", spaceId) }
        space.deletedAt = LocalDateTime.now()
        spaceAdminRelationRepository.saveAll(relations)
        spaceRepository.save(space)
    }

    fun ensureNotSpaceAdmin(spaceId: IdType, userId: IdType) {
        if (spaceAdminRelationRepository.existsBySpaceIdAndUserId(spaceId, userId)) {
            throw AlreadyBeSpaceAdminError(spaceId, userId)
        }
    }

    fun addSpaceAdmin(spaceId: IdType, userId: IdType) {
        ensureNotSpaceAdmin(spaceId, userId)
        val relation =
            SpaceAdminRelation(
                space = Space().apply { id = spaceId },
                user = User().apply { id = userId.toInt() },
                role = SpaceAdminRole.ADMIN,
            )
        spaceAdminRelationRepository.save(relation)
    }

    fun ensureNotSpaceOwner(spaceId: IdType, userId: IdType) {
        if (
            spaceAdminRelationRepository.existsBySpaceIdAndUserIdAndRole(
                spaceId,
                userId,
                SpaceAdminRole.OWNER,
            )
        ) {
            throw AlreadyBeSpaceAdminError(spaceId, userId)
        }
    }

    fun shipSpaceOwnership(spaceId: IdType, userId: IdType) {
        ensureNotSpaceOwner(spaceId, userId)
        val oldRelation =
            spaceAdminRelationRepository
                .findBySpaceIdAndRole(spaceId, SpaceAdminRole.OWNER)
                .orElseThrow { NotFoundError("space", spaceId) }
        val newRelation =
            spaceAdminRelationRepository.findBySpaceIdAndUserId(spaceId, userId).orElseThrow {
                NotSpaceAdminYetError(spaceId, userId)
            }
        oldRelation.role = SpaceAdminRole.ADMIN
        newRelation.role = SpaceAdminRole.OWNER
        spaceAdminRelationRepository.save(oldRelation)
        spaceAdminRelationRepository.save(newRelation)
    }

    fun removeSpaceAdmin(spaceId: IdType, userId: IdType) {
        val relation =
            spaceAdminRelationRepository.findBySpaceIdAndUserId(spaceId, userId).orElseThrow {
                NotSpaceAdminYetError(spaceId, userId)
            }
        relation.deletedAt = LocalDateTime.now()
        spaceAdminRelationRepository.save(relation)
    }

    enum class SpacesSortBy {
        CREATED_AT,
        UPDATED_AT,
    }

    fun enumerateSpaces(
        sortBy: SpacesSortBy,
        sortOrder: SortDirection,
        pageSize: Int,
        pageStart: Long?,
        queryOptions: SpaceQueryOptions,
    ): Pair<List<SpaceDTO>, PageDTO> {
        val direction = sortOrder.toJpaDirection()

        val sortProperty =
            when (sortBy) {
                SpacesSortBy.CREATED_AT -> Space::createdAt
                SpacesSortBy.UPDATED_AT -> Space::updatedAt
            }

        val cursorSpec = spaceRepository.idSeekSpec(Space::id, sortProperty, direction).build()

        val result = spaceRepository.findAllWithIdCursor(cursorSpec, pageStart, pageSize)

        return Pair(result.content.map { it.toSpaceDTO(queryOptions) }, result.pageInfo.toPageDTO())
    }
}
