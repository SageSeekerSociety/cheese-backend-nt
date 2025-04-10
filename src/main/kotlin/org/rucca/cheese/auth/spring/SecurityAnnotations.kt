package org.rucca.cheese.auth.spring

import java.lang.annotation.Inherited

/**
 * Annotation to mark a method as requiring security check. Specifies the domain, action, and
 * resource that the method requires permission for.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
annotation class Secure(
    /** The domain name. */
    val domain: String,

    /** The action name. */
    val action: String,

    /** The resource name. */
    val resource: String,
)

/**
 * Simplified version of @Secure annotation using a single string with format
 * "domain:action:resource".
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
annotation class Auth(
    /** Permission string in format "domain:action:resource". Example: "order:view:order" */
    val value: String = ""
)

/**
 * Annotation to mark a method as not requiring security check. Can be used on individual methods or
 * at the class level.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
annotation class SkipSecurity

/**
 * Annotation to mark a method parameter as containing the resource ID. This ID will be used for
 * permission checks that depend on the specific resource.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ResourceId

/**
 * Annotation to mark a parameter as contributing to the authentication context. The annotated
 * parameter will be added to the context with the specified key.
 *
 * @param key The key to use in the context map
 */
@JvmRepeatable(AuthContexts::class)
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AuthContext(val key: String, val field: String = "")

/** Container annotation for multiple @AuthContext annotations. */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AuthContexts(vararg val value: AuthContext)

/**
 * Annotation to inject the current authenticated user into controller methods. Can be used on
 * method parameters to receive user information from JWT.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AuthUser

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class UseOldAuth
