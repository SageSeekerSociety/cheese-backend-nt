package org.rucca.cheese.auth.spring

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import jakarta.servlet.http.HttpServletRequest
import java.lang.reflect.Method
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rucca.cheese.auth.context.PermissionContextProvider
import org.rucca.cheese.auth.context.PermissionContextProviderFactory
import org.rucca.cheese.auth.core.*
import org.rucca.cheese.auth.model.AuthUserInfo
import org.rucca.cheese.auth.registry.ActionRegistry
import org.rucca.cheese.auth.registry.ResourceRegistry
import org.rucca.cheese.common.persistent.IdType
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@ExtendWith(MockKExtension::class)
@DisplayName("Security Aspect Tests")
class SecurityAspectTest {

    // Test domains, actions, and resources
    private val testDomain =
        object : Domain {
            override val name: String = "test"
        }

    private val viewAction =
        object : Action {
            override val actionId: String = "view"
            override val domain: Domain = testDomain
        }

    private val documentResource =
        object : ResourceType {
            override val typeName: String = "document"
            override val domain: Domain = testDomain
        }

    // Use a real Class object instead of mocking
    private val testClass = SecurityAspectTest::class.java

    // Use mockk() function to explicitly create mock object instead of @MockK annotation
    private val method = mockk<Method>()

    @MockK private lateinit var permissionEvaluator: PermissionEvaluator

    @MockK private lateinit var actionRegistry: ActionRegistry

    @MockK private lateinit var resourceRegistry: ResourceRegistry

    @MockK private lateinit var contextProviderFactory: PermissionContextProviderFactory

    @MockK private lateinit var permissionContextProvider: PermissionContextProvider

    @MockK private lateinit var joinPoint: ProceedingJoinPoint

    @MockK private lateinit var methodSignature: MethodSignature

    @MockK private lateinit var request: HttpServletRequest

    @MockK private lateinit var servletRequestAttributes: ServletRequestAttributes

    private lateinit var securityAspect: SecurityAspect
    private val userId: IdType = 123L
    private val expectedResult = "Test Result"

    @BeforeEach
    fun setUp() {
        // Set the declaringClass for Method object
        every { method.declaringClass } returns testClass

        securityAspect =
            SecurityAspect(
                permissionEvaluator,
                actionRegistry,
                resourceRegistry,
                contextProviderFactory,
            )

        // Setup RequestContextHolder
        mockkStatic(RequestContextHolder::class)
        every { RequestContextHolder.getRequestAttributes() } returns servletRequestAttributes
        every { servletRequestAttributes.request } returns request

        // Setup JoinPoint
        every { joinPoint.signature } returns methodSignature
        every { methodSignature.method } returns method
        every { joinPoint.proceed() } returns expectedResult
    }

