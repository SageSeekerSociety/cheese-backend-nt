package org.rucca.cheese.space

import org.rucca.cheese.api.SpaceApi
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class SpaceController(private val spaceService: SpaceService) : SpaceApi {
    override fun deleteSpace(space: Long): ResponseEntity<DeleteSpace200ResponseDTO> {
        return super.deleteSpace(space)
    }

    override fun deleteSpaceAdmin(space: Long, user: Long): ResponseEntity<DeleteSpace200ResponseDTO> {
        return super.deleteSpaceAdmin(space, user)
    }

    override fun getSpace(space: Long): ResponseEntity<GetSpace200ResponseDTO> {
        val spaceDto = spaceService.getSpaceDto(space)
        return ResponseEntity.ok(GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDto), "OK"))
    }

    override fun patchSpace(
            space: Long,
            patchSpaceRequestDTO: PatchSpaceRequestDTO
    ): ResponseEntity<GetSpace200ResponseDTO> {
        return super.patchSpace(space, patchSpaceRequestDTO)
    }

    override fun patchSpaceAdmin(
            space: Long,
            user: Long,
            patchSpaceAdminRequestDTO: PatchSpaceAdminRequestDTO?
    ): ResponseEntity<GetSpace200ResponseDTO> {
        return super.patchSpaceAdmin(space, user, patchSpaceAdminRequestDTO)
    }

    override fun postSpace(postSpaceRequestDTO: PostSpaceRequestDTO): ResponseEntity<GetSpace200ResponseDTO> {
        return super.postSpace(postSpaceRequestDTO)
    }

    override fun postSpaceAdmin(
            space: Long,
            postSpaceAdminRequestDTO: PostSpaceAdminRequestDTO?
    ): ResponseEntity<GetSpace200ResponseDTO> {
        return super.postSpaceAdmin(space, postSpaceAdminRequestDTO)
    }
}
