package org.rucca.cheese.space

import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.SpaceDTO
import org.springframework.stereotype.Service

@Service
class SpaceService(
        private val spaceRepository: SpaceRepository,
        private val spaceAdminRelationRepository: SpaceAdminRelationRepository,
) {
    fun getSpaceDto(spaceId: IdType): SpaceDTO {
        val space = spaceRepository.findById(spaceId).orElseThrow { NotFoundError("space", spaceId) }
        return SpaceDTO(
                id = space.id,
                intro = space.description,
                name = space.name,
                avatarId = TODO(),
                admins = TODO(),
        )
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
}
