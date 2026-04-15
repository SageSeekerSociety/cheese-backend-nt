package org.rucca.cheese.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

/**
 * Client for creating and managing test topics. Provides reusable methods for topic-related
 * operations in tests.
 */
@Service
class TopicClient(
    private val applicationConfig: ApplicationConfig,
    private val objectMapper: ObjectMapper,
) {
    private val restTemplate = RestTemplate()

    fun createTopic(token: String, name: String): IdType {
        val url = "${applicationConfig.legacyUrl}/topics"

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("Authorization", "Bearer $token")

        val requestBody = mapOf("name" to name)
        val requestEntity = HttpEntity(requestBody, headers)

        val response =
            restTemplate.postForObject(url, requestEntity, String::class.java)
                ?: throw RuntimeException("Failed to create topic: No response from server")

        try {
            val jsonNode = objectMapper.readTree(response)
            return jsonNode.path("data").path("id").asLong()
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse topic creation response: $response", e)
        }
    }
}
