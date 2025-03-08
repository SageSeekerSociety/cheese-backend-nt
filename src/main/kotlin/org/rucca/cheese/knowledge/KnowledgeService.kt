package org.rucca.cheese.knowledge

import java.time.LocalDateTime
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.EntityPatcher
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.KnowledgeDTO
import org.rucca.cheese.model.KnowledgePatchRequestDTO
import org.rucca.cheese.model.KnowledgeTypeDTO
import org.rucca.cheese.user.AvatarRepository
import org.rucca.cheese.user.UserService
import org.springframework.stereotype.Service

@Service
class KnowledgeService(
    private val knowledgeRepository: KnowledgeRepository,
    private val knowledgeLabelRepository: KnowledgeLabelRepository,
    private val userService: UserService,
    private val knowledgeAdminRelationRepository: KnowledgeAdminRelationRepository,
    private val avatarRepository: AvatarRepository,
    private val entityPatcher: EntityPatcher,
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

    //    fun parseType1(type: KnowledgePostRequestDTO.Type) =
    //        when (type.value) {
    //            "document" -> KnowledgeType.DOCUMENT
    //            "text" -> KnowledgeType.TEXT
    //            "image" -> KnowledgeType.IMAGE
    //            "link" -> KnowledgeType.LINK
    //            else -> throw NotImplementedError()
    //        }
    //
    //    fun parseType(type: KnowledgePatchRequestDTO.Type) =
    //        when (type.value) {
    //            "document" -> KnowledgeType.DOCUMENT
    //            "text" -> KnowledgeType.TEXT
    //            "image" -> KnowledgeType.IMAGE
    //            "link" -> KnowledgeType.LINK
    //            else -> throw NotImplementedError()
    //        }
    fun KnowledgeTypeDTO.toKnowledgeType(): KnowledgeType {
        return when (this) {
            KnowledgeTypeDTO.document -> KnowledgeType.DOCUMENT
            KnowledgeTypeDTO.link -> KnowledgeType.LINK
            KnowledgeTypeDTO.text -> KnowledgeType.TEXT
            KnowledgeTypeDTO.image -> KnowledgeType.IMAGE
        }
    }

    fun createKnowledge(
        name: String,
        type: KnowledgeTypeDTO,
        content: String,
        description: String?,
        projectIds: List<Long>? = null,
        labels: List<String>? = null,
    ): KnowledgeDTO { // project panduan ! kong  set
        val knowledge =
            Knowledge(
                    name = name,
                    type = type.toKnowledgeType(),
                    content = content,
                    description = description,
                    projectIds = projectIds!!.toSet(),
                )
                .let { knowledgeRepository.save(it) }

        labels?.forEach {
            val knowledgeLabel = KnowledgeLabelEntity(knowledge = knowledge, label = it)
            knowledgeLabelRepository.save(knowledgeLabel)
        }
        return knowledge.toKnowledgeDTO() // ?
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
        id: IdType,
        knowledgePatchRequestDTO: KnowledgePatchRequestDTO,
    ): KnowledgeDTO {
        val knowledge = getKnowledge(id)
        val updatedKnowledge =
            entityPatcher.patch(knowledge, knowledgePatchRequestDTO) {
                handle(KnowledgePatchRequestDTO::name) { entity, value -> entity.name = value }
                handle(KnowledgePatchRequestDTO::description) { entity, value ->
                    entity.description = value
                }
                handle(KnowledgePatchRequestDTO::type) { entity, value ->
                    entity.type = value.toKnowledgeType()
                }
                handle(KnowledgePatchRequestDTO::content) { entity, value ->
                    entity.content = value
                }
                handle(KnowledgePatchRequestDTO::projectIds) { entity, value ->
                    entity.projectIds = value?.toSet() ?: emptySet()
                }
                handle(KnowledgePatchRequestDTO::labels) { entity, value ->
                    // 删除旧标签
                    knowledgeLabelRepository.deleteAll(
                        knowledgeLabelRepository.findAll().filter { label ->
                            label.knowledge?.id == id
                        }
                    )
                    // 添加新标签
                    value?.forEach { labelText ->
                        knowledgeLabelRepository.save(
                            KnowledgeLabelEntity(knowledge = entity, label = labelText)
                        )
                    }
                }
            }
        return knowledgeRepository.save(updatedKnowledge).toKnowledgeDTO()
    }
}
