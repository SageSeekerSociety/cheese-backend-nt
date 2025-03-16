package org.rucca.cheese.auth.spring

/** Exception thrown when a user does not have permission for an action. */
class AccessDeniedException(message: String) : RuntimeException(message)
