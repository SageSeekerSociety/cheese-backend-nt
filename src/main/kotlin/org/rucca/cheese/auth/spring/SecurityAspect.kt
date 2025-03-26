package org.rucca.cheese.auth.spring

import java.util.*
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.rucca.cheese.auth.context.PermissionContextProviderFactory
import org.rucca.cheese.auth.core.Permission
import org.rucca.cheese.auth.core.PermissionEvaluator
import org.rucca.cheese.auth.core.Role
import org.rucca.cheese.auth.model.AuthUserInfo
import org.rucca.cheese.auth.registry.ActionRegistry
import org.rucca.cheese.auth.registry.ResourceRegistry
import org.rucca.cheese.common.persistent.IdType
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * Aspect that intercepts method calls and performs permission checks. Works with @Secure and @Auth
 * annotations.
 */
@Aspect
@Component
class SecurityAspect(
    private val permissionEvaluator: PermissionEvaluator,
    private val actionRegistry: ActionRegistry,
    private val resourceRegistry: ResourceRegistry,
    private val contextProviderFactory: PermissionContextProviderFactory,
) {
    private val logger = LoggerFactory.getLogger(SecurityAspect::class.java)

    /** Intercepts methods annotated with @Secure and performs permission checks. */
    @Around("@annotation(org.rucca.cheese.auth.spring.Secure)")
    fun checkSecurityWithSecure(joinPoint: ProceedingJoinPoint): Any? {
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method

        // Skip check if SkipSecurity is present
        if (hasSkipSecurityAnnotation(method)) {
            return joinPoint.proceed()
        }

        // Get the Secure annotation
        val secureAnnotation =
            AnnotationUtils.findAnnotation(method, Secure::class.java)
                ?: throw IllegalStateException("Secure annotation not found")

        return checkPermission(
            joinPoint,
            secureAnnotation.domain,
            secureAnnotation.action,
            secureAnnotation.resource,
        )
    }

    /** Intercepts methods annotated with @Auth and performs permission checks. */
    @Around("@annotation(org.rucca.cheese.auth.spring.Auth)")
    fun checkSecurityWithAuth(joinPoint: ProceedingJoinPoint): Any? {
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method

        // Skip check if SkipSecurity is present
        if (hasSkipSecurityAnnotation(method)) {
            return joinPoint.proceed()
        }

        // Get the Auth annotation
        val authAnnotation =
            AnnotationUtils.findAnnotation(method, Auth::class.java)
                ?: throw IllegalStateException("Auth annotation not found")

        // Parse the permission string
        val parts = authAnnotation.value.split(":")
        if (parts.size != 3) {
            throw IllegalArgumentException(
                "Invalid Auth value format: ${authAnnotation.value}. " +
                    "Expected format: 'domain:action:resource'"
            )
        }

        val (domain, action, resource) = parts

        return checkPermission(joinPoint, domain, action, resource)
    }

    /** Checks if a method or its declaring class has the SkipSecurity annotation. */
    private fun hasSkipSecurityAnnotation(method: java.lang.reflect.Method): Boolean {
        return AnnotationUtils.findAnnotation(method, SkipSecurity::class.java) != null ||
            AnnotationUtils.findAnnotation(method.declaringClass, SkipSecurity::class.java) != null
    }

    /** Performs the actual permission check. */
    private fun checkPermission(
        joinPoint: ProceedingJoinPoint,
        domainName: String,
        actionName: String,
        resourceName: String,
    ): Any? {
        logger.debug("Checking permission: $domainName:$actionName:$resourceName")

        // Get action and resource from registries
        val action = actionRegistry.getAction(domainName, actionName)
        val resourceType = resourceRegistry.getResource(domainName, resourceName)

        // Create permission
        val permission = Permission(action, resourceType)

        // Extract resource ID from method parameters
        val resourceId = extractResourceId(joinPoint)

        // Get base context from method parameters
        val paramContext = extractParamContext(joinPoint)

        // Enhance context with domain-specific provider
        val contextProvider = contextProviderFactory.getProvider(domainName)
        val domainContext = contextProvider?.getContext(resourceName, resourceId) ?: emptyMap()

        val context = paramContext + domainContext

        // Get current user info
        val userInfo = getCurrentUserInfo()

        // Evaluate permission
        if (!permissionEvaluator.evaluate(userInfo, permission, resourceId, context)) {
            logger.warn(
                "Permission denied: $domainName:$actionName:$resourceName for user ${userInfo.userId}"
            )
            if (resourceId == null) {
                throw AccessDeniedException(
                    "Access denied for user ${userInfo.userId} to perform $actionName on $resourceName"
                )
            } else {
                throw AccessDeniedException(
                    "Access denied for user ${userInfo.userId} to perform $actionName on $resourceName:$resourceId"
                )
            }
        }

        // Permission granted, proceed with method
        return joinPoint.proceed()
    }

    /** Extracts the resource ID from method parameters annotated with @ResourceId. */
    private fun extractResourceId(joinPoint: ProceedingJoinPoint): IdType? {
        val methodSignature = joinPoint.signature as MethodSignature
        val parameterAnnotations = methodSignature.method.parameterAnnotations

        for (i in parameterAnnotations.indices) {
            if (parameterAnnotations[i].any { it is ResourceId }) {
                return joinPoint.args[i]?.toString()?.toLongOrNull()
            }
        }

        return null
    }

    /** Extracts context information from method parameters annotated with @AuthContext. */
    private fun extractParamContext(joinPoint: ProceedingJoinPoint): Map<String, Any> {
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        val parameterAnnotations = method.parameterAnnotations
        val parameterValues = joinPoint.args

        val context = mutableMapOf<String, Any>()

        for (i in parameterAnnotations.indices) {
            val authContextAnnotations =
                parameterAnnotations[i].filterIsInstance<AuthContext>() +
                    parameterAnnotations[i].filterIsInstance<AuthContexts>().flatMap {
                        it.value.toList()
                    }

            if (authContextAnnotations.isEmpty()) continue
            val paramValue = parameterValues[i] ?: continue

            for (authContextAnnotation in authContextAnnotations) {
                val key = authContextAnnotation.key
                val fieldPath = authContextAnnotation.field

                if (fieldPath.isBlank()) {
                    context[key] = paramValue
                } else {
                    val fieldValue = extractFieldValue(paramValue, fieldPath)
                    if (fieldValue != null) {
                        context[key] = fieldValue
                    }
                    logger.debug("Extracted context {} in field {}: {}", key, fieldPath, fieldValue)
                }
            }
        }

        return context
    }

    /**
     * Extracts a field value from an object using reflection. Supports nested fields with dot
     * notation (e.g., "user.address.city").
     */
    private fun extractFieldValue(obj: Any, fieldPath: String): Any? {
        val fields = fieldPath.split(".")
        var current: Any? = obj

        for (field in fields) {
            if (current == null) return null

            current =
                try {
                    val declaredField = current.javaClass.getDeclaredField(field)
                    declaredField.isAccessible = true
                    declaredField.get(current)
                } catch (e: Exception) {
                    try {
                        val getterName =
                            "get${field.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
                        val getter = current.javaClass.getDeclaredMethod(getterName)
                        getter.invoke(current)
                    } catch (e2: Exception) {
                        logger.warn("Failed to extract field '$field' from object", e2)
                        return null
                    }
                }
        }

        return current
    }

    /** Gets the current user ID from the security context. */
    @Suppress("UNCHECKED_CAST")
    private fun getCurrentUserInfo(): AuthUserInfo {
        try {
            val request =
                (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
            val userId =
                request.getAttribute("userId") as? IdType
                    ?: throw IllegalStateException("User ID not found in request")
            val userRole = request.getAttribute("userRole") as? Set<Role> ?: emptySet()
            return AuthUserInfo(userId, userRole)
        } catch (e: Exception) {
            logger.error("Error getting user auth info from request", e)
            throw IllegalStateException("Failed to get user auth info from request", e)
        }
    }
}
