package org.rucca.cheese.space

import javax.annotation.PostConstruct
import org.rucca.cheese.api.SpacesApi
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.auth.AuthorizationService
import org.rucca.cheese.auth.AuthorizedAction
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.annotation.ResourceId
import org.rucca.cheese.common.persistent.IdGetter
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class SpaceController(
        private val spaceService: SpaceService,
        private val authorizationService: AuthorizationService,
        private val authenticationService: AuthenticationService,
) : SpacesApi {
    @PostConstruct
    fun initialize() {
        authorizationService.ownerIds.register("space", spaceService::getSpaceOwner)
        authorizationService.customAuthLogics.register("is-space-admin") {
                userId: IdType,
                action: AuthorizedAction,
                resourceType: String,
                resourceId: IdType?,
                resourceOwnerIdGetter: IdGetter?,
                customLogicData: Any?,
            ->
            spaceService.isSpaceAdmin(resourceId ?: throw IllegalArgumentException("resourceId is null"), userId)
        }
    }

    @Guard("delete", "space")
    override fun deleteSpace(@ResourceId spaceId: Long): ResponseEntity<DeleteSpace200ResponseDTO> {
        return super.deleteSpace(spaceId)
    }

    @Guard("remove-admin", "space")
    override fun deleteSpaceAdmin(@ResourceId spaceId: Long, user: Long): ResponseEntity<DeleteSpace200ResponseDTO> {
        return super.deleteSpaceAdmin(spaceId, user)
    }

    @Guard("query", "space")
    override fun getSpace(@ResourceId spaceId: Long): ResponseEntity<GetSpace200ResponseDTO> {
        val spaceDTO = spaceService.getSpaceDto(spaceId)
        return ResponseEntity.ok(GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDTO), "OK"))
    }

    @Guard("enumerate", "space")
    override fun getSpaces(
            pageSize: Long?,
            pageStart: Int?,
            sortBy: String,
            sortOrder: String
    ): ResponseEntity<GetSpaces200ResponseDTO> {
        return super.getSpaces(pageSize, pageStart, sortBy, sortOrder)
    }

    @Guard("modify", "space")
    override fun patchSpace(
            @ResourceId spaceId: Long,
            patchSpaceRequestDTO: PatchSpaceRequestDTO
    ): ResponseEntity<GetSpace200ResponseDTO> {
        return super.patchSpace(spaceId, patchSpaceRequestDTO)
    }

    @Guard("modify-admin", "space")
    override fun patchSpaceAdmin(
            @ResourceId spaceId: Long,
            user: Long,
            patchSpaceAdminRequestDTO: PatchSpaceAdminRequestDTO?
    ): ResponseEntity<GetSpace200ResponseDTO> {
        return super.patchSpaceAdmin(spaceId, user, patchSpaceAdminRequestDTO)
    }

    @Guard("create", "space")
    override fun postSpace(postSpaceRequestDTO: PostSpaceRequestDTO): ResponseEntity<GetSpace200ResponseDTO> {
        val spaceId =
                spaceService.createSpace(
                        postSpaceRequestDTO.name,
                        postSpaceRequestDTO.intro,
                        postSpaceRequestDTO.avatarId,
                        authenticationService.getCurrentUserId())
        val spaceDTO = spaceService.getSpaceDto(spaceId)
        return ResponseEntity.ok(GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDTO), "OK"))
    }

    @Guard("add-admin", "space")
    override fun postSpaceAdmin(
            @ResourceId spaceId: Long,
            postSpaceAdminRequestDTO: PostSpaceAdminRequestDTO?
    ): ResponseEntity<GetSpace200ResponseDTO> {
        return super.postSpaceAdmin(spaceId, postSpaceAdminRequestDTO)
    }
}
