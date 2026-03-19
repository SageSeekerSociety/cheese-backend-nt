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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
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
import org.rucca.cheese.space.analytics.SpaceAnalyticsService
import org.rucca.cheese.space.option.SpaceQueryOptions
import org.rucca.cheese.space.view.SpaceParticipantViewService
import org.rucca.cheese.space.view.SpacePublisherViewService
import org.rucca.cheese.task.service.TaskTopicsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
@UseOldAuth
class SpaceController(
    private val spaceService: SpaceService,
    private val authorizationService: AuthorizationService,
    private val jwtService: JwtService,
    private val spaceAnalyticsService: SpaceAnalyticsService,
    private val spacePublisherViewService: SpacePublisherViewService,
    private val spaceParticipantViewService: SpaceParticipantViewService,
    private val taskTopicsService: TaskTopicsService,
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
    override suspend fun deleteSpace(@ResourceId spaceId: Long): ResponseEntity<Unit> {
        withContext(Dispatchers.IO) { spaceService.deleteSpace(spaceId) }
        return ResponseEntity.noContent().build()
    }

    @Guard("remove-admin", "space")
    override suspend fun deleteSpaceAdmin(
        @ResourceId spaceId: Long,
        userId: Long,
    ): ResponseEntity<Unit> {
        withContext(Dispatchers.IO) { spaceService.removeSpaceAdmin(spaceId, userId) }
        return ResponseEntity.noContent().build()
    }

    @Guard("query", "space")
    override suspend fun getSpace(
        @ResourceId spaceId: Long,
        queryMyRank: Boolean,
        queryCategories: Boolean,
    ): ResponseEntity<GetSpace200ResponseDTO> {
        val currentUserId = jwtService.getCurrentUserId()
        val queryOptions =
            SpaceQueryOptions(queryMyRank = queryMyRank, queryCategories = queryCategories)
        val spaceDTO =
            withContext(Dispatchers.IO) {
                spaceService.getSpaceDto(spaceId, queryOptions, currentUserId)
            }
        val categories =
            if (queryCategories) {
                withContext(Dispatchers.IO) {
                    spaceService.listCategories(spaceId, includeArchived = false)
                }
            } else {
                null
            }
        return ResponseEntity.ok(
            GetSpace200ResponseDTO(200, GetSpace200ResponseDataDTO(spaceDTO, categories), "OK")
        )
    }

    @Guard("enumerate", "space")
    override suspend fun getSpaces(
        queryMyRank: Boolean,
        pageStart: Long?,
        pageSize: Int,
        sortBy: String,
        sortOrder: String,
    ): ResponseEntity<GetSpaces200ResponseDTO> {
        val currentUserId = jwtService.getCurrentUserId()

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
            withContext(Dispatchers.IO) {
                spaceService.enumerateSpaces(
                    currentUserId,
                    by,
                    order,
                    pageSize ?: 10,
                    pageStart,
                    queryOptions,
                )
            }
        return ResponseEntity.ok(
            GetSpaces200ResponseDTO(200, GetSpaces200ResponseDataDTO(spaces, page), "OK")
        )
    }

    @Guard("modify", "space")
    override suspend fun patchSpace(
        @ResourceId spaceId: Long,
        patchSpaceRequestDTO: PatchSpaceRequestDTO,
    ): ResponseEntity<PatchSpace200ResponseDTO> {
        val currentUserId = jwtService.getCurrentUserId()

        val spaceDTO =
            withContext(Dispatchers.IO) {
                spaceService.patchSpace(currentUserId, spaceId, patchSpaceRequestDTO)
            }
        return ResponseEntity.ok(
            PatchSpace200ResponseDTO(200, PatchSpace200ResponseDataDTO(spaceDTO), "OK")
        )
    }

    @Guard("modify-admin", "space")
    override suspend fun patchSpaceAdmin(
        @ResourceId spaceId: Long,
        userId: Long,
        patchSpaceAdminRequestDTO: PatchSpaceAdminRequestDTO,
    ): ResponseEntity<PatchSpace200ResponseDTO> {
        val currentUserId = jwtService.getCurrentUserId()
        val token = jwtService.getToken()
        if (patchSpaceAdminRequestDTO.role != null) {
            when (patchSpaceAdminRequestDTO.role) {
                SpaceAdminRoleTypeDTO.OWNER -> {
                    authorizationService.audit(token, "ship-ownership", "space", spaceId)
                    withContext(Dispatchers.IO) { spaceService.shipSpaceOwnership(spaceId, userId) }
                }
                SpaceAdminRoleTypeDTO.ADMIN -> {
                    /* do nothing */
                }
            }
        }
        val spaceDTO =
            withContext(Dispatchers.IO) {
                spaceService.getSpaceDto(spaceId, currentUserId = currentUserId)
            }
        return ResponseEntity.ok(
            PatchSpace200ResponseDTO(200, PatchSpace200ResponseDataDTO(spaceDTO), "OK")
        )
    }

    @Guard("create", "space")
    override suspend fun postSpace(
        postSpaceRequestDTO: PostSpaceRequestDTO
    ): ResponseEntity<PatchSpace200ResponseDTO> {
        val currentUserId = jwtService.getCurrentUserId()
        val spaceDTO =
            withContext(Dispatchers.IO) {
                spaceService.createSpace(
                    name = postSpaceRequestDTO.name,
                    intro = postSpaceRequestDTO.intro,
                    description = postSpaceRequestDTO.description,
                    avatarId = postSpaceRequestDTO.avatarId,
                    ownerId = currentUserId,
                    enableRank = postSpaceRequestDTO.enableRank ?: false,
                    announcements =
                        postSpaceRequestDTO.announcements.takeUnless { it.isEmpty() } ?: "[]",
                    taskTemplates =
                        postSpaceRequestDTO.taskTemplates.takeUnless { it.isEmpty() } ?: "[]",
                    classificationTopics = postSpaceRequestDTO.classificationTopics ?: emptyList(),
                )
            }
        return ResponseEntity.ok(
            PatchSpace200ResponseDTO(200, PatchSpace200ResponseDataDTO(spaceDTO), "OK")
        )
    }

    @Guard("add-admin", "space")
    override suspend fun postSpaceAdmin(
        @ResourceId spaceId: Long,
        postSpaceAdminRequestDTO: PostSpaceAdminRequestDTO,
    ): ResponseEntity<PatchSpace200ResponseDTO> {
        val currentUserId = jwtService.getCurrentUserId()
        val token = jwtService.getToken()
        withContext(Dispatchers.IO) {
            spaceService.addSpaceAdmin(spaceId, postSpaceAdminRequestDTO.userId)
        }
        when (postSpaceAdminRequestDTO.role) {
            SpaceAdminRoleTypeDTO.OWNER -> {
                authorizationService.audit(token, "ship-ownership", "space", spaceId)
                withContext(Dispatchers.IO) {
                    spaceService.shipSpaceOwnership(spaceId, postSpaceAdminRequestDTO.userId)
                }
            }
            SpaceAdminRoleTypeDTO.ADMIN -> {
                /* do nothing */
            }
        }
        val spaceDTO =
            withContext(Dispatchers.IO) {
                spaceService.getSpaceDto(spaceId, currentUserId = currentUserId)
            }
        return ResponseEntity.ok(
            PatchSpace200ResponseDTO(200, PatchSpace200ResponseDataDTO(spaceDTO), "OK")
        )
    }

    @Guard("query", "space")
    override suspend fun listSpaceCategories(
        @ResourceId spaceId: Long,
        includeArchived: Boolean,
    ): ResponseEntity<ListSpaceCategories200ResponseDTO> {
        val categories =
            withContext(Dispatchers.IO) { spaceService.listCategories(spaceId, includeArchived) }
        return ResponseEntity.ok(
            ListSpaceCategories200ResponseDTO(
                200,
                ListSpaceCategories200ResponseDataDTO(categories),
                "OK",
            )
        )
    }

    @Guard("modify", "space")
    override suspend fun createSpaceCategory(
        @ResourceId spaceId: Long,
        createSpaceCategoryRequestDTO: CreateSpaceCategoryRequestDTO,
    ): ResponseEntity<CreateSpaceCategory201ResponseDTO> {
        val createdCategory =
            withContext(Dispatchers.IO) {
                spaceService.createCategory(
                    spaceId = spaceId,
                    name = createSpaceCategoryRequestDTO.name,
                    description = createSpaceCategoryRequestDTO.description,
                    displayOrder = createSpaceCategoryRequestDTO.displayOrder,
                )
            }
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
    override suspend fun getSpaceCategory(
        @ResourceId spaceId: Long,
        @PathVariable("categoryId") categoryId: Long,
    ): ResponseEntity<CreateSpaceCategory201ResponseDTO> {
        val category =
            withContext(Dispatchers.IO) { spaceService.getCategoryDTO(spaceId, categoryId) }
        return ResponseEntity.ok(
            CreateSpaceCategory201ResponseDTO(
                200,
                CreateSpaceCategory201ResponseDataDTO(category),
                "OK",
            )
        )
    }

    @Guard("modify", "space")
    override suspend fun updateSpaceCategory(
        @ResourceId spaceId: Long,
        @PathVariable("categoryId") categoryId: Long,
        updateSpaceCategoryRequestDTO: UpdateSpaceCategoryRequestDTO,
    ): ResponseEntity<CreateSpaceCategory201ResponseDTO> {
        val updatedCategory =
            withContext(Dispatchers.IO) {
                spaceService.updateCategory(
                    spaceId = spaceId,
                    categoryId = categoryId,
                    patch = updateSpaceCategoryRequestDTO,
                )
            }
        return ResponseEntity.ok(
            CreateSpaceCategory201ResponseDTO(
                200,
                CreateSpaceCategory201ResponseDataDTO(updatedCategory),
                "OK",
            )
        )
    }

    @Guard("modify", "space")
    override suspend fun deleteSpaceCategory(
        @ResourceId spaceId: Long,
        @PathVariable("categoryId") categoryId: Long,
    ): ResponseEntity<Unit> {
        withContext(Dispatchers.IO) { spaceService.deleteCategory(spaceId, categoryId) }
        return ResponseEntity.noContent().build()
    }

    @Guard("modify", "space")
    override suspend fun archiveSpaceCategory(
        @ResourceId spaceId: Long,
        categoryId: Long,
    ): ResponseEntity<CreateSpaceCategory201ResponseDTO> {
        val archivedCategory =
            withContext(Dispatchers.IO) { spaceService.archiveCategory(spaceId, categoryId) }
        return ResponseEntity.ok(
            CreateSpaceCategory201ResponseDTO(
                200,
                CreateSpaceCategory201ResponseDataDTO(archivedCategory),
                "OK",
            )
        )
    }

    @Guard("modify", "space")
    override suspend fun unarchiveSpaceCategory(
        @ResourceId spaceId: Long,
        @PathVariable("categoryId") categoryId: Long,
    ): ResponseEntity<CreateSpaceCategory201ResponseDTO> {
        val unarchivedCategory =
            withContext(Dispatchers.IO) { spaceService.unarchiveCategory(spaceId, categoryId) }
        return ResponseEntity.ok(
            CreateSpaceCategory201ResponseDTO(
                200,
                CreateSpaceCategory201ResponseDataDTO(unarchivedCategory),
                "OK",
            )
        )
    }

    @Guard("query", "space")
    override suspend fun getSpaceMePublishedTasks(
        @ResourceId spaceId: Long,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        approved: String?,
        hasPendingParticipantApproval: Boolean?,
        hasPendingReview: Boolean?,
        sortBy: String,
        sortOrder: String,
    ): ResponseEntity<GetSpaceMePublishedTasks200ResponseDTO> {
        val currentUserId = jwtService.getCurrentUserId()
        val tasks =
            withContext(Dispatchers.IO) {
                spacePublisherViewService.getPublishedTasks(
                    spaceId = spaceId,
                    currentUserId = currentUserId,
                    from = from,
                    to = to,
                    categoryId = categoryId,
                    approved = approved,
                    hasPendingParticipantApproval = hasPendingParticipantApproval,
                    hasPendingReview = hasPendingReview,
                    sortBy = sortBy,
                    sortOrder = sortOrder,
                )
            }
        return ResponseEntity.ok(
            GetSpaceMePublishedTasks200ResponseDTO(code = 200, data = tasks, message = "OK")
        )
    }

    @Guard("query", "space")
    override suspend fun getSpaceMePublishing(
        @ResourceId spaceId: Long
    ): ResponseEntity<GetSpaceMePublishing200ResponseDTO> {
        val currentUserId = jwtService.getCurrentUserId()
        val overview =
            withContext(Dispatchers.IO) {
                spacePublisherViewService.getOverview(
                    spaceId = spaceId,
                    currentUserId = currentUserId,
                )
            }
        return ResponseEntity.ok(
            GetSpaceMePublishing200ResponseDTO(code = 200, data = overview, message = "OK")
        )
    }

    @Guard("query", "space")
    override suspend fun getSpaceMeParticipating(
        @ResourceId spaceId: Long
    ): ResponseEntity<GetSpaceMeParticipating200ResponseDTO> {
        val currentUserId = jwtService.getCurrentUserId()
        val overview =
            withContext(Dispatchers.IO) {
                spaceParticipantViewService.getOverview(
                    spaceId = spaceId,
                    currentUserId = currentUserId,
                )
            }
        return ResponseEntity.ok(
            GetSpaceMeParticipating200ResponseDTO(code = 200, data = overview, message = "OK")
        )
    }

    @Guard("query", "space")
    override suspend fun getSpaceMeParticipations(
        @ResourceId spaceId: Long,
        approved: String?,
        completionStatus: TaskCompletionStatusDTO?,
        identityType: TaskSubmitterTypeDTO?,
        sortBy: String,
        sortOrder: String,
    ): ResponseEntity<GetSpaceMeParticipations200ResponseDTO> {
        val currentUserId = jwtService.getCurrentUserId()
        val participations =
            withContext(Dispatchers.IO) {
                spaceParticipantViewService.getParticipations(
                    spaceId = spaceId,
                    currentUserId = currentUserId,
                    approved = approved,
                    completionStatus = completionStatus,
                    identityType = identityType,
                    sortBy = sortBy,
                    sortOrder = sortOrder,
                )
            }
        return ResponseEntity.ok(
            GetSpaceMeParticipations200ResponseDTO(
                code = 200,
                data = participations,
                message = "OK",
            )
        )
    }

    @Guard("query", "space")
    override suspend fun getSpaceAnalyticsOverview(
        @ResourceId spaceId: Long,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        groupBy: String,
    ): ResponseEntity<GetSpaceAnalyticsOverview200ResponseDTO> {
        val overview =
            withContext(Dispatchers.IO) {
                spaceAnalyticsService.getSpaceAnalyticsOverview(
                    spaceId = spaceId,
                    from = from,
                    to = to,
                    categoryId = categoryId,
                    publisherId = publisherId,
                    taskApproved = taskApproved,
                    groupBy = groupBy,
                )
            }
        return ResponseEntity.ok(
            GetSpaceAnalyticsOverview200ResponseDTO(code = 200, data = overview, message = "OK")
        )
    }

    @Guard("query", "space")
    override suspend fun getSpaceTaskAnalytics(
        @ResourceId spaceId: Long,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        hasPendingReview: Boolean?,
        hasPendingApproval: Boolean?,
        sortBy: String,
        sortOrder: String,
    ): ResponseEntity<GetSpaceTaskAnalytics200ResponseDTO> {
        val analytics =
            withContext(Dispatchers.IO) {
                spaceAnalyticsService.getSpaceTaskAnalytics(
                    spaceId = spaceId,
                    from = from,
                    to = to,
                    categoryId = categoryId,
                    publisherId = publisherId,
                    taskApproved = taskApproved,
                    hasPendingReview = hasPendingReview,
                    hasPendingApproval = hasPendingApproval,
                    sortBy = sortBy,
                    sortOrder = sortOrder,
                )
            }
        return ResponseEntity.ok(
            GetSpaceTaskAnalytics200ResponseDTO(code = 200, data = analytics, message = "OK")
        )
    }

    @Guard("query", "space")
    override suspend fun getSpaceAnalyticsPublishers(
        @ResourceId spaceId: Long,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        taskApproved: String?,
        sortBy: String,
        sortOrder: String,
    ): ResponseEntity<GetSpaceAnalyticsPublishers200ResponseDTO> {
        val publishers =
            withContext(Dispatchers.IO) {
                spaceAnalyticsService.getSpaceAnalyticsPublishers(
                    spaceId = spaceId,
                    from = from,
                    to = to,
                    categoryId = categoryId,
                    taskApproved = taskApproved,
                    sortBy = sortBy,
                    sortOrder = sortOrder,
                )
            }
        return ResponseEntity.ok(
            GetSpaceAnalyticsPublishers200ResponseDTO(code = 200, data = publishers, message = "OK")
        )
    }

    @Guard("query", "space")
    override suspend fun getSpaceAnalyticsParticipants(
        @ResourceId spaceId: Long,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        participationApproved: String?,
        completionStatus: String?,
        realName: String,
        groupBy: String,
    ): ResponseEntity<GetSpaceAnalyticsParticipants200ResponseDTO> {
        val participants =
            withContext(Dispatchers.IO) {
                spaceAnalyticsService.getSpaceAnalyticsParticipants(
                    spaceId = spaceId,
                    from = from,
                    to = to,
                    categoryId = categoryId,
                    publisherId = publisherId,
                    taskApproved = taskApproved,
                    participationApproved = participationApproved,
                    completionStatus = completionStatus,
                    realName = realName,
                    groupBy = groupBy,
                )
            }
        return ResponseEntity.ok(
            GetSpaceAnalyticsParticipants200ResponseDTO(
                code = 200,
                data = participants,
                message = "OK",
            )
        )
    }

    @Guard("query", "space")
    override suspend fun getSpaceAnalyticsAlerts(
        @ResourceId spaceId: Long
    ): ResponseEntity<GetSpaceAnalyticsAlerts200ResponseDTO> {
        val alerts =
            withContext(Dispatchers.IO) { spaceAnalyticsService.getSpaceAnalyticsAlerts(spaceId) }
        return ResponseEntity.ok(
            GetSpaceAnalyticsAlerts200ResponseDTO(code = 200, data = alerts, message = "OK")
        )
    }

    @Guard("query", "space")
    override suspend fun getPublishersParticipation(
        @ResourceId spaceId: Long,
        successBy: String,
    ): ResponseEntity<GetPublishersParticipation200ResponseDTO> {
        val participation =
            withContext(Dispatchers.IO) {
                spaceAnalyticsService.getPublishersParticipation(spaceId, successBy)
            }
        return ResponseEntity.ok(
            GetPublishersParticipation200ResponseDTO(
                code = 200,
                data = participation,
                message = "OK",
            )
        )
    }

    @Guard("query", "space")
    override suspend fun exportSpaceParticipants(
        @ResourceId spaceId: Long,
        format: String,
        from: Long?,
        to: Long?,
        taskStatus: String?,
        categoryId: Long?,
        publisherId: Long?,
        realName: String,
        successBy: String,
    ): ResponseEntity<String> {
        val csv =
            withContext(Dispatchers.IO) {
                spaceAnalyticsService.exportParticipants(
                    spaceId,
                    format,
                    from,
                    to,
                    taskStatus,
                    categoryId,
                    publisherId,
                    realName,
                    successBy,
                )
            }
        return ResponseEntity.ok(csv)
    }

    @Guard("query", "space")
    override suspend fun exportSpaceAnalyticsParticipants(
        @ResourceId spaceId: Long,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        participationApproved: String?,
        completionStatus: String?,
        realName: String,
    ): ResponseEntity<String> {
        val currentUserId = jwtService.getCurrentUserId()
        val csv =
            withContext(Dispatchers.IO) {
                spaceAnalyticsService.exportSpaceAnalyticsParticipants(
                    accessorId = currentUserId,
                    spaceId = spaceId,
                    from = from,
                    to = to,
                    categoryId = categoryId,
                    publisherId = publisherId,
                    taskApproved = taskApproved,
                    participationApproved = participationApproved,
                    completionStatus = completionStatus,
                    realName = realName,
                )
            }
        return ResponseEntity.ok(csv)
    }

    @Guard("query", "space")
    override suspend fun exportSpaceAnalyticsTasks(
        @ResourceId spaceId: Long,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        hasPendingReview: Boolean?,
        hasPendingApproval: Boolean?,
    ): ResponseEntity<String> {
        val csv =
            withContext(Dispatchers.IO) {
                spaceAnalyticsService.exportSpaceAnalyticsTasks(
                    spaceId = spaceId,
                    from = from,
                    to = to,
                    categoryId = categoryId,
                    publisherId = publisherId,
                    taskApproved = taskApproved,
                    hasPendingReview = hasPendingReview,
                    hasPendingApproval = hasPendingApproval,
                )
            }
        return ResponseEntity.ok(csv)
    }

    @Guard("query", "space")
    override suspend fun exportSpaceAnalyticsPublishers(
        @ResourceId spaceId: Long,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        taskApproved: String?,
    ): ResponseEntity<String> {
        val csv =
            withContext(Dispatchers.IO) {
                spaceAnalyticsService.exportSpaceAnalyticsPublishers(
                    spaceId = spaceId,
                    from = from,
                    to = to,
                    categoryId = categoryId,
                    taskApproved = taskApproved,
                )
            }
        return ResponseEntity.ok(csv)
    }

    @Guard("query", "space")
    override suspend fun getSpaceTopics(
        spaceId: Long,
        keyword: String?,
        sort: String,
        limit: Int,
    ): ResponseEntity<GetSpaceTopics200ResponseDTO> {
        val safeLimit = limit.coerceAtMost(50)

        // 1. 如果有 keyword，优先走搜索逻辑 (Search)
        if (!keyword.isNullOrBlank()) {
            val data = taskTopicsService.searchSpaceTopics(spaceId, keyword, safeLimit)
            return ResponseEntity.ok(
                GetSpaceTopics200ResponseDTO(200, GetSpaceTopics200ResponseDataDTO(data), "OK")
            )
        }

        // 2. 如果没有 keyword，且 sort=popularity，走热门逻辑 (Hot)
        //        if (sort == "popularity") {
        //            val data = taskTopicsService.getSpaceHotTopics(spaceId, safeLimit)
        //            return RestResult.success(data)
        //        }

        // 默认情况 返回热门
        val data = taskTopicsService.getSpaceHotTopics(spaceId, safeLimit)
        return ResponseEntity.ok(
            GetSpaceTopics200ResponseDTO(200, GetSpaceTopics200ResponseDataDTO(data), "OK")
        )
    }
}
