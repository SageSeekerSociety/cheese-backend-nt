package org.rucca.cheese.common.error

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.springframework.http.HttpStatus

private class ErrorSerializer : JsonSerializer<BaseError>() {
    override fun serialize(err: BaseError, gen: JsonGenerator, serializer: SerializerProvider) {
        gen.writeStartObject()
        gen.writeNumberField("code", err.status.value())
        gen.writeStringField("message", "${err.name}: ${err.message}")
        gen.writeFieldName("error")
        gen.writeStartObject()
        gen.writeStringField("name", err.name)
        gen.writeStringField("message", err.message)
        if (err.data != null) {
            gen.writeObjectField("data", err.data)
        }
        gen.writeEndObject()
        gen.writeEndObject()
    }
}

@JsonSerialize(using = ErrorSerializer::class)
abstract class BaseError(
        val status: HttpStatus,
        val name: String,
        override val message: String,
        val data: Any? = null,
) : Exception("$name: $message")
