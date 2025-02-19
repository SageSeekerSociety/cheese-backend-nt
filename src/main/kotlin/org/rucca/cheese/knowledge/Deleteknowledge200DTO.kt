package org.rucca.cheese.model
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
/**
 * @param code
 * @param message
 */
data class DeleteKnowledge200ResponseDTO(
    @Schema(example = "200", required = true, description = "状态码,200表示删除成功")
    @get:JsonProperty("code", required = true)
    val code: kotlin.Int,
    @Schema(example = "Knowledge item deleted successfully", required = true, description = "删除操作的消息")
    @get:JsonProperty("message", required = true)
    val message: kotlin.String,
)
