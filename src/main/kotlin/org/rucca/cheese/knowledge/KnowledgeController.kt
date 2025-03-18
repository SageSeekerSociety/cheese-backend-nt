package org.rucca.cheese.knowledge

import org.rucca.cheese.api.KnowledgesApi
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.spring.UseOldAuth
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
@UseOldAuth
class KnowledgeController(
    private val knowledgeService: KnowledgeService,
    private val jwtService: JwtService,
) : KnowledgesApi {
    @Guard("create", "knowledge")
    override fun knowledgePost(
        knowledgePostRequestDTO: KnowledgePostRequestDTO
    ): ResponseEntity<KnowledgePost200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()
        val knowledgeDTO =
            knowledgeService.createKnowledge(
                name = knowledgePostRequestDTO.name,
                type = knowledgePostRequestDTO.type,
                content = knowledgePostRequestDTO.content,
                description = knowledgePostRequestDTO.description,
                projectIds = knowledgePostRequestDTO.projectIds,
                labels = knowledgePostRequestDTO.labels,
                createdByUserId = userId,
            )
        return ResponseEntity.ok(
            KnowledgePost200ResponseDTO(200, "ok", KnowledgePost200ResponseDataDTO(knowledgeDTO))
        )
    }

    @Guard("query", "knowledge")
    override fun knowledgeGet(
        projectIds: List<Long>?,
        type: String?,
        labels: List<String>?,
        query: String?,
        pageStart: Long?,
        pageSize: Int,
    ): ResponseEntity<KnowledgeGet200ResponseDTO> {

        val knowledgeDTO = knowledgeService.getKnowledgeDTOByProjectId(projectIds)
        // val queryOptions = SpaceQueryOptions(queryMyRank = queryMyRank)?
        return ResponseEntity.ok(
            KnowledgeGet200ResponseDTO(200, "success", KnowledgeGet200ResponseDataDTO(knowledgeDTO))
        )
    }

    @Guard("query2", "knowledge")
    override fun knowledgeGetById(
        knowledgeId: Long
    ): ResponseEntity<KnowledgeGetById200ResponseDTO> {
        val knowledgeDTO = knowledgeService.getKnowledgeDTO(knowledgeId)
        return ResponseEntity.ok(KnowledgeGetById200ResponseDTO(200, "success", knowledgeDTO))
    }

    @Guard("delete", "knowledge")
    override fun knowledgeDelete(id: Long): ResponseEntity<KnowledgeDelete200ResponseDTO> {
        knowledgeService.deleteKnowledge(id)
        return ResponseEntity.ok(KnowledgeDelete200ResponseDTO(200, "OK"))
    }

    @Guard("update", "knowledge")
    override fun knowledgePatch(
        id: Long,
        knowledgePatchRequestDTO: KnowledgePatchRequestDTO,
    ): ResponseEntity<PatchKnowledge200ResponseDTO> {
        // 更新知识点信息
        val updatedKnowledge = knowledgeService.updateKnowledge(id, knowledgePatchRequestDTO)
        return ResponseEntity.ok(
            PatchKnowledge200ResponseDTO(
                code = 200,
                data = PatchKnowledge200ResponseDataDTO(updatedKnowledge),
                message = "OK",
            )
        )
    }
}
