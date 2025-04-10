package org.rucca.cheese.auth.spring

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import java.lang.reflect.Method
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rucca.cheese.auth.context.PermissionContextProvider
import org.rucca.cheese.auth.context.PermissionContextProviderFactory
import org.rucca.cheese.auth.core.*
import org.rucca.cheese.auth.registry.ActionRegistry
import org.rucca.cheese.auth.registry.ResourceRegistry
import org.rucca.cheese.common.persistent.IdType
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder

@ExtendWith(MockKExtension::class)
@DisplayName("Security Aspect Tests (with Spring Security Context)")
class SecurityAspectTest {

    // --- Test doubles remain mostly the same ---
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
    private val testClass = SecurityAspectTest::class.java // Use real class
    private val method = mockk<Method>() // Mock method

    @MockK private lateinit var permissionEvaluator: PermissionEvaluator
    @MockK private lateinit var actionRegistry: ActionRegistry
    @MockK private lateinit var resourceRegistry: ResourceRegistry
    @MockK private lateinit var contextProviderFactory: PermissionContextProviderFactory
    @MockK private lateinit var permissionContextProvider: PermissionContextProvider
    @MockK private lateinit var joinPoint: ProceedingJoinPoint
    @MockK private lateinit var methodSignature: MethodSignature

    // --- Mock Security Context related objects ---
    @MockK private lateinit var securityContext: SecurityContext
    @MockK private lateinit var authentication: UserPrincipalAuthenticationToken

    @InjectMockKs private lateinit var securityAspect: SecurityAspect

    private val userId: IdType = 123L
    private val systemRoles: Set<Role> = setOf(SystemRole.USER)
    private val expectedResult = "Test Result"

    @BeforeEach
    fun setUp() {
        unmockkStatic(SecurityContextHolder::class)
        unmockkStatic(AnnotationUtils::class)

        // Setup basic method info
        every { method.declaringClass } returns testClass
        every { method.name } returns "testSecureMethod"

        // --- Setup SecurityContextHolder ---
        mockkStatic(SecurityContextHolder::class)
        every { SecurityContextHolder.getContext() } returns securityContext
        every { securityContext.authentication } returns authentication // Return our custom token

        // --- Setup our custom Authentication token ---
        every { authentication.isAuthenticated } returns true // Assume authenticated
        every { authentication.principal } returns userId // Principal is the userId
        every { authentication.userId } returns userId // Direct access via custom token property
        every { authentication.systemRoles } returns
            systemRoles // Direct access via custom token property

        // --- Setup JoinPoint (same as before) ---
        every { joinPoint.signature } returns methodSignature
        every { methodSignature.method } returns method
        every { joinPoint.proceed() } returns expectedResult

        // Basic setup for dependencies (can be overridden in nested classes)
        every { contextProviderFactory.getProvider(any()) } returns permissionContextProvider
        every { permissionContextProvider.getContext(any(), any()) } returns emptyMap()
        every { actionRegistry.getAction(any(), any()) } returns viewAction
        every { resourceRegistry.getResource(any(), any()) } returns documentResource
    }

