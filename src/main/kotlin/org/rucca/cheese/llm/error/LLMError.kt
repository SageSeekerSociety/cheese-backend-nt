package org.rucca.cheese.llm.error

import org.rucca.cheese.common.error.BaseError
import org.springframework.http.HttpStatus

sealed class LLMError(status: HttpStatus, message: String, data: Any? = null) :
    BaseError(status, message, data) {

    class QuotaExceededError(data: Any? = null) :
        LLMError(
            HttpStatus.TOO_MANY_REQUESTS,
            "Daily AI usage quota exceeded. Please try again tomorrow.",
            data,
        )

    class SystemBusyError(data: Any? = null) :
        LLMError(
            HttpStatus.SERVICE_UNAVAILABLE,
            "System is busy processing requests. Please try again later.",
            data,
        )

    class InvalidResponseError(data: Any? = null) :
        LLMError(HttpStatus.BAD_GATEWAY, "Invalid response format from LLM service.", data)

    class ServiceError(
        message: String = "LLM service encountered an error. Please try again later.",
        data: Any? = null,
    ) : LLMError(HttpStatus.BAD_GATEWAY, message, data)

    class QuotaUpdateError(data: Any? = null) :
        LLMError(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update AI usage quota.", data)

    class QuotaResetError(data: Any? = null) :
        LLMError(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to reset AI usage quota.", data)

    class RequestTimeoutError(data: Any? = null) :
        LLMError(HttpStatus.GATEWAY_TIMEOUT, "Request to LLM service timed out.", data)

    class InvalidRequestError(data: Any? = null) :
        LLMError(HttpStatus.BAD_REQUEST, "Invalid request parameters for LLM service.", data)

    class AdviceNotReadyError(status: String, data: Any? = null) :
        LLMError(
            HttpStatus.BAD_REQUEST,
            "AI advice generation is still in progress. Current status: $status",
            data,
        )

    class ModelNotFoundError(modelType: String, data: Any? = null) :
        LLMError(
            HttpStatus.BAD_REQUEST,
            "Specified model type '$modelType' does not exist. Please use a valid model type.",
            data,
        )
}
