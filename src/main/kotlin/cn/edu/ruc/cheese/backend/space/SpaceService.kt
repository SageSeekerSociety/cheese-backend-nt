package cn.edu.ruc.cheese.backend.space

import cn.edu.ruc.cheese.backend.common.IdType
import cn.edu.ruc.cheese.backend.common.NotFoundError
import cn.edu.ruc.cheese.backend.model.SpaceDTO
import org.springframework.stereotype.Service

@Service
class SpaceService(private val spaceRepository: SpaceRepository) {
    fun getSpaceDto(spaceId: IdType): SpaceDTO {
        val space = spaceRepository.findById(spaceId).orElseThrow { NotFoundError("space", spaceId) }
        return SpaceDTO(id = space.id, intro = space.description, name = space.name)
    }
}
