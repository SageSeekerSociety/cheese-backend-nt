package org.rucca.cheese.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class TopicCreatorService(
    private val applicationConfig: ApplicationConfig,
    private val objectMapper: ObjectMapper, // 注入 ObjectMapper
) {
    private val restTemplate = RestTemplate()

    fun createTopic(token: String, name: String): IdType {
        val url = "${applicationConfig.legacyUrl}/topics"

        // 设置请求头
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("Authorization", "Bearer $token")

        // 构造请求体 Map
        val requestBody: Map<String, String> = mapOf("name" to name)

        // 组装成 HttpEntity
        val requestEntity = HttpEntity(requestBody, headers)

        // 发送 POST 请求
        val response =
            restTemplate.postForObject(url, requestEntity, String::class.java)
                ?: throw IllegalStateException("Failed to get response when creating topic")

        // 使用 ObjectMapper 解析响应
        try {
            val jsonNode = objectMapper.readTree(response)
            return jsonNode.path("data").path("id").asLong()
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse create topic response: $response", e)
        }
    }
}
