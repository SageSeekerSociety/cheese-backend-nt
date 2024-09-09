package org.rucca.cheese.auth

import java.util.logging.Logger
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class UserCreatorServiceTest {
    @Autowired lateinit var userCreatorService: UserCreatorService
    val logger = Logger.getLogger(UserCreatorServiceTest::class.java.name)

    @Test
    fun createUserAndLogin() {
        val response = userCreatorService.createUser()
        val token = userCreatorService.login(response.username, response.password)
        logger.info("Token: $token")
    }
}
