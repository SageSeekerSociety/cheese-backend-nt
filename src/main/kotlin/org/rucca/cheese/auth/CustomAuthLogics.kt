package org.rucca.cheese.auth

import org.rucca.cheese.common.persistent.IdGetter
import org.rucca.cheese.common.persistent.IdType

typealias CustomAuthLogicHandler =
        (
                userId: IdType,
                action: AuthorizedAction,
                resourceType: String,
                resourceId: IdType?,
                authInfo: Map<String, Any>,
                resourceOwnerIdGetter: IdGetter?,
                customLogicData: Any?,
        ) -> Boolean

class CustomAuthLogics {
    private val logics: MutableMap<String, CustomAuthLogicHandler> = mutableMapOf()

    fun register(
            name: String,
            handler: CustomAuthLogicHandler,
    ) {
        if (logics.containsKey(name)) throw RuntimeException("Custom auth logic '$name' already exists.")
        logics[name] = handler
    }

    fun invoke(
            name: String,
            userId: IdType,
            action: AuthorizedAction,
            resourceType: String,
            resourceId: IdType?,
            authInfo: Map<String, Any>,
            resourceOwnerIdGetter: IdGetter?,
            customLogicData: Any?,
    ): Boolean {
        val handler = logics[name] ?: throw RuntimeException("Custom auth logic '$name' not found.")
        return handler(userId, action, resourceType, resourceId, authInfo, resourceOwnerIdGetter, customLogicData)
    }
}