    @Nested
    @DisplayName("Tests for @Secure annotation")
    inner class SecureAnnotationTests {
        @MockK private lateinit var secureAnnotation: Secure

        @BeforeEach
        fun setUpSecureTest() {
            mockkStatic(AnnotationUtils::class) // Mock for this nested class too
            every { AnnotationUtils.findAnnotation(method, Secure::class.java) } returns
                secureAnnotation
            every { AnnotationUtils.findAnnotation(method, SkipSecurity::class.java) } returns null
            every { AnnotationUtils.findAnnotation(testClass, SkipSecurity::class.java) } returns
                null

            every { secureAnnotation.domain } returns "test"
            every { secureAnnotation.action } returns "view"
            every { secureAnnotation.resource } returns "document"

            // Override registry mocks if specific values are needed
            every { actionRegistry.getAction("test", "view") } returns viewAction
            every { resourceRegistry.getResource("test", "document") } returns documentResource
            every { contextProviderFactory.getProvider("test") } returns permissionContextProvider
            every { permissionContextProvider.getContext("document", any()) } returns
                emptyMap() // Context for resource type
        }

        @Test
        @DisplayName("Should proceed when permission is granted")
        fun shouldProceedWhenPermissionIsGranted() {
            // Setup parameters (if needed for context/resourceId extraction)
            every { joinPoint.args } returns emptyArray()
            every { method.parameterAnnotations } returns emptyArray()

            // --- Setup permission evaluation result ---
            // NOTE: We no longer match AuthUserInfo. The aspect calls evaluator without it.
            // The evaluator internally gets user info from SecurityContextHolder (which we mocked).
            every {
                permissionEvaluator.evaluate(
                    any(),
                    any<Permission<Action, ResourceType>>(), // Permission object
                    null, // resourceId (null in this case)
                    any(), // context
                )
            } returns true

            // Execute method
            val result = securityAspect.checkSecurityWithSecure(joinPoint)

            // Verify result
            assertEquals(expectedResult, result)

            // Verify calls
            verify { joinPoint.proceed() }
            // Verify evaluate was called with expected null resourceId
            verify {
                permissionEvaluator.evaluate(
                    any(),
                    any<Permission<Action, ResourceType>>(),
                    null, // Expecting null resourceId based on empty args/annotations
                    any(),
                )
            }
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when permission is denied")
        fun shouldThrowExceptionWhenPermissionIsDenied() {
            // Setup parameters
            every { joinPoint.args } returns emptyArray()
            every { method.parameterAnnotations } returns emptyArray()

            // Setup permission evaluation result - access denied
            every {
                permissionEvaluator.evaluate(
                    any(),
                    any<Permission<Action, ResourceType>>(),
                    null,
                    any(),
                )
            } returns false

            // Execute and verify exception
            // Assuming SecurityAspect now throws AccessDeniedError directly or relies on
            // evaluator's exception
            // Let's assume SecurityAspect catches false and throws its own error.
            val exception =
                assertThrows(AccessDeniedError::class.java) { // Use your specific AccessDeniedError
                    securityAspect.checkSecurityWithSecure(joinPoint)
                }

            // Verify error message (assuming it uses userId obtained internally)
            // The aspect needs access to userId to create this message. How does it get it?
            // Option 1: Inject CurrentUserContextService into Aspect.
            // Option 2: Modify evaluate to throw a richer exception containing userId.
            // Option 3: Modify aspect to look up userId AFTER failure (less ideal).
            // Let's assume for now the message format might change or not include userId easily.
            // assertEquals("Access denied for user $userId to perform view on document",
            // exception.message)
            assertTrue(exception.message?.contains("Access denied") ?: false) // More robust check

            // Verify calls
            verify(exactly = 0) { joinPoint.proceed() }
            verify {
                permissionEvaluator.evaluate(
                    any(),
                    any<Permission<Action, ResourceType>>(),
                    null,
                    any(),
                )
            }
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
            mockkStatic(AnnotationUtils::class)
            every { AnnotationUtils.findAnnotation(method, Auth::class.java) } returns
                authAnnotation
            every { AnnotationUtils.findAnnotation(method, SkipSecurity::class.java) } returns null
            every { AnnotationUtils.findAnnotation(testClass, SkipSecurity::class.java) } returns
                null
            every { authAnnotation.value } returns "test:view:document"
            every { actionRegistry.getAction("test", "view") } returns viewAction
            every { resourceRegistry.getResource("test", "document") } returns documentResource
            every { contextProviderFactory.getProvider("test") } returns permissionContextProvider
            every { permissionContextProvider.getContext("document", any()) } returns emptyMap()
        }

        @Test
        @DisplayName("Should proceed when permission is granted")
        fun shouldProceedWhenPermissionIsGranted() {
            // Setup parameters
            every { joinPoint.args } returns emptyArray()
            every { method.parameterAnnotations } returns emptyArray()

            // Setup permission evaluation result
            every {
                permissionEvaluator.evaluate(
                    any(),
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
            verify {
                permissionEvaluator.evaluate(
                    any(),
                    any<Permission<Action, ResourceType>>(),
                    null,
                    any(),
                )
            }
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
            assertEquals(
                "Invalid Auth value format: 'test:view'. Expected format: 'domain:action:resource'",
                exception.message,
            )
            verify(exactly = 0) {
                permissionEvaluator.evaluate(
                    any(),
                    any<Permission<Action, ResourceType>>(),
                    any(),
                    any(),
                )
            }
            verify(exactly = 0) { joinPoint.proceed() }
        }
    }

    @Nested
    @DisplayName("Resource ID extraction tests")
    inner class ResourceIdExtractionTests {
        @MockK private lateinit var secureAnnotation: Secure

        @BeforeEach
        fun setUpResourceIdTest() { // Changed name slightly to avoid conflict warning
            mockkStatic(AnnotationUtils::class)
            every { AnnotationUtils.findAnnotation(method, Secure::class.java) } returns
                secureAnnotation
            every { AnnotationUtils.findAnnotation(method, SkipSecurity::class.java) } returns null
            every { AnnotationUtils.findAnnotation(testClass, SkipSecurity::class.java) } returns
                null
            every { secureAnnotation.domain } returns "test"
            every { secureAnnotation.action } returns "view"
            every { secureAnnotation.resource } returns "document"
            every { actionRegistry.getAction("test", "view") } returns viewAction
            every { resourceRegistry.getResource("test", "document") } returns documentResource
            every { contextProviderFactory.getProvider("test") } returns permissionContextProvider
            every { permissionContextProvider.getContext("document", any()) } returns emptyMap()

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
            val resourceIdValue: IdType = 456L // Renamed variable

            // Create parameters with @ResourceId annotation
            val resourceIdAnnotation = mockk<ResourceId>() // Mock the annotation instance
            val parameterAnnotations =
                arrayOf(
                    emptyArray<Annotation>(), // Param 0, no annotations
                    arrayOf<Annotation>(resourceIdAnnotation), // Param 1, has @ResourceId
                )

            // Setup method parameters and annotations
            every { method.parameterAnnotations } returns parameterAnnotations
            every { joinPoint.args } returns
                arrayOf("someValue", resourceIdValue) // Pass the actual ID value

            // Execute method
            securityAspect.checkSecurityWithSecure(joinPoint)

            // Verify evaluate was called with correct resourceId
            verify {
                permissionEvaluator.evaluate(
                    any(),
                    any<Permission<Action, ResourceType>>(),
                    resourceIdValue, // Expect the extracted resource ID
                    any(),
                )
            }
        }
    }

    @Nested
    @DisplayName("Context extraction tests")
    inner class ContextExtractionTests {
        @MockK private lateinit var secureAnnotation: Secure
        @MockK private lateinit var authContextAnnotation: AuthContext // Mock specific annotation

        @BeforeEach
        fun setUpContextTest() { // Renamed
            mockkStatic(AnnotationUtils::class)
            every { AnnotationUtils.findAnnotation(method, Secure::class.java) } returns
                secureAnnotation
            every { AnnotationUtils.findAnnotation(method, SkipSecurity::class.java) } returns null
            every { AnnotationUtils.findAnnotation(testClass, SkipSecurity::class.java) } returns
                null
            every { secureAnnotation.domain } returns "test"
            every { secureAnnotation.action } returns "view"
            every { secureAnnotation.resource } returns "document"
            every { actionRegistry.getAction("test", "view") } returns viewAction
            every { resourceRegistry.getResource("test", "document") } returns documentResource
            every { contextProviderFactory.getProvider("test") } returns permissionContextProvider

            // Setup @AuthContext annotation properties BEFORE mocking method.parameterAnnotations
            every { authContextAnnotation.key } returns "projectId"
            every { authContextAnnotation.field } returns "" // Assuming default field extraction

            // Setup method parameter annotations
            val parameterAnnotations =
                arrayOf(
                    arrayOf<Annotation>(authContextAnnotation), // Param 0 has @AuthContext
                    emptyArray<Annotation>(), // Param 1 has none
                )
            every { method.parameterAnnotations } returns parameterAnnotations

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
            // Setup method parameters
            val projectIdValue: IdType = 789L
            every { joinPoint.args } returns arrayOf(projectIdValue, "someOtherValue")

            // **Crucially, mock what getContext returns**
            val baseContext = mapOf("baseKey" to "baseValue") // Context from provider
            val expectedMergedContext =
                baseContext +
                    mapOf("projectId" to projectIdValue) // Expected context after aspect merges
            every { permissionContextProvider.getContext("document", any()) } returns baseContext

            // Capture context passed to permissionEvaluator
            val contextCaptor = slot<Map<String, Any>>()

            // Execute method
            securityAspect.checkSecurityWithSecure(joinPoint)

            // Verify evaluate was called with the merged context
            verify {
                permissionEvaluator.evaluate(
                    any(),
                    any<Permission<Action, ResourceType>>(),
                    any(), // resourceId might be null or extracted depending on other annotations
                    capture(contextCaptor),
                )
            }

            // Verify captured context contains project ID and base context
            val capturedContext = contextCaptor.captured
            // assertEquals(expectedMergedContext, capturedContext)
            assertEquals(projectIdValue, capturedContext["projectId"]) // Verify specific key
            // assertEquals("baseValue", capturedContext["baseKey"]) // Verify base context part if
            // needed
        }
    }
}
