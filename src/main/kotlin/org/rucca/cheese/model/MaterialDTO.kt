package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * 用户上传的资料（图片、视频、音频、文件等）
 *
 * @param id 资料 ID
 * @param type 资料类型
 * @param uploader
 * @param createdAt 创建时间
 * @param expires 过期时间，永不过期则为 undefined
 * @param downloadCount 下载数
 * @param url 资料下载 URL
 * @param meta
 */
data class MaterialDTO(
    @Schema(example = "null", description = "资料 ID")
    @get:JsonProperty("id")
    val id: kotlin.Long? = null,
    @Schema(example = "null", description = "资料类型")
    @get:JsonProperty("type")
    val type: MaterialDTO.Type? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("uploader")
    val uploader: UserDTO? = null,
    @Schema(example = "null", description = "创建时间")
    @get:JsonProperty("created_at")
    val createdAt: kotlin.Long? = null,
    @Schema(example = "null", description = "过期时间，永不过期则为 undefined")
    @get:JsonProperty("expires")
    val expires: kotlin.Long? = null,
    @Schema(example = "null", description = "下载数")
    @get:JsonProperty("download_count")
    val downloadCount: kotlin.Long? = null,
    @Schema(example = "null", description = "资料下载 URL")
    @get:JsonProperty("url")
    val url: kotlin.String? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("meta")
    val meta: AttachmentMetaDTO? = null,
) : Serializable {

    /** 资料类型 Values: image,video,audio,file */
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
