/*
 *  Description: Unit tests for the UserCreatorService class
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.auth

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.rucca.cheese.client.UserClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@Disabled("Disabled to speed up tests")
@SpringBootTest
@ActiveProfiles("test")
class UserClientTest @Autowired constructor(private val userClient: UserClient) {
    private val logger = LoggerFactory.getLogger(UserClientTest::class.java)

    @Test
    fun createUserAndLogin() {
        val response = userClient.createUser()
        val token = userClient.login(response.username, response.password)
        logger.info("Token: $token")
    }
}
