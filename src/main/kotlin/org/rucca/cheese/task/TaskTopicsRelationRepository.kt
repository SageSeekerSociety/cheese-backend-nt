package org.rucca.cheese.task

import java.util.*
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository

interface TaskTopicsRelationRepository : JpaRepository<TaskTopicsRelation, IdType> {
    fun findByTaskIdAndTopicId(taskId: IdType, topicId: IdType): Optional<TaskTopicsRelation>

    fun findAllByTaskId(taskId: IdType): List<TaskTopicsRelation>
}
