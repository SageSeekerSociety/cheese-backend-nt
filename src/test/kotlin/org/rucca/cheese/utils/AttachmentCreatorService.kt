package org.rucca.cheese.utils

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.persistent.IdType
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

@Service
class AttachmentCreatorService(
    private val applicationConfig: ApplicationConfig,
    private val objectMapper: ObjectMapper,
) {

    private val restTemplate = RestTemplate()

    fun createAttachment(token: String): IdType {
        val file = File.createTempFile("attachment", ".txt")
        file.writeText("This is a test attachment")
        file.deleteOnExit()

        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        headers.set("Authorization", "Bearer $token")

        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("type", "file")
        body.add("file", FileSystemResource(file))

        val requestEntity: HttpEntity<MultiValueMap<String, Any>> = HttpEntity(body, headers)

        val url = "${applicationConfig.legacyUrl}/attachments"
        val response =
            restTemplate.postForObject(url, requestEntity, String::class.java)
                ?: throw IllegalStateException("Failed to get response from legacy system")

        val jsonNode = objectMapper.readTree(response)
        return jsonNode.path("data").path("id").asLong()
    }
}
