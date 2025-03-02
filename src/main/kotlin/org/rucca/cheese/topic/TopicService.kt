/*
 *  Description: This file implements the TopicService class.
 *               It is responsible for providing topic's DTO.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.topic

import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TopicDTO
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

fun Topic.toDTO() = TopicDTO(id = id!!.toLong(), name = name!!)

@Service
class TopicService(
    private val topicRepository: TopicRepository,
    private val cacheManager: CacheManager,
) {
    fun ensureTopicExists(topicId: IdType) {
        if (!topicRepository.existsById(topicId.toInt())) {
            throw NotFoundError("topic", topicId)
        }
    }

    @Cacheable(cacheNames = ["topics"], key = "#id")
    fun getTopicDTO(id: IdType): TopicDTO {
        return topicRepository
            .findById(id.toInt())
            .orElseThrow { NotFoundError("topic", id) }
            .toDTO()
    }

    fun getTopicDTOs(ids: List<IdType>): List<TopicDTO> {
        if (ids.isEmpty()) return emptyList()

        val idsInt = ids.map { it.toInt() }

        // 首先尝试从缓存获取
        val cache = cacheManager.getCache("topics")
        val result = mutableListOf<TopicDTO>()
        val missingIds = mutableListOf<Int>()

        // 检查哪些ID已缓存
        idsInt.forEach { id ->
            val cachedTopic = cache?.get(id)?.get() as? TopicDTO
            if (cachedTopic != null) {
                result.add(cachedTopic)
            } else {
                missingIds.add(id)
            }
        }

        // 获取未缓存的实体并放入缓存
        if (missingIds.isNotEmpty()) {
            val fetchedTopics = topicRepository.findAllByIdIsIn(missingIds).map { it.toDTO() }
            fetchedTopics.forEach { topic ->
                topic.id.let { id ->
                    cache?.put(id, topic)
                    result.add(topic)
                }
            }
        }

        // 按原始ID顺序排序
        val idToTopicMap = result.associateBy { it.id }
        return ids.mapNotNull { id -> idToTopicMap[id] }
    }
}
