package org.rucca.cheese.task

import jakarta.persistence.*
import java.time.LocalDateTime
import org.rucca.cheese.common.persistent.IdType

@Entity
@Table(name = "task_ai_advice", indexes = [Index(columnList = "task_id,model_hash", unique = true)])
class TaskAIAdvice {
    @Column(nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: IdType? = null

    @Column(name = "task_id", nullable = false) var taskId: IdType? = null

    @Column(name = "model_hash", nullable = false, length = 64) var modelHash: String? = null

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: TaskAIAdviceStatus = TaskAIAdviceStatus.PENDING

    @Column(name = "topic_summary", columnDefinition = "TEXT") var topicSummary: String? = null

    @Column(name = "knowledge_fields", columnDefinition = "TEXT")
    var knowledgeFields: String? = null

    @Column(name = "learning_paths", columnDefinition = "TEXT") var learningPaths: String? = null

    @Column(name = "methodology", columnDefinition = "TEXT") var methodology: String? = null

    @Column(name = "team_tips", columnDefinition = "TEXT") var teamTips: String? = null

    @Column(name = "raw_response", columnDefinition = "TEXT") var rawResponse: String? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

enum class TaskAIAdviceStatus {
    PENDING, // 等待处理
    PROCESSING, // 正在处理
    COMPLETED, // 处理完成
    FAILED, // 处理失败
}
