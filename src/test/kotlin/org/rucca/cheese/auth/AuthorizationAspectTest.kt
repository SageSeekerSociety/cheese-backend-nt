package org.rucca.cheese.auth

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.rucca.cheese.auth.exception.DuplicatedResourceIdAnnotationException
import org.rucca.cheese.auth.exception.NoGuardOrNoAuthAnnotationException
import org.rucca.cheese.auth.exception.ResourceIdTypeMismatchException
import org.rucca.cheese.common.error.GlobalErrorHandler
import org.rucca.cheese.common.error.InternalServerError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationAspectTest
@Autowired
constructor(
        private val mockMvc: MockMvc,
        @MockkBean private val globalErrorHandler: GlobalErrorHandler,
        @MockkBean private val authorizationService: AuthorizationService,
) {
    @BeforeEach
    fun prepare() {
        // Avoid error logging in the console
        every { globalErrorHandler.handleException(any()) } returns
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(InternalServerError())
    }

    @Test
    fun testNoGuardOrNoAuthAnnotationException() {
        mockMvc.perform(MockMvcRequestBuilders.get("/example/1"))
        verify { globalErrorHandler.handleException(ofType<NoGuardOrNoAuthAnnotationException>()) }
    }

    @Test
    fun testDuplicatedResourceIdAnnotationException() {
        mockMvc.perform(MockMvcRequestBuilders.get("/example/2?id1=1&id2=2"))
        verify { globalErrorHandler.handleException(ofType<DuplicatedResourceIdAnnotationException>()) }
    }

    @Test
    fun testResourceIdTypeMismatchException() {
        mockMvc.perform(MockMvcRequestBuilders.get("/example/3?id=1"))
        verify { globalErrorHandler.handleException(ofType<ResourceIdTypeMismatchException>()) }
    }

    @Test
    fun testNoIdWithoutToken() {
        mockMvc.perform(MockMvcRequestBuilders.get("/example/4"))
        verify { authorizationService.audit(null, "query", "example", null) }
    }

    @Test
    fun testNoIdTokenInCapital() {
        mockMvc.perform(MockMvcRequestBuilders.get("/example/4").header("Authorization", "Bearer token Xxx"))
        verify { authorizationService.audit("Bearer token Xxx", "query", "example", null) }
    }

    @Test
    fun testNoIdTokenNotInCapital() {
        mockMvc.perform(MockMvcRequestBuilders.get("/example/4").header("authorization", "Bearer token Xxx"))
        verify { authorizationService.audit("Bearer token Xxx", "query", "example", null) }
    }

    @Test
    fun testWithId() {
        mockMvc.perform(MockMvcRequestBuilders.get("/example/5?id=123").header("authorization", "Bearer token Xxx"))
        verify { authorizationService.audit("Bearer token Xxx", "query", "example", 123) }
    }

    @Test
    fun testMvcController() {
        mockMvc.perform(MockMvcRequestBuilders.get("/example/6?id=123").header("authorization", "Bearer token Xxx"))
        verify { authorizationService.audit("Bearer token Xxx", "query", "example", 123) }
    }
}
