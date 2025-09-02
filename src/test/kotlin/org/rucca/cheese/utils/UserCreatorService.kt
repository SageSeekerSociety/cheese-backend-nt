package org.rucca.cheese.utils

import org.rucca.cheese.client.UserClient
import org.rucca.cheese.common.persistent.IdType
import org.springframework.stereotype.Service

/**
 * Legacy service for creating test users. This class is kept for backward compatibility. New code
 * should use UserClient directly.
 */
@Service
class UserCreatorService(private val userClient: UserClient) {
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
        val response = userClient.createUser(username, password, email, nickname, avatarId, intro)
        return CreateUserResponse(
            response.userId,
            response.username,
            response.password,
            response.email,
            response.nickname,
            response.avatarId,
            response.intro,
        )
    }

    /** @return JWT token */
    fun login(username: String, password: String): String {
        return userClient.login(username, password)
    }

    fun testUsername(): String {
        return userClient.testUsername()
    }

    fun testPassword(): String {
        return userClient.testPassword()
    }

    fun testEmail(): String {
        return userClient.testEmail()
    }

    fun testNickname(): String {
        return userClient.testNickname()
    }

    fun testAvatarId(): IdType {
        return userClient.testAvatarId()
    }

    fun testIntro(): String {
        return userClient.testIntro()
    }
}
