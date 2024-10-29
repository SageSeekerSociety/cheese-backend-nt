package org.rucca.cheese.space

import javax.annotation.PostConstruct
import org.hibernate.query.SortDirection
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
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            _: Map<String, Any?>?,
            _: IdGetter?,
            _: Any?,
            ->
            spaceService.isSpaceAdmin(
                resourceId ?: throw IllegalArgumentException("resourceId is null"),
                userId
            )
        }
    }

    @Guard("delete", "space")
    override fun deleteSpace(@ResourceId spaceId: Long): ResponseEntity<DeleteSpace200ResponseDTO> {
        spaceService.deleteSpace(spaceId)
        return ResponseEntity.ok(DeleteSpace200ResponseDTO(200, "OK"))
    }

    @Guard("remove-admin", "space")
    override fun deleteSpaceAdmin(
        @ResourceId spaceId: Long,
        userId: Long
    ): ResponseEntity<DeleteSpace200ResponseDTO> {
        spaceService.removeSpaceAdmin(spaceId, userId)
        return ResponseEntity.ok(DeleteSpace200ResponseDTO(200, "OK"))
    }

    @Guard("query", "space")
    override fun getSpace(
        @ResourceId spaceId: Long,
        queryMyRank: Boolean
    ): ResponseEntity<GetSpace200ResponseDTO> {
        val spaceDTO = spaceService.getSpaceDto(spaceId, queryMyRank)
        return ResponseEntity.ok(
            GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDTO), "OK")
        )
    }

    @Guard("enumerate", "space")
    override fun getSpaces(
        queryMyRank: Boolean,
        pageSize: Int?,
        pageStart: Long?,
        sortBy: String,
        sortOrder: String
    ): ResponseEntity<GetSpaces200ResponseDTO> {
        val by =
            when (sortBy) {
                "updatedAt" -> SpaceService.SpacesSortBy.UPDATED_AT
                "createdAt" -> SpaceService.SpacesSortBy.CREATED_AT
                else -> throw IllegalArgumentException("Invalid sortBy: $sortBy")
            }
        val order =
            when (sortOrder) {
                "asc" -> SortDirection.ASCENDING
                "desc" -> SortDirection.DESCENDING
                else -> throw IllegalArgumentException("Invalid sortOrder: $sortOrder")
            }
        val (spaces, page) =
            spaceService.enumerateSpaces(queryMyRank, by, order, pageSize ?: 10, pageStart)
        return ResponseEntity.ok(
            GetSpaces200ResponseDTO(200, GetSpaces200ResponseDataDTO(spaces, page), "OK")
        )
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
            spaceService.updateSpaceIntro(spaceId, patchSpaceRequestDTO.intro)
        }
        if (patchSpaceRequestDTO.description != null) {
            spaceService.updateSpaceDescription(spaceId, patchSpaceRequestDTO.description)
        }
        if (patchSpaceRequestDTO.avatarId != null) {
            spaceService.updateSpaceAvatar(spaceId, patchSpaceRequestDTO.avatarId)
        }
        if (patchSpaceRequestDTO.enableRank != null) {
            spaceService.updateSpaceEnableRank(spaceId, patchSpaceRequestDTO.enableRank)
        }
        if (patchSpaceRequestDTO.announcements != null) {
            spaceService.updateSpaceAnnouncements(spaceId, patchSpaceRequestDTO.announcements)
        }
        if (patchSpaceRequestDTO.taskTemplates != null) {
            spaceService.updateSpaceTaskTemplates(spaceId, patchSpaceRequestDTO.taskTemplates)
        }
        val spaceDTO = spaceService.getSpaceDto(spaceId)
        return ResponseEntity.ok(
            GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDTO), "OK")
        )
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
        return ResponseEntity.ok(
            GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDTO), "OK")
        )
    }

    @Guard("create", "space")
    override fun postSpace(
        postSpaceRequestDTO: PostSpaceRequestDTO
    ): ResponseEntity<GetSpace200ResponseDTO> {
        val spaceId =
            spaceService.createSpace(
                name = postSpaceRequestDTO.name,
                intro = postSpaceRequestDTO.intro,
                description = postSpaceRequestDTO.description,
                avatarId = postSpaceRequestDTO.avatarId,
                ownerId = authenticationService.getCurrentUserId(),
                enableRank = postSpaceRequestDTO.enableRank,
                announcements = postSpaceRequestDTO.announcements,
                taskTemplates = postSpaceRequestDTO.taskTemplates
            )
        val spaceDTO = spaceService.getSpaceDto(spaceId)
        return ResponseEntity.ok(
            GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDTO), "OK")
        )
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
        return ResponseEntity.ok(
            GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDTO), "OK")
        )
    }
}
