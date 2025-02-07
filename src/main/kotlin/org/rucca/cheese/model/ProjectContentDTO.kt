package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * 项目富文本内容
 *
 * @param raw 原始markdown内容
 * @param html 渲染后的HTML
 * @param attachments
 */
data class ProjectContentDTO(
    @Schema(example = "null", description = "原始markdown内容")
    @get:JsonProperty("raw")
    val raw: kotlin.String? = null,
    @Schema(example = "null", description = "渲染后的HTML")
    @get:JsonProperty("html")
    val html: kotlin.String? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("attachments")
    val attachments: kotlin.collections.List<AttachmentDTO>? = null,
) {}
