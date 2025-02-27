package org.rucca.cheese.knowledge

import org.rucca.cheese.api.KnowledgeApi
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.annotation.ResourceId
import org.rucca.cheese.model.KnowledgeGet200ResponseDTO
import org.rucca.cheese.model.KnowledgePost200ResponseDTO
import org.rucca.cheese.model.KnowledgePostRequestDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class KnowledgeController(private val knowledgeService: KnowledgeService) : KnowledgeApi {
    @Guard("create", "knowledge")
    override fun knowledgePost(
        knowledgePostRequestDTO: KnowledgePostRequestDTO
    ): ResponseEntity<KnowledgePost200ResponseDTO> {
        // TODO: Implement
        TODO()
    }

    @Guard("query", "knowledge")
    override fun knowledgeGet(
        @ResourceId projectIds: List<Long>?,
        type: String?,
        labels: List<String>?,
        query: String?,
        pageStart: Long?,
        pageSize: Int,
    ): ResponseEntity<KnowledgeGet200ResponseDTO> {
        // TODO: Implement
        TODO()
    }
}
