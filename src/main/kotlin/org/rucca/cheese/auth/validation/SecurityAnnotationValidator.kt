package org.rucca.cheese.auth.validation

import java.lang.reflect.Method
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.javaMethod
import org.rucca.cheese.auth.spring.Auth
import org.rucca.cheese.auth.spring.Secure
import org.rucca.cheese.auth.spring.SkipSecurity
import org.rucca.cheese.auth.spring.UseOldAuth
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

/**
 * Validator that ensures all request handler methods in controllers have security annotations.
 * Validates at application startup and fails-fast if any violations are found.
 */
@Component
class SecurityAnnotationValidator(private val applicationContext: ApplicationContext) :
    InitializingBean {

    private val logger = LoggerFactory.getLogger(SecurityAnnotationValidator::class.java)

    /** Annotation types that identify request handler methods. */
    private val requestMappingAnnotations =
        setOf(
            RequestMapping::class.java,
            GetMapping::class.java,
            PostMapping::class.java,
            PutMapping::class.java,
            DeleteMapping::class.java,
            PatchMapping::class.java,
        )

    /** Security annotation types that are required. */
    private val securityAnnotations =
        setOf(Auth::class.java, Secure::class.java, SkipSecurity::class.java)

    /** Validate all controllers when the application starts. */
    override fun afterPropertiesSet() {
        logger.info("Validating security annotations on controllers...")

        val violations = mutableListOf<String>()

        // Find all controllers in the target package
        val controllers = findControllersInPackage("org.rucca.cheese")

        // Check each controller
        for (controller in controllers) {

            val invalidMethods = findMethodsWithoutSecurityAnnotations(controller)

            if (invalidMethods.isNotEmpty()) {
                val violationMsg = buildViolationMessage(controller, invalidMethods)
                violations.add(violationMsg)
            }
        }

        // If any violations found, log them and throw exception
        if (violations.isNotEmpty()) {
            val errorMessage = buildErrorMessage(violations)
            logger.error(errorMessage)
            throw IllegalStateException(errorMessage)
        }

        logger.info("Security annotation validation successful.")
    }

    /** Find all controllers in a specific package and its sub-packages. */
    private fun findControllersInPackage(packageName: String): List<Class<*>> {
        // Get all beans that are controllers
        val controllerBeans =
            applicationContext.getBeansWithAnnotation(Controller::class.java) +
                applicationContext.getBeansWithAnnotation(RestController::class.java)

        // Filter to only those in the target package
        return controllerBeans.values
            .map { AopUtils.getTargetClass(it) }
            .filter {
                it.name.startsWith(packageName) && !it.isAnnotationPresent(UseOldAuth::class.java)
            }
    }

    private fun isMethodOverridden(targetClass: Class<*>, methodName: String): Boolean {
        val kClass = targetClass.kotlin
        val kFunction = kClass.declaredFunctions.find { it.name == methodName }
        return kFunction?.javaMethod?.declaringClass == targetClass
    }

    /** Find methods that handle HTTP requests but don't have security annotations. */
    private fun findMethodsWithoutSecurityAnnotations(controllerClass: Class<*>): List<Method> {
        // Skip if class has SkipSecurity annotation
        if (AnnotationUtils.findAnnotation(controllerClass, SkipSecurity::class.java) != null) {
            return emptyList()
        }

        return controllerClass.methods.filter { method ->
            val isImplemented = isMethodOverridden(controllerClass, method.name)

            // Skip if method is not implemented in this class
            if (!isImplemented) return@filter false

            // Check if this is a request handler method
            val isRequestHandler =
                requestMappingAnnotations.any { annotationClass ->
                    AnnotationUtils.findAnnotation(method, annotationClass) != null
                }

            // If it's a request handler, it needs a security annotation
            if (isRequestHandler) {
                val hasSecurityAnnotation =
                    securityAnnotations.any { annotationClass ->
                        AnnotationUtils.findAnnotation(method, annotationClass) != null
                    }

                !hasSecurityAnnotation
            } else {
                false
            }
        }
    }

    /** Build error message for a single controller. */
    private fun buildViolationMessage(controller: Class<*>, methods: List<Method>): String {
        val methodNames = methods.joinToString(", ") { it.name }
        return "Controller [${controller.name}] has methods without security annotations: $methodNames"
    }

    /** Build the final error message from all violations. */
    private fun buildErrorMessage(violations: List<String>): String {
        return "Security annotation validation failed with ${violations.size} violations:\n" +
            violations.joinToString("\n") +
            "\n\n" +
            "All controller methods must have either @Auth, @Secure, or @SkipSecurity annotations."
    }
}
