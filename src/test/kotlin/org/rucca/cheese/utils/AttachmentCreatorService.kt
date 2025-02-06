package org.rucca.cheese.utils

import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.MediaType
import java.io.File
import org.glassfish.jersey.media.multipart.FormDataMultiPart
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart
import org.json.JSONObject
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.persistent.IdType
import org.springframework.stereotype.Service

@Service
class AttachmentCreatorService(private val applicationConfig: ApplicationConfig) {
    fun createAttachment(token: String): IdType {
        val file = File.createTempFile("attachment", ".txt")
        file.writeText("This is a test attachment")
        file.deleteOnExit()
        val client =
            ClientBuilder.newBuilder()
                .register(org.glassfish.jersey.media.multipart.MultiPartFeature::class.java)
                .build()
        val target = client.target(applicationConfig.legacyUrl).path("/attachments")
        val multiPart =
            FormDataMultiPart()
                .field("type", "file")
                .bodyPart(FileDataBodyPart("file", file, MediaType.APPLICATION_OCTET_STREAM_TYPE))
        val response =
            target
                .request(MediaType.MULTIPART_FORM_DATA_TYPE)
                .header("Authorization", "Bearer $token") // 替换为实际的 Bearer token
                .post(Entity.entity(multiPart, multiPart.mediaType))
        val json = JSONObject(response.readEntity(String::class.java))
        return json.getJSONObject("data").getLong("id")
    }
}
