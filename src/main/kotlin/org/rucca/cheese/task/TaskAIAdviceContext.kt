package org.rucca.cheese.task

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType

/** 任务AI建议特定的上下文信息 */
@Entity
@Table(
    name = "task_ai_advice_context",
    uniqueConstraints =
        [
            UniqueConstraint(
                name = "uk_task_section_index",
                columnNames = ["task_id", "section", "section_index"],
            )
        ],
)
@SQLRestriction("deleted_at IS NULL")
class TaskAIAdviceContext : BaseEntity() {
    @Column(name = "task_id", nullable = false) var taskId: IdType = 0

    @Column(name = "section", nullable = true) var section: String? = null // 对话相关的建议部分

    @Column(name = "section_index", nullable = true) var sectionIndex: Int? = null // 建议部分的索引
}
