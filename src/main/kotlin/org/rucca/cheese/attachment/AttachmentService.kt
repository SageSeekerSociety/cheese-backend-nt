package org.rucca.cheese.attachment

import com.fasterxml.jackson.databind.ObjectMapper
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.AttachmentDTO
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class AttachmentService(
    private val applicationConfig: ApplicationConfig,
    private val jwtService: JwtService,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(AttachmentService::class.java)

    private val restTemplate = RestTemplate()

    class GetAttachmentResponse(
        val code: Int? = null,
        val message: String? = null,
        val data: GetAttachmentResponseData? = null,
    )

    class GetAttachmentResponseData(val attachment: AttachmentDTO? = null)

    fun getAttachmentDto(attachmentId: IdType): AttachmentDTO {
        val url = "${applicationConfig.legacyUrl}/attachments/$attachmentId"

        val headers = HttpHeaders()
        val token = jwtService.getToken()
        headers.set("Authorization", token)

        val entity = HttpEntity<String>(headers)

        val responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, String::class.java)

        if (responseEntity.statusCode != HttpStatus.OK) {
            val info =
                "Failed to get attachment with id $attachmentId from legacy service. Status: ${responseEntity.statusCode}, Response: ${responseEntity.body}"
            logger.error(info)
            throw RuntimeException(info)
        }

        val responseBody =
            responseEntity.body ?: throw IllegalStateException("Response body is null")

        val attachmentResponse =
            objectMapper.readValue(responseBody, GetAttachmentResponse::class.java)

        return attachmentResponse.data?.attachment
            ?: throw IllegalStateException(
                "Attachment data is missing in the response from legacy service."
            )
    }
}
