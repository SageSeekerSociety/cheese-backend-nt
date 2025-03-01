package org.rucca.cheese.knowledge

import java.time.LocalDateTime
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.KnowledgeDTO
import org.rucca.cheese.model.KnowledgePatchRequestDTO
import org.rucca.cheese.model.KnowledgePostRequestDTO
import org.rucca.cheese.user.UserService
import org.springframework.stereotype.Service

@Service
class KnowledgeService(
    private val knowledgeRepository: KnowledgeRepository,
    private val knowledgeLabelRepository: KnowledgeLabelRepository,
    private val userService: UserService,
    private val knowledgeAdminRelationRepository: KnowledgeAdminRelationRepository,
) {
    fun deleteknowledge(kid: IdType) {
        val knowledge =
            knowledgeRepository.findById(kid).orElseThrow { NotFoundError("knowledge", kid) }
        knowledge.deletedAt = LocalDateTime.now()
        knowledgeRepository.save(knowledge)
    }

    fun isKnowledgeAdmin(knowledgeId: IdType, userId: IdType): Boolean {
        return knowledgeAdminRelationRepository.existsByKnowledgeIdAndUserId(knowledgeId, userId)
    }

    fun getKnowledgeOwner(knowledgeId: IdType): IdType {
        val knowledgeAdminRelation =
            knowledgeAdminRelationRepository
                .findByKnowledgeIdAndRole(knowledgeId, KnowledgeAdminRole.OWNER)
                .orElseThrow { NotFoundError("knowledge", knowledgeId) }
        return knowledgeAdminRelation.user!!.id!!.toLong()
    }

    fun getKnowledge(kid: IdType): Knowledge =
        knowledgeRepository.findById(kid).orElseThrow { NotFoundError("knowledge", kid) }

    fun parseType1(type: KnowledgePostRequestDTO.Type) =
        when (type.value) {
            "document" -> KnowledgeType.DOCUMENT
            "text" -> KnowledgeType.TEXT
            "image" -> KnowledgeType.IMAGE
            "link" -> KnowledgeType.LINK
            else -> throw NotImplementedError()
        }

    fun parseType(type: KnowledgePatchRequestDTO.Type) =
        when (type.value) {
            "document" -> KnowledgeType.DOCUMENT
            "text" -> KnowledgeType.TEXT
            "image" -> KnowledgeType.IMAGE
            "link" -> KnowledgeType.LINK
            else -> throw NotImplementedError()
        }

    fun createKnowledge(
        name: String,
        type: KnowledgePostRequestDTO.Type, // 按照knowledgepostrequestDTO 填？
        content: String,
        description: String?,
        projectIds: List<Long>? = null,
        labels: List<String>? = null,
    ): IdType { // project panduan ! kong  set
        val knowledge =
            Knowledge(
                name = name,
                type = parseType1(type),
                content = content,
                description = description,
                projectIds = projectIds!!.toSet(),
            )
        knowledgeRepository.save(knowledge)

        labels?.forEach {
            val knowledgeLabel = KnowledgeLabelEntity(knowledge = knowledge, label = it)
            knowledgeLabelRepository.save(knowledgeLabel)
        }
        return knowledge.id!!
    }

    fun getKnowledgeDTO(knowledgeId: IdType): KnowledgeDTO {
        return getKnowledge(knowledgeId).toKnowledgeDTO()
    }

    fun getKnowledgeDTOByProjectId(projectId: List<Long>?): List<KnowledgeDTO> {
        return knowledgeRepository.findKnowledgeByProjectId(projectId).map { it.toKnowledgeDTO() }
    }

    fun Knowledge.toKnowledgeDTO(): KnowledgeDTO {
        val creatorId = this.createdBy!!.id!!.toLong()
        return KnowledgeDTO(
            id = this.id!!,
            name = this.name!!,
            type = KnowledgeDTO.Type.forValue(this.type!!.name),
            content = this.content!!,
            description = this.description,
            materialId = this.material?.id?.toLong(),
            projectIds = this.projectIds.toList(),
            creator = userService.getUserDto(creatorId),
            createdAt = this.createdAt!!.toEpochMilli(),
            updatedAt = this.updatedAt!!.toEpochMilli(),
        )
    }

    fun updateKnowledge(
        id: Long,
        name: String?,
        description: String?,
        type: KnowledgePatchRequestDTO.Type?,
        content: String?,
        projectIds: List<Long>?,
        labels: List<String>?,
    ) {
        val knowledge = getKnowledge(id)

        // 更新基本信息
        name?.let { knowledge.name = it }
        description?.let { knowledge.description = it }
        type?.let { knowledge.type = parseType(it) }
        content?.let { knowledge.content = it }
        projectIds?.let { knowledge.projectIds = it.toSet() }

        // 保存知识点更新
        knowledgeRepository.save(knowledge)

        // 更新标签
        labels?.let {
            // 删除旧标签
            knowledgeLabelRepository.deleteAll(
                knowledgeLabelRepository.findAll().filter { label -> label.knowledge?.id == id }
            )
            // 添加新标签
            it.forEach { labelText ->
                knowledgeLabelRepository.save(
                    KnowledgeLabelEntity(knowledge = knowledge, label = labelText)
                )
            }
        }
    }
}
