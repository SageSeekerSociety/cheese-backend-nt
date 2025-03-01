/*
 *  Description: This file implements the SpaceClassificationTopicsService class.
 *               It is responsible for managing a space's classification topics.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.space

import java.time.LocalDateTime
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TopicDTO
import org.rucca.cheese.topic.Topic
import org.rucca.cheese.topic.TopicService
import org.rucca.cheese.topic.toDTO
import org.springframework.stereotype.Service

@Service
class SpaceClassificationTopicsService(
    private val topicsRelationRepository: SpaceClassificationTopicsRelationRepository,
    private val topicService: TopicService,
) {
    fun getClassificationTopicIds(spaceId: IdType): List<IdType> {
        return topicsRelationRepository.findAllBySpaceId(spaceId).map { it.topic!!.id!!.toLong() }
    }

    fun getClassificationTopicDTOs(spaceId: IdType): List<TopicDTO> {
        // 使用一次查询获取所有主题关系及其主题数据
        val relationsWithTopics = topicsRelationRepository.findAllBySpaceIdFetchTopic(spaceId)
        
        // 直接从已加载的关系中获取主题并转换为DTO
        return relationsWithTopics.map { relation -> 
            relation.topic!!.toDTO() 
        }
    }

    fun updateClassificationTopics(spaceId: IdType, topicIds: List<IdType>) {
        val current =
            topicsRelationRepository.findAllBySpaceId(spaceId).map { it.topic!!.id!!.toLong() }
        val toDelete = current.filter { !topicIds.contains(it) }
        val toAdd = topicIds.filter { !current.contains(it) }
        toDelete.forEach {
            val relation =
                topicsRelationRepository.findBySpaceIdAndTopicId(spaceId, it).orElseThrow {
                    RuntimeException(
                        "SpaceClassificationTopicsRelation not found, but it was found just a few lines above"
                    )
                }
            relation.deletedAt = LocalDateTime.now()
            topicsRelationRepository.save(relation)
        }
        toAdd.forEach {
            topicService.ensureTopicExists(it)
            val relation =
                SpaceClassificationTopicsRelation(
                    space = Space().apply { id = spaceId },
                    topic = Topic().apply { id = it.toInt() },
                )
            topicsRelationRepository.save(relation)
        }
    }
}
