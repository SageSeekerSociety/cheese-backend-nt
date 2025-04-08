package org.rucca.cheese.knowledge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hibernate.query.SortDirection
import org.rucca.cheese.api.KnowledgeApi
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.auth.annotation.UseNewAuth
import org.rucca.cheese.auth.spring.Auth
import org.rucca.cheese.auth.spring.AuthContext
import org.rucca.cheese.auth.spring.ResourceId
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
@UseNewAuth
class KnowledgeController(
    private val knowledgeService: KnowledgeService,
    private val jwtService: JwtService,
) : KnowledgeApi {
    @Auth("knowledge:create:knowledge")
    override suspend fun createKnowledge(
        @AuthContext("teamId", field = "teamId")
        createKnowledgeRequestDTO: CreateKnowledgeRequestDTO
    ): ResponseEntity<CreateKnowledge200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()
        val knowledgeDTO =
            withContext(Dispatchers.IO) {
                knowledgeService.createKnowledge(
                    name = createKnowledgeRequestDTO.name,
                    type = createKnowledgeRequestDTO.type,
                    content = createKnowledgeRequestDTO.content,
                    description = createKnowledgeRequestDTO.description,
                    teamId = createKnowledgeRequestDTO.teamId,
                    materialId = createKnowledgeRequestDTO.materialId,
                    projectId = createKnowledgeRequestDTO.projectId,
                    labels = createKnowledgeRequestDTO.labels,
                    discussionId = createKnowledgeRequestDTO.discussionId,
                    userId = userId,
                )
            }
        return ResponseEntity.ok(
            CreateKnowledge200ResponseDTO(
                200,
                "ok",
                CreateKnowledge200ResponseDataDTO(knowledgeDTO),
            )
        )
    }

    @Auth("knowledge:list:knowledge")
    override suspend fun listKnowledge(
        @AuthContext("teamId") teamId: Long,
        projectId: Long?,
        type: String?,
        labels: List<String>?,
        query: String?,
        pageStart: Long?,
        pageSize: Int,
        sortBy: String,
        sortOrder: String,
    ): ResponseEntity<ListKnowledge200ResponseDTO> {
        // 解析排序字段
        val sortByEnum =
            when (sortBy) {
                "updatedAt" -> KnowledgeService.KnowledgeSortBy.UPDATED_AT
                else -> KnowledgeService.KnowledgeSortBy.CREATED_AT
            }

        // 解析排序方向
        val sortOrderEnum =
            when (sortOrder.lowercase()) {
                "asc" -> SortDirection.ASCENDING
                else -> SortDirection.DESCENDING
            }

        // 解析知识类型
        val knowledgeType =
            type?.let {
                try {
                    KnowledgeType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }

        // 获取知识条目
        val (knowledgeDTOs, page) =
            knowledgeService.getKnowledges(
                teamId = teamId,
                projectId = projectId,
                type = knowledgeType,
                labels = labels,
                query = query,
                pageStart = pageStart,
                pageSize = pageSize,
                sortBy = sortByEnum,
                sortOrder = sortOrderEnum,
            )

        return ResponseEntity.ok(
            ListKnowledge200ResponseDTO(
                code = 200,
                message = "success",
                data = ListKnowledge200ResponseDataDTO(knowledges = knowledgeDTOs, page = page),
            )
        )
    }

    @Auth("knowledge:view:knowledge")
    override suspend fun knowledgeGetById(
        @ResourceId knowledgeId: Long
    ): ResponseEntity<KnowledgeGetById200ResponseDTO> {
        val knowledgeDTO = knowledgeService.getKnowledgeDTO(knowledgeId)
        return ResponseEntity.ok(KnowledgeGetById200ResponseDTO(200, "success", knowledgeDTO))
    }

    @Auth("knowledge:delete:knowledge")
    override suspend fun knowledgeDelete(
        @ResourceId knowledgeId: Long
    ): ResponseEntity<KnowledgeDelete200ResponseDTO> {
        knowledgeService.deleteKnowledge(knowledgeId)
        return ResponseEntity.ok(KnowledgeDelete200ResponseDTO(200, "OK"))
    }

    @Auth("knowledge:update:knowledge")
    override suspend fun updateKnowledge(
        @ResourceId knowledgeId: Long,
        updateKnowledgeRequestDTO: UpdateKnowledgeRequestDTO,
    ): ResponseEntity<UpdateKnowledge200ResponseDTO> {
        // 更新知识点信息
        val updatedKnowledge =
            withContext(Dispatchers.IO) {
                knowledgeService.updateKnowledge(knowledgeId, updateKnowledgeRequestDTO)
            }
        return ResponseEntity.ok(
            UpdateKnowledge200ResponseDTO(
                code = 200,
                data = UpdateKnowledge200ResponseDataDTO(updatedKnowledge),
                message = "OK",
            )
        )
    }
}