    @Nested
    @DisplayName("Tests for @Secure annotation")
    inner class SecureAnnotationTests {
        @MockK private lateinit var secureAnnotation: Secure

        @BeforeEach
        fun setUpSecureTest() {
            // Mock AnnotationUtils
            mockkStatic(AnnotationUtils::class)
            every { AnnotationUtils.findAnnotation(method, Secure::class.java) } returns
                secureAnnotation
            every { AnnotationUtils.findAnnotation(method, SkipSecurity::class.java) } returns null
            every { AnnotationUtils.findAnnotation(testClass, SkipSecurity::class.java) } returns
                null

            // Setup Secure annotation values
            every { secureAnnotation.domain } returns "test"
            every { secureAnnotation.action } returns "view"
            every { secureAnnotation.resource } returns "document"
        }

        @Test
        @DisplayName("Should proceed when permission is granted")
        fun shouldProceedWhenPermissionIsGranted() {
            // Setup registry and permission evaluation
            every { actionRegistry.getAction("test", "view") } returns viewAction
            every { resourceRegistry.getResource("test", "document") } returns documentResource
            every { contextProviderFactory.getProvider("test") } returns permissionContextProvider
            every { permissionContextProvider.getContext("document", any()) } returns emptyMap()

            // Setup parameters
            every { joinPoint.args } returns emptyArray()
            every { method.parameterAnnotations } returns emptyArray()

            // Setup user info
            every { request.getAttribute("userId") } returns userId
            every { request.getAttribute("userRole") } returns emptySet<Role>()

            // Setup permission evaluation result
            every {
                permissionEvaluator.evaluate(
                    match<AuthUserInfo> { it.userId == userId },
                    any<Permission<Action, ResourceType>>(),
                    null,
                    any(),
                )
            } returns true

            // Execute method
            val result = securityAspect.checkSecurityWithSecure(joinPoint)

            // Verify result
            assertEquals(expectedResult, result)

            // Verify calls
            verify { joinPoint.proceed() }
            verify {
                permissionEvaluator.evaluate(
                    match<AuthUserInfo> { it.userId == userId },
                    any<Permission<Action, ResourceType>>(),
                    null,
                    any(),
                )
            }
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when permission is denied")
        fun shouldThrowExceptionWhenPermissionIsDenied() {
            // Setup registry and permission evaluation
            every { actionRegistry.getAction("test", "view") } returns viewAction
            every { resourceRegistry.getResource("test", "document") } returns documentResource
            every { contextProviderFactory.getProvider("test") } returns permissionContextProvider
            every { permissionContextProvider.getContext("document", any()) } returns emptyMap()

            // Setup parameters
            every { joinPoint.args } returns emptyArray()
            every { method.parameterAnnotations } returns emptyArray()

            // Setup user info
            every { request.getAttribute("userId") } returns userId
            every { request.getAttribute("userRole") } returns emptySet<Role>()

            // Setup permission evaluation result - access denied
            every {
                permissionEvaluator.evaluate(
                    match<AuthUserInfo> { it.userId == userId },
                    any<Permission<Action, ResourceType>>(),
                    null,
                    any(),
                )
            } returns false

            // Execute and verify exception
            val exception =
                assertThrows(AccessDeniedError::class.java) {
                    securityAspect.checkSecurityWithSecure(joinPoint)
                }

            // Verify error message
            assertEquals(
                "Access denied for user $userId to perform view on document",
                exception.message,
            )

            // Verify calls
            verify(exactly = 0) { joinPoint.proceed() }
        }

        @Test
        @DisplayName("Should skip security check when SkipSecurity annotation is present")
        fun shouldSkipSecurityCheckWhenSkipSecurityAnnotationIsPresent() {
            // Setup SkipSecurity annotation
            every { AnnotationUtils.findAnnotation(method, SkipSecurity::class.java) } returns
                mockk()

            // Execute method
            val result = securityAspect.checkSecurityWithSecure(joinPoint)

            // Verify result
            assertEquals(expectedResult, result)

            // Verify calls
            verify { joinPoint.proceed() }
            verify(exactly = 0) {
                permissionEvaluator.evaluate(
                    any(),
                    any<Permission<Action, ResourceType>>(),
                    any(),
                    any(),
                )
            }
        }
    }

    @Nested
    @DisplayName("Tests for @Auth annotation")
    inner class AuthAnnotationTests {
        @MockK private lateinit var authAnnotation: Auth

        @BeforeEach
        fun setUpAuthTest() {
            // Mock AnnotationUtils
            mockkStatic(AnnotationUtils::class)
            every { AnnotationUtils.findAnnotation(method, Auth::class.java) } returns
                authAnnotation
            every { AnnotationUtils.findAnnotation(method, SkipSecurity::class.java) } returns null
            every { AnnotationUtils.findAnnotation(testClass, SkipSecurity::class.java) } returns
                null

            // Setup Auth annotation value
            every { authAnnotation.value } returns "test:view:document"
        }

        @Test
        @DisplayName("Should proceed when permission is granted")
        fun shouldProceedWhenPermissionIsGranted() {
            // Setup registry and permission evaluation
            every { actionRegistry.getAction("test", "view") } returns viewAction
            every { resourceRegistry.getResource("test", "document") } returns documentResource
            every { contextProviderFactory.getProvider("test") } returns permissionContextProvider
            every { permissionContextProvider.getContext("document", any()) } returns emptyMap()

            // Setup parameters
            every { joinPoint.args } returns emptyArray()
            every { method.parameterAnnotations } returns emptyArray()

            // Setup user info
            every { request.getAttribute("userId") } returns userId
            every { request.getAttribute("userRole") } returns emptySet<Role>()

            // Setup permission evaluation result
            every {
                permissionEvaluator.evaluate(
                    match<AuthUserInfo> { it.userId == userId },
                    any<Permission<Action, ResourceType>>(),
                    null,
                    any(),
                )
            } returns true

            // Execute method
            val result = securityAspect.checkSecurityWithAuth(joinPoint)

            // Verify result
            assertEquals(expectedResult, result)

            // Verify calls
            verify { joinPoint.proceed() }
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when Auth value format is invalid")
        fun shouldThrowExceptionWhenAuthValueFormatIsInvalid() {
            // Setup invalid Auth annotation value
            every { authAnnotation.value } returns "test:view" // Missing resource part

            // Execute and verify exception
            val exception =
                assertThrows(IllegalArgumentException::class.java) {
                    securityAspect.checkSecurityWithAuth(joinPoint)
                }

            // Verify error message
            assertEquals(
                "Invalid Auth value format: test:view. Expected format: 'domain:action:resource'",
                exception.message,
            )
        }
    }

    @Nested
    @DisplayName("Resource ID extraction tests")
    inner class ResourceIdExtractionTests {
        @MockK private lateinit var secureAnnotation: Secure

        @BeforeEach
        fun setUp() {
            // Mock AnnotationUtils
            mockkStatic(AnnotationUtils::class)
            every { AnnotationUtils.findAnnotation(method, Secure::class.java) } returns
                secureAnnotation
            every { AnnotationUtils.findAnnotation(method, SkipSecurity::class.java) } returns null
            every { AnnotationUtils.findAnnotation(testClass, SkipSecurity::class.java) } returns
                null

            // Setup Secure annotation values
            every { secureAnnotation.domain } returns "test"
            every { secureAnnotation.action } returns "view"
            every { secureAnnotation.resource } returns "document"

            // Basic setup
            every { actionRegistry.getAction("test", "view") } returns viewAction
            every { resourceRegistry.getResource("test", "document") } returns documentResource
            every { contextProviderFactory.getProvider("test") } returns permissionContextProvider
            every { permissionContextProvider.getContext("document", any()) } returns emptyMap()

            // Setup user info
            every { request.getAttribute("userId") } returns userId
            every { request.getAttribute("userRole") } returns emptySet<Role>()

            // Setup permission evaluation result
            every {
                permissionEvaluator.evaluate(
                    any(),
                    any<Permission<Action, ResourceType>>(),
                    any(),
                    any(),
                )
            } returns true
        }

        @Test
        @DisplayName("Should correctly extract parameter with @ResourceId annotation")
        fun shouldExtractResourceIdFromAnnotatedParameter() {
            val resourceId: IdType = 456L

            // Create parameters with @ResourceId annotation
            val parameterAnnotations =
                arrayOf(arrayOf<Annotation>(), arrayOf<Annotation>(mockk<ResourceId>()))

            // Setup method parameters and annotations
            every { method.parameterAnnotations } returns parameterAnnotations
            every { joinPoint.args } returns arrayOf("someValue", resourceId)

            // Execute method
            securityAspect.checkSecurityWithSecure(joinPoint)

            // Verify calls with correct resourceId
            verify {
                permissionEvaluator.evaluate(
                    any(),
                    any<Permission<Action, ResourceType>>(),
                    resourceId,
                    any(),
                )
            }
        }
    }

    @Nested
    @DisplayName("Context extraction tests")
    inner class ContextExtractionTests {
        @MockK private lateinit var secureAnnotation: Secure

        @MockK private lateinit var authContextAnnotation: AuthContext

        @BeforeEach
        fun setUp() {
            // Mock AnnotationUtils
            mockkStatic(AnnotationUtils::class)
            every { AnnotationUtils.findAnnotation(method, Secure::class.java) } returns
                secureAnnotation
            every { AnnotationUtils.findAnnotation(method, SkipSecurity::class.java) } returns null
            every { AnnotationUtils.findAnnotation(testClass, SkipSecurity::class.java) } returns
                null

            // Setup Secure annotation values
            every { secureAnnotation.domain } returns "test"
            every { secureAnnotation.action } returns "view"
            every { secureAnnotation.resource } returns "document"

            // Basic setup
            every { actionRegistry.getAction("test", "view") } returns viewAction
            every { resourceRegistry.getResource("test", "document") } returns documentResource
            every { contextProviderFactory.getProvider("test") } returns permissionContextProvider
            every { permissionContextProvider.getContext("document", any()) } returns emptyMap()

            // Setup user info
            every { request.getAttribute("userId") } returns userId
            every { request.getAttribute("userRole") } returns emptySet<Role>()

            // Setup permission evaluation result
            every {
                permissionEvaluator.evaluate(
                    any(),
                    any<Permission<Action, ResourceType>>(),
                    any(),
                    any(),
                )
            } returns true
        }

        @Test
        @DisplayName("Should correctly extract parameter with @AuthContext annotation")
        fun shouldExtractContextFromAnnotatedParameter() {
            // Create parameters with @AuthContext annotation
            val parameterAnnotations =
                arrayOf(arrayOf<Annotation>(mockk<AuthContext>()), arrayOf<Annotation>())

            // Setup @AuthContext annotation properties
            every { authContextAnnotation.key } returns "projectId"
            every { authContextAnnotation.field } returns ""

            // Setup method.parameterAnnotations to return our test annotations
            val authContextAnnotations = arrayOf(authContextAnnotation)
            every { method.parameterAnnotations } returns
                arrayOf(authContextAnnotations, emptyArray())

            // Setup method parameters
            val projectId: IdType = 789L
            every { joinPoint.args } returns arrayOf(projectId, "someOtherValue")

            // Capture context passed to permissionEvaluator
            val contextCaptor = slot<Map<String, Any>>()

            // Execute method
            securityAspect.checkSecurityWithSecure(joinPoint)

            // Verify calls
            verify {
                permissionEvaluator.evaluate(
                    any(),
                    any<Permission<Action, ResourceType>>(),
                    any(),
                    capture(contextCaptor),
                )
            }

            // Verify context contains project ID
            val capturedContext = contextCaptor.captured
            assertEquals(projectId, capturedContext["projectId"])
        }
    }
}
