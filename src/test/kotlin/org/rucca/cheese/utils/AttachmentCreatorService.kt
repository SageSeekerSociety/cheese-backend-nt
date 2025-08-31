package org.rucca.cheese.utils

import org.rucca.cheese.client.AttachmentClient
import org.rucca.cheese.common.persistent.IdType
import org.springframework.stereotype.Service

@Service
class AttachmentCreatorService(private val attachmentClient: AttachmentClient) {

    fun createAttachment(token: String): IdType {
        return attachmentClient.createAttachment(token)
    }
}
