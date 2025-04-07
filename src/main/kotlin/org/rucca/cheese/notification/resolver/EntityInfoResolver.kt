package org.rucca.cheese.notification.resolver

/**
 * Interface for components capable of resolving a batch of entity IDs of a specific type into
 * displayable information.
 */
interface EntityInfoResolver {
    /**
     * The entity type this resolver handles (e.g., "user", "team"). Must be unique across all
     * resolvers.
     */
    fun supportedEntityType(): String

    /**
     * Resolves a set of entity IDs into a map of ID -> ResolvableEntityInfo. Implementations should
     * perform efficient batch lookups.
     *
     * @param entityIds Set of IDs (as Strings) to resolve for the supported type.
     * @return Map where key is entityId (String) and value is the resolved info (or null if not
     *   found/error).
     */
    suspend fun resolve(entityIds: Set<String>): Map<String, ResolvableEntityInfo?>
}
