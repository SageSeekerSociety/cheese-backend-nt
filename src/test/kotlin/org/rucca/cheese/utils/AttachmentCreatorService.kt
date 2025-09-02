package org.rucca.cheese.utils

import org.rucca.cheese.client.AttachmentClient
import org.rucca.cheese.common.persistent.IdType
import org.springframework.stereotype.Service

/**
 * Legacy service for creating test attachments. This class is kept for backward compatibility. New
 * code should use AttachmentClient directly.
 */
@Service
class AttachmentCreatorService(private val attachmentClient: AttachmentClient) {
    fun createAttachment(token: String): IdType {
        return attachmentClient.createAttachment(token)
    }
}
