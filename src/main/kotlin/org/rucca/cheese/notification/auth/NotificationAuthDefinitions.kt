package org.rucca.cheese.notification.auth

import org.rucca.cheese.auth.core.*
import org.springframework.stereotype.Component

@Component // Make it a Spring bean
object NotificationDomain : Domain {
    override val name: String = "notification"
}

enum class NotificationAction(override val actionId: String) : Action {
    LIST("list"),
    VIEW("view"),
    UPDATE("update"),
    DELETE("delete");

    override val domain: Domain = NotificationDomain
}

enum class NotificationResource(override val typeName: String) : ResourceType {
    NOTIFICATION("notification");

    override val domain: Domain = NotificationDomain

    companion object {
        fun of(typeName: String) =
            entries.firstOrNull { it.typeName == typeName }
                ?: throw IllegalArgumentException("Invalid notification resource type: $typeName")
    }
}

enum class NotificationRole(override val roleId: String) : Role {
    OWNER("owner"); // The user owns the notification(s) being accessed

    override val domain: Domain = NotificationDomain
}
