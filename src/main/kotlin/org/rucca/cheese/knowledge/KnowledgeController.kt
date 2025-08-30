package org.rucca.cheese.knowledge

import org.hibernate.query.SortDirection
import org.rucca.cheese.api.KnowledgesApi
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
) : KnowledgesApi {
    @Auth("knowledge:create:knowledge")
    override fun knowledgePost(
        @AuthContext("teamId", field = "teamId") knowledgePostRequestDTO: KnowledgePostRequestDTO
    ): ResponseEntity<KnowledgePost200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()
        val knowledgeDTO =
            knowledgeService.createKnowledge(
                name = knowledgePostRequestDTO.name,
                type = knowledgePostRequestDTO.type,
                content = knowledgePostRequestDTO.content,
                description = knowledgePostRequestDTO.description,
                teamId = knowledgePostRequestDTO.teamId,
                materialId = knowledgePostRequestDTO.materialId,
                projectId = knowledgePostRequestDTO.projectId,
                labels = knowledgePostRequestDTO.labels,
                discussionId = knowledgePostRequestDTO.discussionId,
                userId = userId,
            )
        return ResponseEntity.ok(
            KnowledgePost200ResponseDTO(200, "ok", KnowledgePost200ResponseDataDTO(knowledgeDTO))
        )
    }

    @Auth("knowledge:list:knowledge")
    override fun knowledgeGet(
        @AuthContext("teamId") teamId: Long,
        projectId: Long?,
        type: String?,
        labels: List<String>?,
        query: String?,
        pageStart: Long?,
        pageSize: Int,
        sortBy: String,
        sortOrder: String,
    ): ResponseEntity<KnowledgeGet200ResponseDTO> {
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
            KnowledgeGet200ResponseDTO(
                code = 200,
                message = "success",
                data = KnowledgeGet200ResponseDataDTO(knowledges = knowledgeDTOs, page = page),
            )
        )
    }

    @Auth("knowledge:view:knowledge")
    override fun knowledgeGetById(
        @ResourceId knowledgeId: Long
    ): ResponseEntity<KnowledgeGetById200ResponseDTO> {
        val knowledgeDTO = knowledgeService.getKnowledgeDTO(knowledgeId)
        return ResponseEntity.ok(KnowledgeGetById200ResponseDTO(200, "success", knowledgeDTO))
    }

    @Auth("knowledge:delete:knowledge")
    override fun knowledgeDelete(
        @ResourceId knowledgeId: Long
    ): ResponseEntity<KnowledgeDelete200ResponseDTO> {
        knowledgeService.deleteKnowledge(knowledgeId)
        return ResponseEntity.ok(KnowledgeDelete200ResponseDTO(200, "OK"))
    }

    @Auth("knowledge:update:knowledge")
    override fun updateKnowledge(
        @ResourceId knowledgeId: Long,
        updateKnowledgeRequestDTO: UpdateKnowledgeRequestDTO,
    ): ResponseEntity<UpdateKnowledge200ResponseDTO> {
        // 更新知识点信息
        val updatedKnowledge =
            knowledgeService.updateKnowledge(knowledgeId, updateKnowledgeRequestDTO)
        return ResponseEntity.ok(
            UpdateKnowledge200ResponseDTO(
                code = 200,
                data = UpdateKnowledge200ResponseDataDTO(updatedKnowledge),
                message = "OK",
            )
        )
    }
}
