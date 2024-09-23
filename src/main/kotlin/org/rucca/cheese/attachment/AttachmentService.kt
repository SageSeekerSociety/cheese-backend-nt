package org.rucca.cheese.attachment

import jakarta.ws.rs.client.ClientBuilder
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.AttachmentDTO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AttachmentService(
        private val applicationConfig: ApplicationConfig,
        private val authenticationService: AuthenticationService,
) {
    private val logger = LoggerFactory.getLogger(AttachmentService::class.java)

    class GetAttachmentResponse(
            val code: Int? = null,
            val message: String? = null,
            val data: GetAttachmentResponseData? = null,
    )

    class GetAttachmentResponseData(val attachment: AttachmentDTO? = null)

    fun getAttachmentDto(attachmentId: IdType): AttachmentDTO {
        val client = ClientBuilder.newClient()
        val target = client.target(applicationConfig.legacyUrl).path("/attachments/$attachmentId")
        val token = authenticationService.getToken()
        val response = target.request().header("Authorization", token).get()
        if (response.status != 200) {
            val info = "Failed to get attachment with id $attachmentId from legacy service. Response: $response"
            logger.error(info)
            throw RuntimeException(info)
        }
        return response.readEntity(GetAttachmentResponse::class.java)!!.data!!.attachment!!
    }
}