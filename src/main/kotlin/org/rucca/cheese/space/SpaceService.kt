package org.rucca.cheese.space

import jakarta.persistence.EntityManager
import java.time.LocalDateTime
import org.hibernate.query.SortDirection
import org.rucca.cheese.common.error.NameAlreadyExistsError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.PageDTO
import org.rucca.cheese.model.SpaceAdminDTO
import org.rucca.cheese.model.SpaceAdminRoleTypeDTO
import org.rucca.cheese.model.SpaceDTO
import org.rucca.cheese.space.error.AlreadyBeSpaceAdminError
import org.rucca.cheese.space.error.NotSpaceAdminYetError
import org.rucca.cheese.user.Avatar
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
    private val entityManager: EntityManager,
) {
    fun getSpaceDto(spaceId: IdType): SpaceDTO {
        val space =
            spaceRepository.findById(spaceId).orElseThrow { NotFoundError("space", spaceId) }
        val admins = spaceAdminRelationRepository.findAllBySpaceId(spaceId)
        return SpaceDTO(
            id = space.id!!,
            intro = space.description!!,
            name = space.name!!,
            avatarId = space.avatar!!.id!!.toLong(),
            admins =
                admins.map {
                    SpaceAdminDTO(
                        convertAdminRole(it.role!!),
                        userService.getUserDto(it.user!!.id!!.toLong()),
                        createdAt = it.createdAt!!.toEpochMilli(),
                        updatedAt = it.updatedAt!!.toEpochMilli()
                    )
                },
            updatedAt = space.updatedAt!!.toEpochMilli(),
            createdAt = space.createdAt!!.toEpochMilli()
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

    fun createSpace(name: String, description: String, avatarId: IdType, ownerId: IdType): IdType {
        ensureSpaceNameNotExists(name)
        val space =
            spaceRepository.save(
                Space(
                    name = name,
                    description = description,
                    avatar = Avatar().apply { id = avatarId.toInt() }
                )
            )
        spaceAdminRelationRepository.save(
            SpaceAdminRelation(
                space = space,
                role = SpaceAdminRole.OWNER,
                user = User().apply { id = ownerId.toInt() }
            )
        )
        return space.id!!
    }

    fun updateSpaceName(spaceId: IdType, name: String) {
        ensureSpaceNameNotExists(name)
        val space =
            spaceRepository.findById(spaceId).orElseThrow { NotFoundError("space", spaceId) }
        space.name = name
        spaceRepository.save(space)
    }

    fun updateSpaceDescription(spaceId: IdType, description: String) {
        val space =
            spaceRepository.findById(spaceId).orElseThrow { NotFoundError("space", spaceId) }
        space.description = description
        spaceRepository.save(space)
    }

    fun updateSpaceAvatar(spaceId: IdType, avatarId: IdType) {
        val space =
            spaceRepository.findById(spaceId).orElseThrow { NotFoundError("space", spaceId) }
        space.avatar = Avatar().apply { id = avatarId.toInt() }
        spaceRepository.save(space)
    }

    fun ensureSpaceExists(spaceId: IdType) {
        if (!spaceRepository.existsById(spaceId)) {
            throw NotFoundError("space", spaceId)
        }
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
                role = SpaceAdminRole.ADMIN
            )
        spaceAdminRelationRepository.save(relation)
    }

    fun ensureNotSpaceOwner(spaceId: IdType, userId: IdType) {
        if (
            spaceAdminRelationRepository.existsBySpaceIdAndUserIdAndRole(
                spaceId,
                userId,
                SpaceAdminRole.OWNER
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
    ): Pair<List<SpaceDTO>, PageDTO> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val cq = criteriaBuilder.createQuery(Space::class.java)
        val root = cq.from(Space::class.java)
        val by =
            when (sortBy) {
                SpacesSortBy.CREATED_AT -> root.get<LocalDateTime>("createdAt")
                SpacesSortBy.UPDATED_AT -> root.get<LocalDateTime>("updatedAt")
            }
        val order =
            when (sortOrder) {
                SortDirection.ASCENDING -> criteriaBuilder.asc(by)
                SortDirection.DESCENDING -> criteriaBuilder.desc(by)
            }
        cq.orderBy(order)
        val query = entityManager.createQuery(cq)
        val result = query.resultList
        val (curr, page) =
            PageHelper.pageFromAll(
                result,
                pageStart,
                pageSize,
                { it.id!! },
                { id -> throw NotFoundError("space", id) }
            )
        return Pair(
            curr.map {
                SpaceDTO(
                    id = it.id!!,
                    intro = it.description!!,
                    name = it.name!!,
                    avatarId = it.avatar!!.id!!.toLong(),
                    admins =
                        spaceAdminRelationRepository.findAllBySpaceId(it.id!!).map {
                            SpaceAdminDTO(
                                convertAdminRole(it.role!!),
                                userService.getUserDto(it.user!!.id!!.toLong()),
                                createdAt = it.createdAt!!.toEpochMilli(),
                                updatedAt = it.updatedAt!!.toEpochMilli()
                            )
                        },
                    updatedAt = it.updatedAt!!.toEpochMilli(),
                    createdAt = it.createdAt!!.toEpochMilli()
                )
            },
            page
        )
    }
}
