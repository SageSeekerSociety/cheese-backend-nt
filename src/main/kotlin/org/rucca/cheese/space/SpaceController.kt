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
        spaceService.deleteSpace(spaceId)
        return ResponseEntity.ok(DeleteSpace200ResponseDTO(200, "OK"))
    }

    @Guard("remove-admin", "space")
    override fun deleteSpaceAdmin(@ResourceId spaceId: Long, userId: Long): ResponseEntity<DeleteSpace200ResponseDTO> {
        spaceService.removeSpaceAdmin(spaceId, userId)
        return ResponseEntity.ok(DeleteSpace200ResponseDTO(200, "OK"))
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
        if (patchSpaceRequestDTO.name != null) {
            spaceService.updateSpaceName(spaceId, patchSpaceRequestDTO.name)
        }
        if (patchSpaceRequestDTO.intro != null) {
            spaceService.updateSpaceDescription(spaceId, patchSpaceRequestDTO.intro)
        }
        if (patchSpaceRequestDTO.avatarId != null) {
            spaceService.updateSpaceAvatar(spaceId, patchSpaceRequestDTO.avatarId)
        }
        val spaceDTO = spaceService.getSpaceDto(spaceId)
        return ResponseEntity.ok(GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDTO), "OK"))
    }

    @Guard("modify-admin", "space")
    override fun patchSpaceAdmin(
            @ResourceId spaceId: Long,
            userId: Long,
            patchSpaceAdminRequestDTO: PatchSpaceAdminRequestDTO
    ): ResponseEntity<GetSpace200ResponseDTO> {
        if (patchSpaceAdminRequestDTO.role != null) {
            when (patchSpaceAdminRequestDTO.role) {
                SpaceAdminRoleTypeDTO.OWNER -> {
                    authorizationService.audit("ship-ownership", "space", spaceId)
                    spaceService.shipSpaceOwnership(spaceId, userId)
                }
                SpaceAdminRoleTypeDTO.ADMIN -> {
                    /* do nothing */
                }
            }
        }
        val spaceDTO = spaceService.getSpaceDto(spaceId)
        return ResponseEntity.ok(GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDTO), "OK"))
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
            postSpaceAdminRequestDTO: PostSpaceAdminRequestDTO
    ): ResponseEntity<GetSpace200ResponseDTO> {
        spaceService.addSpaceAdmin(spaceId, postSpaceAdminRequestDTO.userId)
        when (postSpaceAdminRequestDTO.role) {
            SpaceAdminRoleTypeDTO.OWNER -> {
                authorizationService.audit("ship-ownership", "space", spaceId)
                spaceService.shipSpaceOwnership(spaceId, postSpaceAdminRequestDTO.userId)
            }
            SpaceAdminRoleTypeDTO.ADMIN -> {
                /* do nothing */
            }
        }
        val spaceDTO = spaceService.getSpaceDto(spaceId)
        return ResponseEntity.ok(GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDTO), "OK"))
    }
}
