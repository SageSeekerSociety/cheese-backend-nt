package org.rucca.cheese.space

import org.rucca.cheese.api.SpacesApi
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class SpaceController(private val spaceService: SpaceService) : SpacesApi {
    override fun deleteSpace(spaceId: Long): ResponseEntity<DeleteSpace200ResponseDTO> {
        return super.deleteSpace(spaceId)
    }

    override fun deleteSpaceAdmin(spaceId: Long, user: Long): ResponseEntity<DeleteSpace200ResponseDTO> {
        return super.deleteSpaceAdmin(spaceId, user)
    }

    override fun getSpace(spaceId: Long): ResponseEntity<GetSpace200ResponseDTO> {
        val spaceDto = spaceService.getSpaceDto(spaceId)
        return ResponseEntity.ok(GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDto), "OK"))
    }

    override fun getSpaces(
            pageSize: Long?,
            pageStart: Int?,
            sortBy: String,
            sortOrder: String
    ): ResponseEntity<GetSpaces200ResponseDTO> {
        return super.getSpaces(pageSize, pageStart, sortBy, sortOrder)
    }

    override fun patchSpace(
            spaceId: Long,
            patchSpaceRequestDTO: PatchSpaceRequestDTO
    ): ResponseEntity<GetSpace200ResponseDTO> {
        return super.patchSpace(spaceId, patchSpaceRequestDTO)
    }

    override fun patchSpaceAdmin(
            spaceId: Long,
            user: Long,
            patchSpaceAdminRequestDTO: PatchSpaceAdminRequestDTO?
    ): ResponseEntity<GetSpace200ResponseDTO> {
        return super.patchSpaceAdmin(spaceId, user, patchSpaceAdminRequestDTO)
    }

    override fun postSpace(postSpaceRequestDTO: PostSpaceRequestDTO): ResponseEntity<GetSpace200ResponseDTO> {
        return super.postSpace(postSpaceRequestDTO)
    }

    override fun postSpaceAdmin(
            spaceId: Long,
            postSpaceAdminRequestDTO: PostSpaceAdminRequestDTO?
    ): ResponseEntity<GetSpace200ResponseDTO> {
        return super.postSpaceAdmin(spaceId, postSpaceAdminRequestDTO)
    }
}
