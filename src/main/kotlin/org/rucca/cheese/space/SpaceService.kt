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
import org.hibernate.query.SortDirection
import org.rucca.cheese.auth.AuthenticationService
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
import org.rucca.cheese.space.option.SpaceQueryOptions
import org.rucca.cheese.topic.TopicService
import org.rucca.cheese.user.Avatar
import org.rucca.cheese.user.AvatarRepository
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SpaceService(
    private val spaceRepository: SpaceRepository,
    private val spaceAdminRelationRepository: SpaceAdminRelationRepository,
    private val userService: UserService,
    private val spaceUserRankService: SpaceUserRankService,
    private val authenticationService: AuthenticationService,
    private val topicService: TopicService,
    private val spaceClassificationTopicsService: SpaceClassificationTopicsService,
    private val avatarRepository: AvatarRepository,
    private val entityPatcher: EntityPatcher,
) {
    fun Space.toSpaceDTO(
        options: SpaceQueryOptions,
        admins: List<SpaceAdminDTO>? = null,
        topics: List<TopicDTO>? = null,
        rank: Int? = null,
    ): SpaceDTO {
        val myRank =
            rank
                ?: if (options.queryMyRank && this.enableRank!!) {
                    val userId = authenticationService.getCurrentUserId()
                    spaceUserRankService.getRank(this.id!!, userId)
                } else null

        val classificationTopics =
            topics ?: spaceClassificationTopicsService.getClassificationTopicDTOs(this.id!!)

        val adminDTOs =
            admins
                ?: spaceAdminRelationRepository.findAllBySpaceId(this.id!!).map {
                    SpaceAdminDTO(
                        convertAdminRole(it.role!!),
                        userService.getUserDto(it.user!!.id!!.toLong()),
                        createdAt = it.createdAt!!.toEpochMilli(),
                        updatedAt = it.updatedAt!!.toEpochMilli(),
                    )
                }

        return SpaceDTO(
            id = this.id!!,
            intro = this.intro!!,
            description = this.description!!,
            name = this.name!!,
            avatarId = this.avatar!!.id!!.toLong(),
            admins = adminDTOs,
            updatedAt = this.updatedAt!!.toEpochMilli(),
            createdAt = this.createdAt!!.toEpochMilli(),
            enableRank = this.enableRank!!,
            announcements = this.announcements!!,
            taskTemplates = this.taskTemplates!!,
            myRank = myRank,
            classificationTopics = classificationTopics,
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
                val userId = authenticationService.getCurrentUserId()
                spaceUserRankService.getRank(spaceId, userId)
            } else null

        // 批量获取所有管理员的用户信息
        val adminUsers = admins.mapNotNull { it.user }
        val userDtosMap = userService.convertUsersToDto(adminUsers)

        return SpaceDTO(
            id = space.id!!,
            intro = space.intro!!,
            description = space.description!!,
            name = space.name!!,
            avatarId = space.avatar!!.id!!.toLong(),
            admins =
                admins.map { admin ->
                    val userId = admin.user!!.id!!.toLong()
                    val userDto = userDtosMap[userId] ?: userService.getUserDto(userId)

                    SpaceAdminDTO(
                        convertAdminRole(admin.role!!),
                        userDto,
                        createdAt = admin.createdAt!!.toEpochMilli(),
                        updatedAt = admin.updatedAt!!.toEpochMilli(),
                    )
                },
            updatedAt = space.updatedAt!!.toEpochMilli(),
            createdAt = space.createdAt!!.toEpochMilli(),
            enableRank = space.enableRank!!,
            announcements = space.announcements!!,
            taskTemplates = space.taskTemplates!!,
            myRank = myRank,
            classificationTopics = classificationTopics,
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
    ): IdType {
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
        spaceAdminRelationRepository.save(
            SpaceAdminRelation(
                space = space,
                role = SpaceAdminRole.OWNER,
                user = User().apply { id = ownerId.toInt() },
            )
        )
        spaceClassificationTopicsService.updateClassificationTopics(
            space.id!!,
            classificationTopics,
        )
        return space.id!!
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
     * @return The updated Space entity
     */
    @Transactional
    fun patchSpace(spaceId: IdType, patchDto: PatchSpaceRequestDTO): Space {
        val space = getSpace(spaceId)

        if (patchDto.name != null && patchDto.name != space.name) {
            ensureSpaceNameNotExists(patchDto.name)
        }

        if (patchDto.classificationTopics != null) {
            for (topic in patchDto.classificationTopics) {
                topicService.ensureTopicExists(topic)
            }
        }

        val updatedSpace =
            entityPatcher.patch(space, patchDto) {
                handle(PatchSpaceRequestDTO::avatarId) { entity, value ->
                    entity.avatar = avatarRepository.getReferenceById(value.toInt())
                }

                handle(PatchSpaceRequestDTO::classificationTopics) { _, value ->
                    // Only perform association updates, no validation
                    spaceClassificationTopicsService.updateClassificationTopics(spaceId, value)
                }
            }

        // IMPORTANT: Save the entity ONCE after all changes are applied
        return spaceRepository.save(updatedSpace)
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
