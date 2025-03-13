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
import org.rucca.cheese.user.UserRepository
import org.rucca.cheese.user.UserService
import org.springframework.stereotype.Service

@Service
class KnowledgeService(
    private val knowledgeRepository: KnowledgeRepository,
    private val knowledgeLabelRepository: KnowledgeLabelRepository,
    private val userService: UserService,
    private val avatarRepository: AvatarRepository,
    private val entityPatcher: EntityPatcher,
    private val userRepository: UserRepository,
) {
    fun deleteKnowledge(kid: IdType) {
        val knowledge =
            knowledgeRepository.findById(kid).orElseThrow { NotFoundError("knowledge", kid) }
        knowledge.deletedAt = LocalDateTime.now()
        knowledgeRepository.save(knowledge)
    }

    private fun getKnowledge(kid: IdType): Knowledge =
        knowledgeRepository.findById(kid).orElseThrow { NotFoundError("knowledge", kid) }

    fun getKnowledgeDTO(kid: IdType): KnowledgeDTO =
        knowledgeRepository
            .findById(kid)
            .orElseThrow { NotFoundError("knowledge", kid) }
            .toKnowledgeDTO()

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
        return KnowledgeType.valueOf(this.value)
    }

    fun createKnowledge(
        name: String,
        type: KnowledgeTypeDTO,
        createdByUserId: IdType,
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
                    createdBy = userRepository.getReferenceById(createdByUserId.toInt()),
                )
                .let { knowledgeRepository.save(it) }

        labels?.forEach {
            val knowledgeLabel = KnowledgeLabelEntity(knowledge = knowledge, label = it)
            knowledgeLabelRepository.save(knowledgeLabel)
        }
        return knowledge.toKnowledgeDTO() // ?
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
            labels = this.knowledgeLabels.mapNotNull { it.label },
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
                    entity.projectIds = value.toSet()
                }
                handle(KnowledgePatchRequestDTO::labels) { entity, value ->
                    entity.knowledgeLabels.removeIf { labelEntity ->
                        value.none { labelEntity.label == it }
                    }
                    entity.knowledgeLabels.addAll(
                        value
                            .filter { label -> entity.knowledgeLabels.none { it.label == label } }
                            .map { KnowledgeLabelEntity(entity, it) }
                    )
                }
            }
        return knowledgeRepository.save(updatedKnowledge).toKnowledgeDTO()
    }
}
