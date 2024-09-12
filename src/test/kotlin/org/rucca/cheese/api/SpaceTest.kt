package org.rucca.cheese.api

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.rucca.cheese.auth.UserCreatorService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
class SpaceTest
@Autowired
constructor(
        private val mockMvc: MockMvc,
        private val userCreatorService: UserCreatorService,
) {
    lateinit var user: UserCreatorService.CreateUserResponse
    lateinit var token: String

    @BeforeAll
    fun prepare() {
        user = userCreatorService.createUser()
        token = userCreatorService.login(user.username, user.password)
    }

    @Test
    fun testGetSpaceAndNotFound() {
        val request = MockMvcRequestBuilders.get("/spaces/-1").header("Authorization", "Bearer $token")
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.name").value("NotFoundError"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.type").value("space"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.id").value("-1"))
    }
}
