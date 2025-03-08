package org.rucca.cheese.llm.error

import org.rucca.cheese.common.error.BaseError
import org.springframework.http.HttpStatus

class ConversationNotFoundError(conversationId: String) :
    BaseError(
        HttpStatus.NOT_FOUND,
        "Conversation $conversationId not found",
        mapOf("conversationId" to conversationId),
    )
