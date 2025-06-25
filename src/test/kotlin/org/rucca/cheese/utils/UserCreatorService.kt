package org.rucca.cheese.utils

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.math.floor
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.AvatarRepository
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserProfile
import org.rucca.cheese.user.UserProfileRepository
import org.rucca.cheese.user.UserRepository
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class UserCreatorService(
    private val applicationConfig: ApplicationConfig,
    private val userRepository: UserRepository,
    private val avatarRepository: AvatarRepository,
    private val userProfileRepository: UserProfileRepository,
    private val objectMapper: ObjectMapper, // 注入 ObjectMapper
) {
    private val restTemplate = RestTemplate()

    class CreateUserResponse(
        val userId: IdType,
        val username: String,
        val password: String,
        val email: String,
        val nickname: String,
        val avatarId: IdType,
        val intro: String,
        val isLegacyAuth: Boolean = false,
    )

    fun createUser(
        username: String = testUsername(),
        password: String = testPassword(),
        email: String = testEmail(),
        nickname: String = testNickname(),
        avatarId: IdType = testAvatarId(),
        intro: String = testIntro(),
    ): CreateUserResponse {
        val user = User()
        user.username = username
        user.hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        user.email = email
        val userId = userRepository.save(user).id!!
        val userProfile = UserProfile()
        userProfile.nickname = nickname
        userProfile.avatar = avatarRepository.getReferenceById(1)
        userProfile.intro = intro
        userProfile.user = user
        userProfileRepository.save(userProfile)
        return CreateUserResponse(
            userId.toLong(),
            username,
            password,
            email,
            nickname,
            avatarId,
            intro,
        )
    }

    /** @return JWT token */
    fun login(username: String, password: String): String {
        val url = "${applicationConfig.legacyUrl}/users/auth/login"

        // 设置请求头
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        // 构造请求体 Map
        val requestBody: Map<String, String> = mapOf("username" to username, "password" to password)

        // 组装成 HttpEntity
        val requestEntity = HttpEntity(requestBody, headers)

        // 发送 POST 请求并获取响应
        val response =
            restTemplate.postForObject(url, requestEntity, String::class.java)
                ?: throw RuntimeException("Failed to login: No response from server")

        // 使用 ObjectMapper 解析响应
        try {
            val jsonNode = objectMapper.readTree(response)
            return jsonNode.path("data").path("accessToken").asText()
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse login response: $response", e)
        }
    }

    // ... test* 方法保持不变 ...
    fun testUsername(): String {
        return "NTTestUsername-${floor(Math.random() * 10000000000).toLong()}"
    }

    fun testPassword(): String {
        return "abc123456!!!"
    }

    fun testEmail(): String {
        return "test-${floor(Math.random() * 10000000000).toLong()}@ruc.edu.cn"
    }

    fun testNickname(): String {
        return "test_user"
    }

    fun testAvatarId(): IdType {
        return 1
    }

    fun testIntro(): String {
        return "This user has not set an introduction yet."
    }
}
