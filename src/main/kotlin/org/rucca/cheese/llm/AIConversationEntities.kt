package org.rucca.cheese.llm

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import java.time.OffsetDateTime
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.Type
import org.hibernate.type.SqlTypes
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.AIConversationDTO
import org.rucca.cheese.model.AIMessageDTO
import org.rucca.cheese.task.TaskAIAdviceContext

/** 表示一个完整的对话，包含多条消息 */
@Entity
@Table(name = "ai_conversation")
@SQLRestriction("deleted_at IS NULL")
class AIConversationEntity : BaseEntity() {
    @Column(name = "conversation_id", nullable = false, unique = true)
    var conversationId: String = ""

    @Column(name = "title", nullable = true) var title: String? = null

    @Column(name = "module_type", nullable = false)
    var moduleType: String = "" // 标识对话所属的模块，如 "task_ai_advice"、"paper_review" 等

    @Column(name = "context_id", nullable = true) var contextId: IdType? = null // 关联到具体模块的上下文信息

    @Column(name = "owner_id", nullable = false) var ownerId: IdType = 0 // 对话所有者的用户ID

    @Column(name = "model_type", nullable = false, columnDefinition = "TEXT DEFAULT 'standard'")
    var modelType: String = "standard" // 使用的AI模型类型

    @OneToMany(mappedBy = "conversation", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var messages: MutableList<AIMessageEntity> = mutableListOf()
}

/** 表示对话中的单条消息 */
@Entity
@Table(name = "ai_message")
@SQLRestriction("deleted_at IS NULL")
class AIMessageEntity : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", referencedColumnName = "id", nullable = false)
    var conversation: AIConversationEntity? = null

    @Column(name = "role", nullable = false) var role: String = "" // "user" 或 "assistant"

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String = "" // 消息内容

    @Column(name = "parent_id", nullable = true) var parentId: IdType? = null // 父消息ID，用于分支对话

    @Column(name = "reasoning_content", nullable = true, columnDefinition = "TEXT")
    var reasoningContent: String? = null // AI的推理过程，仅对assistant角色有效

    @Column(name = "reasoning_time_ms", nullable = true)
    var reasoningTimeMs: Long? = null // AI的推理时间，仅对assistant角色有效

    @Type(JsonBinaryType::class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    var metadata: Map<String, Any> = mapOf() // 消息的额外元数据，如followupQuestions等

    fun toDTO(): AIMessageDTO {
        return AIMessageDTO(
            id = id!!,
            conversationId = conversation?.conversationId!!,
            role = role,
            content = content,
            parentId = parentId,
            reasoningContent = reasoningContent,
            reasoningTimeMs = reasoningTimeMs,
            metadata = metadata,
            createdAt = OffsetDateTime.of(createdAt, OffsetDateTime.now().offset),
        )
    }
}
