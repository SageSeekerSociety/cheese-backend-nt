package org.rucca.cheese.llm.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class ConversationNotFoundError(taskId: IdType, conversationId: String) :
    BaseError(
        HttpStatus.NOT_FOUND,
        "Conversation $conversationId not found",
        mapOf("conversationId" to conversationId, "taskId" to taskId),
    )
