package org.rucca.cheese.llm

import com.aallam.openai.api.chat.ChatReference
import com.aallam.openai.api.chat.ChatReferenceExtra
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import java.time.OffsetDateTime
import kotlinx.serialization.Serializable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.Type
import org.hibernate.type.SqlTypes
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.AIMessageDTO
import org.rucca.cheese.model.AIMessageMetadataDTO
import org.rucca.cheese.model.ConversationReferenceDTO
import org.rucca.cheese.model.ConversationReferenceExtraDTO

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

    @Column(name = "model_type", nullable = false, columnDefinition = "TEXT DEFAULT 'standard'")
    var modelType: String = "standard" // 使用的AI模型类型

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String = "" // 消息内容

    @Column(name = "parent_id", nullable = true) var parentId: IdType? = null // 父消息ID，用于分支对话

    @Column(name = "reasoning_content", nullable = true, columnDefinition = "TEXT")
    var reasoningContent: String? = null // AI的推理过程，仅对assistant角色有效

    @Column(name = "reasoning_time_ms", nullable = true)
    var reasoningTimeMs: Long? = null // AI的推理时间，仅对assistant角色有效

    @Column(name = "tokens_used", nullable = true) var tokensUsed: Int? = null // 消息消耗的token数量

    @Column(name = "seu_consumed", nullable = true, precision = 10, scale = 4)
    var seuConsumed: java.math.BigDecimal? = null // 消息消耗的SEU数量

    @Type(JsonBinaryType::class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    var metadata: AIMessageMetadata? = null // 消息的额外元数据，如followupQuestions等

    fun toDTO(): AIMessageDTO {
        return AIMessageDTO(
            id = id!!,
            conversationId = conversation?.conversationId!!,
            role = role,
            modelType = modelType,
            content = content,
            parentId = parentId,
            reasoningContent = reasoningContent,
            reasoningTimeMs = reasoningTimeMs,
            tokensUsed = tokensUsed ?: 0,
            seuConsumed = seuConsumed?.setScale(2, java.math.RoundingMode.HALF_UP)?.toDouble() ?: 0.0,
            metadata = metadata?.toDTO(),
            createdAt = OffsetDateTime.of(createdAt, OffsetDateTime.now().offset),
        )
    }
}

@Serializable
data class AIMessageMetadata(
    val followupQuestions: List<String>? = null,
    val references: List<ChatReference>? = null,
)

fun AIMessageMetadata.toDTO(): AIMessageMetadataDTO {
    return AIMessageMetadataDTO(
        followupQuestions = followupQuestions,
        references = references?.map { it.toDTO() },
    )
}

fun AIMessageMetadataDTO.toEntity(): AIMessageMetadata {
    return AIMessageMetadata(
        followupQuestions = followupQuestions,
        references = references?.map { it.toEntity() },
    )
}

fun ChatReference.toDTO(): ConversationReferenceDTO {
    return ConversationReferenceDTO(
        url = url,
        logoUrl = logoUrl,
        title = title,
        summary = summary,
        publishTime = publishTime,
        extra = extra.toDTO(),
    )
}

fun ConversationReferenceDTO.toEntity(): ChatReference {
    return ChatReference(
        url = url,
        logoUrl = logoUrl,
        title = title,
        summary = summary,
        publishTime = publishTime,
        extra = extra.toEntity(),
    )
}

fun ChatReferenceExtra.toDTO(): ConversationReferenceExtraDTO {
    return ConversationReferenceExtraDTO(
        relInfo = relInfo,
        freshnessInfo = freshnessInfo,
        authInfo = authInfo,
        finalRef = finalRef,
    )
}

fun ConversationReferenceExtraDTO.toEntity(): ChatReferenceExtra {
    return ChatReferenceExtra(
        relInfo = relInfo,
        freshnessInfo = freshnessInfo,
        authInfo = authInfo,
        finalRef = finalRef,
    )
}
