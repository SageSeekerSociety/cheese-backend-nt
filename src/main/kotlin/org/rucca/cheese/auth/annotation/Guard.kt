package org.rucca.cheese.auth.annotation

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class Guard(val action: String, val resourceType: String)
