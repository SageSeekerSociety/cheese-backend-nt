/*
 *  Description: Unit tests for the AuthorizationService class
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.auth

import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.rucca.cheese.client.UserClient
import org.rucca.cheese.common.persistent.IdType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@EnabledIfSystemProperty(named = "cli", matches = "true")
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthorizationServiceTest
@Autowired
constructor(private val jwtService: JwtService, private val userClient: UserClient) {
    var userId: IdType = -1
    lateinit var token: String

    @BeforeAll
    fun prepare() {
        val user = userClient.createUser()
        userId = user.userId
        token = userClient.login(user.username, user.password)
    }

    @Test
    fun testVerify() {
        val authorization = jwtService.verify(token)
        assertEquals(userId, authorization.userId)
    }
}
