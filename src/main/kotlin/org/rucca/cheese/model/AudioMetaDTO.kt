package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param propertySize 文件大小
 * @param name 文件名称
 * @param mime MIME 类型
 * @param hash 文件哈希
 * @param duration 时长
 */
data class AudioMetaDTO(
    @Schema(example = "null", description = "文件大小")
    @get:JsonProperty("size")
    val propertySize: kotlin.Long? = null,
    @Schema(example = "null", description = "文件名称")
    @get:JsonProperty("name")
    val name: kotlin.String? = null,
    @Schema(example = "null", description = "MIME 类型")
    @get:JsonProperty("mime")
    val mime: kotlin.String? = null,
    @Schema(example = "null", description = "文件哈希")
    @get:JsonProperty("hash")
    val hash: kotlin.String? = null,
    @Schema(example = "null", description = "时长")
    @get:JsonProperty("duration")
    val duration: kotlin.Long? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
