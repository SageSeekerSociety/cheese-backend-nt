/*
 *  Description: This file implements the TaskTopicsService class.
 *               It is responsible for CRUD of a task's topics.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.task.service

import java.time.LocalDateTime
import org.rucca.cheese.common.config.CacheConfig.Companion.SPACE_HOT_TOPICS_CACHE
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TopicDTO
import org.rucca.cheese.task.TaskRepository
import org.rucca.cheese.task.TaskTopicsRelation
import org.rucca.cheese.task.TaskTopicsRelationRepository
import org.rucca.cheese.topic.TopicRepository
import org.rucca.cheese.topic.TopicService
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class TaskTopicsService(
    private val taskRepository: TaskRepository,
    private val topicRepository: TopicRepository,
    private val topicsRelationRepository: TaskTopicsRelationRepository,
    private val topicService: TopicService,
) {
    fun getTaskTopicIds(taskId: IdType): List<IdType> {
        return topicsRelationRepository.findAllByTaskId(taskId).map { it.topic!!.id!!.toLong() }
    }

    fun getTaskTopicDTOs(taskId: IdType): List<TopicDTO> {
        val ids = getTaskTopicIds(taskId)
        return topicService.getTopicDTOs(ids)
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
                    task = taskRepository.getReferenceById(taskId),
                    topic = topicRepository.getReferenceById(it.toInt()),
                )
            topicsRelationRepository.save(relation)
        }
    }

    @Cacheable(
        value = [SPACE_HOT_TOPICS_CACHE],
        key = "#spaceId + '-' + #limit",
        unless = "#result.isEmpty()",
    )
    fun getSpaceHotTopics(spaceId: IdType, limit: Int): List<TopicDTO> {
        val pageable = PageRequest.of(0, limit)
        val topics = topicsRelationRepository.findHotTopicsBySpaceId(spaceId, pageable)
        return topicService.getTopicDTOs(topics.map { it.id!!.toLong() })
    }

    /** 搜索话题 */
    fun searchSpaceTopics(spaceId: IdType, keyword: String, limit: Int): List<TopicDTO> {
        if (keyword.isBlank()) return emptyList()

        val pageable = PageRequest.of(0, limit)
        val topics = topicsRelationRepository.searchTopicsInSpace(spaceId, keyword, pageable)
        return topicService.getTopicDTOs(topics.map { it.id!!.toLong() })
    }
}
