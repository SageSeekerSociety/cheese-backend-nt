package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * 附件
 *
 * @param id 附件 ID
 * @param type 类型
 * @param url 访问 URL
 * @param meta
 */
data class AttachmentDTO(
    @Schema(example = "null", description = "附件 ID")
    @get:JsonProperty("id")
    val id: kotlin.Long? = null,
    @Schema(example = "null", description = "类型")
    @get:JsonProperty("type")
    val type: AttachmentDTO.Type? = null,
    @Schema(example = "null", description = "访问 URL")
    @get:JsonProperty("url")
    val url: kotlin.String? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("meta")
    val meta: AttachmentMetaDTO? = null,
) : Serializable {

    /** 类型 Values: image,video,audio,file */
    enum class Type(@get:JsonValue val value: kotlin.String) {

        image("image"),
        video("video"),
        audio("audio"),
        file("file");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Type {
                return values().first { it -> it.value == value }
            }
        }
    }

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
