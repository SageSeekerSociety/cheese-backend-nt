/*
 *  Description: This file implements the TaskTopicsService class.
 *               It is responsible for CRUD of a task's topics.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.task

import java.time.LocalDateTime
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TopicDTO
import org.rucca.cheese.topic.Topic
import org.rucca.cheese.topic.TopicService
import org.springframework.stereotype.Service

@Service
class TaskTopicsService(
    private val topicsRelationRepository: TaskTopicsRelationRepository,
    private val topicService: TopicService,
) {
    fun getTaskTopicIds(taskId: IdType): List<IdType> {
        return topicsRelationRepository.findAllByTaskId(taskId).map { it.topic!!.id!!.toLong() }
    }

    fun getTaskTopicDTOs(taskId: IdType): List<TopicDTO> {
        val ids = getTaskTopicIds(taskId)
        return ids.map { topicService.getTopicDTO(it) }
    }

    fun updateTaskTopics(taskId: IdType, topicIds: List<IdType>) {
        val taskTopics = topicsRelationRepository.findAllByTaskId(taskId)
        val currentTopicIds = taskTopics.map { it.topic!!.id!!.toLong() }
        val toDelete = currentTopicIds.filter { !topicIds.contains(it) }
        val toAdd = topicIds.filter { !currentTopicIds.contains(it) }
        toDelete.forEach {
            val relation =
                topicsRelationRepository.findByTaskIdAndTopicId(taskId, it).orElseThrow {
                    RuntimeException(
                        "TaskTopicsRelation not found, but it was found just a few lines above"
                    )
                }
            relation.deletedAt = LocalDateTime.now()
            topicsRelationRepository.save(relation)
        }
        toAdd.forEach {
            topicService.ensureTopicExists(it)
            val relation =
                TaskTopicsRelation(
                    task = Task().apply { id = taskId },
                    topic = Topic().apply { id = it.toInt() },
                )
            topicsRelationRepository.save(relation)
        }
    }
}
