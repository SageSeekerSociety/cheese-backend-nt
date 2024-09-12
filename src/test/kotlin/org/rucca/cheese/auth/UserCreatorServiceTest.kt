package org.rucca.cheese.auth

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class UserCreatorServiceTest
@Autowired
constructor(
        private val userCreatorService: UserCreatorService,
) {
    private val logger = LoggerFactory.getLogger(UserCreatorServiceTest::class.java)

    @Test
    fun createUserAndLogin() {
        val response = userCreatorService.createUser()
        val token = userCreatorService.login(response.username, response.password)
        logger.info("Token: $token")
    }
}
