package org.rucca.cheese.space

import org.rucca.cheese.common.error.NameAlreadyExistsError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.SpaceAdminDTO
import org.rucca.cheese.model.SpaceAdminRoleTypeDTO
import org.rucca.cheese.model.SpaceDTO
import org.rucca.cheese.user.Avatar
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserService
import org.springframework.stereotype.Service

@Service
class SpaceService(
        private val spaceRepository: SpaceRepository,
        private val spaceAdminRelationRepository: SpaceAdminRelationRepository,
        private val userService: UserService,
) {
    fun getSpaceDto(spaceId: IdType): SpaceDTO {
        val space = spaceRepository.findById(spaceId).orElseThrow { NotFoundError("space", spaceId) }
        val admins = spaceAdminRelationRepository.findAllBySpaceId(spaceId)
        return SpaceDTO(
                id = space.id,
                intro = space.description,
                name = space.name,
                avatarId = space.avatar.id!!.toLong(),
                admins =
                        admins.map {
                            SpaceAdminDTO(convertAdminRole(it.role), userService.getUserDto(it.user.id!!.toLong()))
                        })
    }

    fun getSpaceOwner(spaceId: IdType): IdType {
        val spaceAdminRelation =
                spaceAdminRelationRepository.findBySpaceIdAndRole(spaceId, SpaceAdminRole.OWNER).orElseThrow {
                    NotFoundError("space", spaceId)
                }
        return spaceAdminRelation.user.id!!.toLong()
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
                                avatar = Avatar().apply { id = avatarId.toInt() }))
        spaceAdminRelationRepository.save(
                SpaceAdminRelation(
                        space = space, role = SpaceAdminRole.OWNER, user = User().apply { id = ownerId.toInt() }))
        return space.id
    }
}
