/*
 *  Description: This file defines the SpaceController class.
 *               It provides endpoints of /spaces.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.space

import jakarta.annotation.PostConstruct
import java.net.URI
import org.hibernate.query.SortDirection
import org.rucca.cheese.api.SpacesApi
import org.rucca.cheese.auth.AuthorizationService
import org.rucca.cheese.auth.AuthorizedAction
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.annotation.ResourceId
import org.rucca.cheese.auth.spring.UseOldAuth
import org.rucca.cheese.common.persistent.IdGetter
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.space.option.SpaceQueryOptions
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@UseOldAuth
class SpaceController(
    private val spaceService: SpaceService,
    private val authorizationService: AuthorizationService,
    private val jwtService: JwtService,
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
            _: Any? ->
            spaceService.isSpaceAdmin(
                resourceId ?: throw IllegalArgumentException("resourceId is null"),
                userId,
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
        userId: Long,
    ): ResponseEntity<DeleteSpace200ResponseDTO> {
        spaceService.removeSpaceAdmin(spaceId, userId)
        return ResponseEntity.ok(DeleteSpace200ResponseDTO(200, "OK"))
    }

    @Guard("query", "space")
    override fun getSpace(
        @ResourceId spaceId: Long,
        queryMyRank: Boolean,
    ): ResponseEntity<GetSpace200ResponseDTO> {
        val queryOptions = SpaceQueryOptions(queryMyRank = queryMyRank)
        val spaceDTO = spaceService.getSpaceDto(spaceId, queryOptions)
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
        sortOrder: String,
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
        val queryOptions = SpaceQueryOptions(queryMyRank = queryMyRank)
        val (spaces, page) =
            spaceService.enumerateSpaces(by, order, pageSize ?: 10, pageStart, queryOptions)
        return ResponseEntity.ok(
            GetSpaces200ResponseDTO(200, GetSpaces200ResponseDataDTO(spaces, page), "OK")
        )
    }

    @Guard("modify", "space")
    override fun patchSpace(
        @ResourceId spaceId: Long,
        patchSpaceRequestDTO: PatchSpaceRequestDTO,
    ): ResponseEntity<GetSpace200ResponseDTO> {
        val spaceDTO = spaceService.patchSpace(spaceId, patchSpaceRequestDTO)
        return ResponseEntity.ok(
            GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDTO), "OK")
        )
    }

    @Guard("modify-admin", "space")
    override fun patchSpaceAdmin(
        @ResourceId spaceId: Long,
        userId: Long,
        patchSpaceAdminRequestDTO: PatchSpaceAdminRequestDTO,
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
        val spaceDTO =
            spaceService.createSpace(
                name = postSpaceRequestDTO.name,
                intro = postSpaceRequestDTO.intro,
                description = postSpaceRequestDTO.description,
                avatarId = postSpaceRequestDTO.avatarId,
                ownerId = jwtService.getCurrentUserId(),
                enableRank = postSpaceRequestDTO.enableRank ?: false,
                announcements =
                    postSpaceRequestDTO.announcements.takeUnless { it.isEmpty() } ?: "[]",
                taskTemplates =
                    postSpaceRequestDTO.taskTemplates.takeUnless { it.isEmpty() } ?: "[]",
                classificationTopics = postSpaceRequestDTO.classificationTopics ?: emptyList(),
            )
        return ResponseEntity.ok(
            GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDTO), "OK")
        )
    }

    @Guard("add-admin", "space")
    override fun postSpaceAdmin(
        @ResourceId spaceId: Long,
        postSpaceAdminRequestDTO: PostSpaceAdminRequestDTO,
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

    @Guard("query", "space")
    override fun listSpaceCategories(
        @ResourceId spaceId: Long,
        includeArchived: Boolean,
    ): ResponseEntity<ListSpaceCategories200ResponseDTO> {
        val categories = spaceService.listCategories(spaceId, includeArchived)
        return ResponseEntity.ok(
            ListSpaceCategories200ResponseDTO(
                200,
                ListSpaceCategories200ResponseDataDTO(categories),
                "OK",
            )
        )
    }

    @Guard("modify", "space")
    override fun createSpaceCategory(
        @ResourceId spaceId: Long,
        createSpaceCategoryRequestDTO: CreateSpaceCategoryRequestDTO,
    ): ResponseEntity<CreateSpaceCategory201ResponseDTO> {
        val createdCategory =
            spaceService.createCategory(
                spaceId = spaceId,
                name = createSpaceCategoryRequestDTO.name,
                description = createSpaceCategoryRequestDTO.description,
                displayOrder = createSpaceCategoryRequestDTO.displayOrder,
            )
        return ResponseEntity.created(
                URI.create("/spaces/$spaceId/categories/${createdCategory.id}")
            )
            .body(
                CreateSpaceCategory201ResponseDTO(
                    201,
                    CreateSpaceCategory201ResponseDataDTO(createdCategory),
                    "Created",
                )
            )
    }

    @Guard("query", "space")
    override fun getSpaceCategory(
        @ResourceId spaceId: Long,
        @PathVariable("categoryId") categoryId: Long,
    ): ResponseEntity<CreateSpaceCategory201ResponseDTO> {
        val category = spaceService.getCategoryDTO(spaceId, categoryId)
        return ResponseEntity.ok(
            CreateSpaceCategory201ResponseDTO(
                200,
                CreateSpaceCategory201ResponseDataDTO(category),
                "OK",
            )
        )
    }

    @Guard("modify", "space")
    override fun updateSpaceCategory(
        @ResourceId spaceId: Long,
        @PathVariable("categoryId") categoryId: Long,
        updateSpaceCategoryRequestDTO: UpdateSpaceCategoryRequestDTO,
    ): ResponseEntity<CreateSpaceCategory201ResponseDTO> {
        val updatedCategory =
            spaceService.updateCategory(
                spaceId = spaceId,
                categoryId = categoryId,
                patch = updateSpaceCategoryRequestDTO,
            )
        return ResponseEntity.ok(
            CreateSpaceCategory201ResponseDTO(
                200,
                CreateSpaceCategory201ResponseDataDTO(updatedCategory),
                "OK",
            )
        )
    }

    @Guard("modify", "space")
    override fun deleteSpaceCategory(
        @ResourceId spaceId: Long,
        @PathVariable("categoryId") categoryId: Long,
    ): ResponseEntity<Unit> {
        spaceService.deleteCategory(spaceId, categoryId)
        return ResponseEntity.noContent().build()
    }

    @Guard("modify", "space")
    override fun archiveSpaceCategory(
        @ResourceId spaceId: Long,
        categoryId: Long,
    ): ResponseEntity<CreateSpaceCategory201ResponseDTO> {
        val archivedCategory = spaceService.archiveCategory(spaceId, categoryId)
        return ResponseEntity.ok(
            CreateSpaceCategory201ResponseDTO(
                200,
                CreateSpaceCategory201ResponseDataDTO(archivedCategory),
                "OK",
            )
        )
    }

    @Guard("modify", "space")
    override fun unarchiveSpaceCategory(
        @ResourceId spaceId: Long,
        @PathVariable("categoryId") categoryId: Long,
    ): ResponseEntity<CreateSpaceCategory201ResponseDTO> {
        val unarchivedCategory = spaceService.unarchiveCategory(spaceId, categoryId)
        return ResponseEntity.ok(
            CreateSpaceCategory201ResponseDTO(
                200,
                CreateSpaceCategory201ResponseDataDTO(unarchivedCategory),
                "OK",
            )
        )
    }
}
