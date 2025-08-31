package org.rucca.cheese.utils

import org.rucca.cheese.client.UserClient
import org.rucca.cheese.common.persistent.IdType
import org.springframework.stereotype.Service

@Service
class UserCreatorService(private val userClient: UserClient) {

    data class CreateUserResponse(val userId: IdType, val username: String, val password: String)

    fun createUser(): CreateUserResponse {
        val response = userClient.createUser()
        return CreateUserResponse(
            userId = response.userId,
            username = response.username,
            password = response.password,
        )
    }

    fun login(username: String, password: String): String {
        return userClient.login(username, password)
    }

    fun testAvatarId(): IdType {
        return userClient.testAvatarId()
    }
}
