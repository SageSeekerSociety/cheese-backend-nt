package org.rucca.cheese.client

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.persistent.IdType
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

/**
 * Client for creating and managing test attachments. Provides reusable methods for
 * attachment-related operations in tests.
 */
@Service
class AttachmentClient(
    private val applicationConfig: ApplicationConfig,
    private val objectMapper: ObjectMapper,
) {
    private val restTemplate = RestTemplate()

    fun createAttachment(token: String): IdType {
        val file = File.createTempFile("attachment", ".txt")
        file.writeText("This is a test attachment")
        file.deleteOnExit()

        val url = "${applicationConfig.legacyUrl}/attachments"

        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        headers.set("Authorization", "Bearer $token")

        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("type", "file")
        bodyBuilder.part("file", FileSystemResource(file))

        val body: MultiValueMap<String, HttpEntity<*>> = bodyBuilder.build()
        val requestEntity = HttpEntity(body, headers)

        val response =
            restTemplate.postForObject(url, requestEntity, String::class.java)
                ?: throw RuntimeException("Failed to create attachment: No response from server")

        try {
            val jsonNode = objectMapper.readTree(response)
            return jsonNode.path("data").path("id").asLong()
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse attachment creation response: $response", e)
        }
    }
}
