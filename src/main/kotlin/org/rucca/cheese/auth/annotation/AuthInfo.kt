package org.rucca.cheese.auth.annotation

@Target(AnnotationTarget.VALUE_PARAMETER) @MustBeDocumented annotation class AuthInfo(val key: String)
