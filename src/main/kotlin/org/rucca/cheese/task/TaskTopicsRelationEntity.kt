package org.rucca.cheese.task

import jakarta.persistence.*
import java.util.Optional
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.topic.Topic
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "task_id"), Index(columnList = "topic_id")])
class TaskTopicsRelation(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val task: Task? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val topic: Topic? = null,
) : BaseEntity()

interface TaskTopicsRelationRepository : JpaRepository<TaskTopicsRelation, IdType> {
    fun findByTaskIdAndTopicId(taskId: IdType, topicId: IdType): Optional<TaskTopicsRelation>

    fun findAllByTaskId(taskId: IdType): List<TaskTopicsRelation>
}
