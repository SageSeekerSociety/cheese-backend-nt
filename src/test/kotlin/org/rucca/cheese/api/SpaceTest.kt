package org.rucca.cheese.api

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@AutoConfigureMockMvc
class SpaceTest @Autowired constructor(private val mockMvc: MockMvc) {
    @Test
    fun testGetSpaceAndNotFound() {
        mockMvc.perform(MockMvcRequestBuilders.get("/spaces/-1"))
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.name").value("NotFoundError"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.type").value("space"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.id").value("-1"))
    }
}
