package org.rucca.cheese.topic

import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TopicDTO
import org.springframework.stereotype.Service

fun Topic.toDTO() = TopicDTO(id = id!!.toLong(), name = name!!)

@Service
class TopicService(private val topicRepository: TopicRepository) {
    fun ensureTopicExists(topicId: IdType) {
        if (!topicRepository.existsById(topicId.toInt())) {
            throw NotFoundError("topic", topicId)
        }
    }

    fun getTopicDTO(topicId: IdType): TopicDTO {
        val topic =
            topicRepository.findById(topicId.toInt()).orElseThrow {
                NotFoundError("topic", topicId)
            }
        return topic.toDTO()
    }
}
