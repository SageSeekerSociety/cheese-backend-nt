package org.rucca.cheese.notification.resolver

import org.rucca.cheese.model.ResolvedEntityInfoDTO

/**
 * Represents the basic information needed to display an entity in a notification. Different entity
 * types can implement this or be mapped to it.
 */
interface ResolvableEntityInfo {
    val id: String // Use String ID for generality, or keep Long if all your IDs are Long
    val name: String // The display name/nickname/title
    val type: String // A unique identifier for the entity type (e.g., "user", "team", "project")
    val url: String? // Optional URL to link to the entity
    val avatarUrl: String? // Optional avatar/icon URL
    val status: String? // Optional status (e.g., "PENDING", "ACCEPTED") for specific entity types
        get() = null // Default implementation returns null
}

fun ResolvableEntityInfo.toDTO(): ResolvedEntityInfoDTO {
    return ResolvedEntityInfoDTO(
        id = id,
        name = name,
        type = type,
        url = url,
        avatarUrl = avatarUrl,
        status = status,
    )
}
