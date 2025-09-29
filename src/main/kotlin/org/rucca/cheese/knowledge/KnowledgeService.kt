package org.rucca.cheese.knowledge

import java.time.LocalDateTime
import java.time.ZoneId
import org.hibernate.query.SortDirection
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.EntityPatcher
import org.rucca.cheese.common.pagination.model.toPageDTO
import org.rucca.cheese.common.pagination.repository.findAllWithIdCursor
import org.rucca.cheese.common.pagination.repository.idSeekSpec
import org.rucca.cheese.common.pagination.repository.specification
import org.rucca.cheese.common.pagination.util.toJpaDirection
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.spec.div
import org.rucca.cheese.common.query.internal.spec.col
import org.rucca.cheese.common.query.internal.spec.exists
import org.rucca.cheese.common.query.internal.spec.parent
import org.rucca.cheese.discussion.DiscussionRepository
import org.rucca.cheese.material.MaterialRepository
import org.rucca.cheese.material.toMaterialDTO
import org.rucca.cheese.model.KnowledgeDTO
import org.rucca.cheese.model.KnowledgeTypeDTO
import org.rucca.cheese.model.PageDTO
import org.rucca.cheese.model.UpdateKnowledgeRequestDTO
import org.rucca.cheese.team.Team
import org.rucca.cheese.team.TeamRepository
import org.rucca.cheese.user.UserRepository
import org.rucca.cheese.user.services.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KnowledgeService(
    private val knowledgeRepository: KnowledgeRepository,
    private val knowledgeLabelRepository: KnowledgeLabelRepository,
    private val userService: UserService,
    private val entityPatcher: EntityPatcher,
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository,
    private val discussionRepository: DiscussionRepository,
    private val materialRepository: MaterialRepository,
) {
    fun deleteKnowledge(knowledgeId: IdType) {
        val knowledge =
            knowledgeRepository.findById(knowledgeId).orElseThrow {
                NotFoundError("knowledge", knowledgeId)
            }
        knowledge.deletedAt = LocalDateTime.now()
        knowledgeRepository.save(knowledge)
    }

    fun getKnowledge(knowledgeId: IdType): Knowledge =
        knowledgeRepository.findById(knowledgeId).orElseThrow {
            NotFoundError("knowledge", knowledgeId)
        }

    private fun getKnowledgeForEdit(knowledgeId: IdType): Knowledge =
        knowledgeRepository.findById(knowledgeId).orElseThrow {
            NotFoundError("knowledge", knowledgeId)
        }

    fun getKnowledgeDTO(knowledgeId: IdType): KnowledgeDTO =
        knowledgeRepository
            .findById(knowledgeId)
            .orElseThrow { NotFoundError("knowledge", knowledgeId) }
            .toKnowledgeDTO()

    fun KnowledgeTypeDTO.toKnowledgeType(): KnowledgeType {
        return KnowledgeType.valueOf(this.value)
    }

    @Transactional
    fun createKnowledge(
        name: String,
        type: KnowledgeTypeDTO,
        userId: IdType,
        content: String,
        description: String?,
        teamId: Long,
        materialId: IdType? = null,
        projectId: Long? = null,
        labels: List<String>? = null,
        discussionId: Long? = null,
    ): KnowledgeDTO {
        val team = teamRepository.findById(teamId).orElseThrow { NotFoundError("team", teamId) }

        val sourceDiscussion =
            if (discussionId != null) {
                discussionRepository.findById(discussionId).orElseThrow {
                    NotFoundError("discussion", discussionId)
                }
            } else null

        val sourceType =
            if (discussionId != null) {
                KnowledgeSource.FROM_DISCUSSION
            } else {
                KnowledgeSource.MANUAL
            }

        val knowledge =
            Knowledge(
                    name = name,
                    type = type.toKnowledgeType(),
                    content = content,
                    description = description,
                    team = team,
                    material =
                        if (materialId != null) materialRepository.getReferenceById(materialId)
                        else null,
                    projectId = projectId,
                    sourceDiscussion = sourceDiscussion,
                    sourceType = sourceType,
                    createdBy = userRepository.getReferenceById(userId.toInt()),
                )
                .apply {
                    labels?.forEach { label ->
                        this.knowledgeLabels.add(KnowledgeLabelEntity(this, label))
                    }
                }
                .let { knowledgeRepository.save(it) }

        return knowledge.toKnowledgeDTO()
    }

    enum class KnowledgeSortBy {
        CREATED_AT,
        UPDATED_AT,
    }

    /**
     * 获取知识条目列表，支持分页和多种筛选条件
     *
     * @param teamId 团队ID（必填）
     * @param projectId 项目ID（可选）
     * @param type 知识条目类型（可选）
     * @param labels 标签列表（可选）
     * @param query 搜索关键词（可选）
     * @param pageStart 分页起始ID
     * @param pageSize 每页大小
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @return 知识条目列表和分页信息
     */
    fun getKnowledges(
        teamId: Long,
        projectId: Long? = null,
        type: KnowledgeType? = null,
        labels: List<String>? = null,
        query: String? = null,
        pageStart: Long? = null,
        pageSize: Int = 10,
        sortBy: KnowledgeSortBy = KnowledgeSortBy.CREATED_AT,
        sortOrder: SortDirection = SortDirection.DESCENDING,
    ): Pair<List<KnowledgeDTO>, PageDTO> {
        val direction = sortOrder.toJpaDirection()
        val sortProperty =
            when (sortBy) {
                KnowledgeSortBy.CREATED_AT -> Knowledge::createdAt
                KnowledgeSortBy.UPDATED_AT -> Knowledge::updatedAt
            }

        val cursorSpec =
            knowledgeRepository
                .idSeekSpec(Knowledge::id, sortProperty = sortProperty, direction = direction)
                .specification {
                    where {
                        Knowledge::team / Team::id eq teamId

                        projectId?.let { Knowledge::projectId eq it }
                        type?.let { Knowledge::type eq it }

                        if (!query.isNullOrBlank()) {
                            or {
                                val pattern = "%${query.lowercase()}%"
                                Knowledge::name ilike pattern
                                Knowledge::description ilike pattern
                            }
                        }

                        if (!labels.isNullOrEmpty()) {
                            exists<KnowledgeLabelEntity> {
                                col(KnowledgeLabelEntity::knowledge / Knowledge::id) eq
                                    parent(Knowledge::id)
                                KnowledgeLabelEntity::label inList labels
                            }
                        }
                    }
                }
                .build()

        val result = knowledgeRepository.findAllWithIdCursor(cursorSpec, pageStart, pageSize)
        val knowledgeDTOs = result.content.map { it.toKnowledgeDTO() }

        return Pair(knowledgeDTOs, result.pageInfo.toPageDTO())
    }

    fun Knowledge.toKnowledgeDTO(): KnowledgeDTO {
        val creatorId = this.createdBy!!.id!!.toLong()
        return KnowledgeDTO(
            id = this.id!!,
            name = this.name!!,
            type = KnowledgeTypeDTO.forValue(this.type!!.name),
            content = this.content!!,
            description = this.description,
            material = this.material?.toMaterialDTO(),
            teamId = this.team?.id,
            projectId = this.projectId,
            sourceType = this.sourceType.name,
            discussionId = this.sourceDiscussion?.id,
            creator = userService.getUserDto(creatorId),
            createdAt = this.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            updatedAt = this.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            labels = this.knowledgeLabels.mapNotNull { it.label },
        )
    }

    @Transactional
    fun updateKnowledge(id: IdType, patch: UpdateKnowledgeRequestDTO): KnowledgeDTO {
        val knowledge = getKnowledgeForEdit(id)
        val updatedKnowledge =
            entityPatcher.patch(knowledge, patch) {
                handle(UpdateKnowledgeRequestDTO::type) { entity, value ->
                    entity.type = value.toKnowledgeType()
                }
                handle(UpdateKnowledgeRequestDTO::projectId) { entity, value ->
                    entity.projectId = value
                }
                handle(UpdateKnowledgeRequestDTO::teamId) { entity, value ->
                    val team =
                        teamRepository.findById(value).orElseThrow { NotFoundError("team", value) }
                    entity.team = team
                }
                handle(UpdateKnowledgeRequestDTO::labels) { entity, value ->
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
