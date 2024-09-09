package org.rucca.cheese.auth

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.annotation.NoAuth
import org.rucca.cheese.auth.annotation.ResourceId
import org.rucca.cheese.auth.exception.DuplicatedResourceIdAnnotationException
import org.rucca.cheese.auth.exception.NoGuardOrNoAuthAnnotationException
import org.rucca.cheese.auth.exception.ResourceIdTypeMismatchException
import org.rucca.cheese.common.persistent.IdType
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class AuthorizationAspect(
        private val authorizationService: AuthorizationService,
) {
    @Before(
            "@within(org.springframework.web.bind.annotation.RestController) || " +
                    "@within(org.springframework.stereotype.Controller)")
    fun checkAuthorization(joinPoint: JoinPoint) {
        val method = (joinPoint.signature as MethodSignature).method
        val guardAnnotation: Guard? = method.getAnnotation<Guard>(Guard::class.java)
        val noAuthAnnotation: NoAuth? = method.getAnnotation<NoAuth>(NoAuth::class.java)
        if (noAuthAnnotation == null) {
            if (guardAnnotation == null)
                    throw NoGuardOrNoAuthAnnotationException(
                            joinPoint.target.javaClass.name,
                            joinPoint.signature.name,
                    )

            var resourceId: IdType? = null
            val parameterAnnotations = method.parameterAnnotations
            for (i in parameterAnnotations.indices) {
                for (annotation in parameterAnnotations[i]) {
                    if (annotation is ResourceId) {
                        if (resourceId != null) {
                            throw DuplicatedResourceIdAnnotationException(
                                    joinPoint.target.javaClass.name,
                                    joinPoint.signature.name,
                            )
                        }
                        val arg = joinPoint.args[i]
                        if (arg !is IdType) {
                            throw ResourceIdTypeMismatchException(
                                    joinPoint.target.javaClass.name,
                                    joinPoint.signature.name,
                            )
                        }
                        resourceId = arg
                    }
                }
            }

            val token: String? =
                    (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes)
                            .request
                            .getHeader("Authorization")

            authorizationService.audit(
                    token,
                    guardAnnotation.action,
                    guardAnnotation.resourceType,
                    resourceId,
            )
        }
    }
}