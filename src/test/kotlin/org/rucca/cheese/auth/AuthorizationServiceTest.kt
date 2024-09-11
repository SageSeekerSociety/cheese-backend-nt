package org.rucca.cheese.auth

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthorizationServiceTest
@Autowired
constructor(
        private val authorizationService: AuthorizationService,
        private val userCreatorService: UserCreatorService,
) {
    lateinit var token: String

    @BeforeAll
    fun beforeAll() {
        val user = userCreatorService.createUser()
        token = userCreatorService.login(user.username, user.password)
    }

    @Test
    fun testVerify() {
        authorizationService.verify(token)
    }
}
