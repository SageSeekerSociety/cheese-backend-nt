/*
 *  Description: This file implements the AttachmentService class.
 *               It is responsible for invoking the legacy service to operate on attachments.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.attachment

import jakarta.ws.rs.client.ClientBuilder
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.AttachmentDTO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AttachmentService(
    private val applicationConfig: ApplicationConfig,
    private val jwtService: JwtService,
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
        val token = jwtService.getToken()
        val response = target.request().header("Authorization", token).get()
        if (response.status != 200) {
            val info =
                "Failed to get attachment with id $attachmentId from legacy service. Response: $response"
            logger.error(info)
            throw RuntimeException(info)
        }
        return response.readEntity(GetAttachmentResponse::class.java)!!.data!!.attachment!!
    }
}
