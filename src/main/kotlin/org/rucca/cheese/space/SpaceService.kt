package org.rucca.cheese.space

import org.rucca.cheese.common.IdType
import org.rucca.cheese.model.SpaceDTO
import org.springframework.stereotype.Service

@Service
class SpaceService(private val spaceRepository: SpaceRepository) {
    fun getSpaceDto(spaceId: IdType): SpaceDTO {
        val space =
                spaceRepository.findById(spaceId).orElseThrow {
                    org.rucca.cheese.common.NotFoundError("space", spaceId)
                }
        return SpaceDTO(
                id = space.id,
                intro = space.description,
                name = space.name,
                avatarId = TODO(),
                admins = TODO(),
        )
    }
}
