package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.Min

/**
 * @param avatarId 头像 id
 * @param id 用户 ID
 * @param intro 个人简介
 * @param nickname 昵称
 * @param username 用户名
 */
data class UserDTO(
    @Schema(example = "null", required = true, description = "头像 id")
    @get:JsonProperty("avatarId", required = true)
    val avatarId: kotlin.Long,
    @get:Min(1L)
    @Schema(example = "null", required = true, description = "用户 ID")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @Schema(example = "null", required = true, description = "个人简介")
    @get:JsonProperty("intro", required = true)
    val intro: kotlin.String = "This user has not set an introduction yet.",
    @Schema(example = "芝士", required = true, description = "昵称")
    @get:JsonProperty("nickname", required = true)
    val nickname: kotlin.String,
    @Schema(example = "cheese", required = true, description = "用户名")
    @get:JsonProperty("username", required = true)
    val username: kotlin.String,
) {}
