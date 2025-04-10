package org.rucca.cheese.notification.resolver

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NotificationEntityResolverFacade(private val resolvers: List<EntityInfoResolver>) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val resolverMap: Map<String, EntityInfoResolver> by lazy {
        resolvers
            .associateBy {
                log.info("Registering EntityInfoResolver for type: {}", it.supportedEntityType())
                it.supportedEntityType()
            }
            .also { map ->
                if (map.size != resolvers.size) {
                    val duplicates =
                        resolvers
                            .groupingBy { it.supportedEntityType() }
                            .eachCount()
                            .filter { it.value > 1 }
                            .keys
                    log.error("Duplicate EntityInfoResolver types detected: {}", duplicates)
                }
            }
    }

    /**
     * Resolves entities based on structured references like {"type": "user", "id": "123"} found
     * within notification metadata maps.
     *
     * @param metadataMaps Collection of parsed metadata maps from notifications.
     * @return Map where key is entity type ("user", "team") and value is another map of
     *   [entityId (String) -> ResolvedInfo (ResolvableEntityInfo?)].
     */
    suspend fun resolveEntitiesFromMetadata(
        metadataMaps: Collection<Map<String, Any>>
    ): Map<String, Map<String, ResolvableEntityInfo?>> {
        if (metadataMaps.isEmpty()) return emptyMap()

        // Step 1: Collect unique entity references (type -> Set<ID>) by parsing structured data
        val entitiesToResolve = mutableMapOf<String, MutableSet<String>>()

        metadataMaps.forEach { metadata ->
            metadata.values.forEach { value -> // Iterate through values in the metadata map
                if (value is Map<*, *>) { // Check if the value is a map itself
                    val entityType = value["type"] as? String
                    val entityId = value["id"] as? String // Assuming ID is stored as String now
                    if (
                        entityType != null &&
                            entityId != null &&
                            resolverMap.containsKey(entityType)
                    ) {
                        // If it looks like our entity reference structure and we have a resolver
                        // for it
                        entitiesToResolve
                            .computeIfAbsent(entityType) { mutableSetOf() }
                            .add(entityId)
                    }
                }
                // Could add checks for other potential structures if needed
            }
        }

        if (entitiesToResolve.isEmpty()) return emptyMap()

        // Step 2: Resolve entities in parallel using coroutines (same as before)
        val resolvedEntities = mutableMapOf<String, Map<String, ResolvableEntityInfo?>>()
        coroutineScope {
            val deferredResults =
                entitiesToResolve.mapNotNull { (entityType, entityIds) ->
                    resolverMap[entityType]?.let { resolver ->
                        async<Pair<String, Map<String, ResolvableEntityInfo?>>> {
                            log.debug("Resolving {} {} entities...", entityIds.size, entityType)
                            try {
                                entityType to
                                    resolver.resolve(
                                        entityIds
                                    ) // Resolver still expects Set<String>
                            } catch (e: Exception) {
                                log.error(
                                    "Resolver for type '{}' failed: {}",
                                    entityType,
                                    e.message,
                                    e,
                                )
                                entityType to entityIds.associateWith { null }
                            }
                        }
                    }
                        ?: run {
                            log.warn("No resolver found for entity type: {}", entityType)
                            null
                        }
                }
            deferredResults.awaitAll().forEach { (entityType, resultMap) ->
                resolvedEntities[entityType] = resultMap
            }
        }

        return resolvedEntities
    }
}
