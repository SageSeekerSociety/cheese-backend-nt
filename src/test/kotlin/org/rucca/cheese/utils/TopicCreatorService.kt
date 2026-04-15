package org.rucca.cheese.utils

import org.rucca.cheese.client.TopicClient
import org.rucca.cheese.common.persistent.IdType
import org.springframework.stereotype.Service

/**
 * Legacy service for creating test topics. This class is kept for backward compatibility. New code
 * should use TopicClient directly.
 */
@Service
class TopicCreatorService(private val topicClient: TopicClient) {
    fun createTopic(token: String, name: String): IdType {
        return topicClient.createTopic(token, name)
    }
}
