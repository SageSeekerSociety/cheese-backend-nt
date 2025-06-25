package org.rucca.cheese.utils

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
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class UserCreatorService(
    private val applicationConfig: ApplicationConfig,
    private val userRepository: UserRepository,
    private val avatarRepository: AvatarRepository,
    private val userProfileRepository: UserProfileRepository,
    private val objectMapper: ObjectMapper,
    private val passwordEncoder: PasswordEncoder,
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
        val user =
            User()
                .apply {
                    this.username = username
                    this.hashedPassword = passwordEncoder.encode(password)
                    this.email = email
                }
                .let { userRepository.save(it) }
        val userId = user.id!!
        UserProfile()
            .apply {
                this.nickname = nickname
                this.avatar = avatarRepository.getReferenceById(avatarId.toInt())
                this.intro = intro
                this.user = user
            }
            .let { userProfileRepository.save(it) }
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

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val requestBody: Map<String, String> = mapOf("username" to username, "password" to password)

        val requestEntity = HttpEntity(requestBody, headers)

        val response =
            restTemplate.postForObject(url, requestEntity, String::class.java)
                ?: throw RuntimeException("Failed to login: No response from server")

        try {
            val jsonNode = objectMapper.readTree(response)
            return jsonNode.path("data").path("accessToken").asText()
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse login response: $response", e)
        }
    }

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
