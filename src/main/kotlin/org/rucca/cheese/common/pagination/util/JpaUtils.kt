package org.rucca.cheese.common.pagination.util

import jakarta.persistence.criteria.Path

/** Utility functions for JPA Criteria API operations. */
object JpaUtils {

    /**
     * Safely resolves a potentially nested property path string (e.g., "user.address.city") into a
     * JPA Criteria API Path object starting from the given root.
     *
     * @param root The starting root object.
     * @param pathString The property path string.
     * @return The resolved Path object.
     * @throws IllegalArgumentException if the path is invalid or cannot be resolved.
     */
    @Suppress("UNCHECKED_CAST")
    fun <Y> getPath(root: Path<*>, pathString: String): Path<Y> {
        val parts = pathString.split('.')
        var currentPath: Path<*> = root
        try {
            for (part in parts) {
                currentPath = currentPath.get<Any>(part) // Iteratively get nested paths
            }
        } catch (e: Exception) {
            // Catch JPA exceptions (like IllegalArgumentException if path doesn't exist)
            // or potentially NullPointerException if intermediate path is null (though get should
            // handle this)
            throw IllegalArgumentException(
                "Failed to resolve path '$pathString' from root ${root.alias ?: root}",
                e,
            )
        }

        try {
            return currentPath as Path<Y>
        } catch (e: ClassCastException) {
            // This indicates the resolved path type doesn't match the expected type Y
            throw IllegalArgumentException(
                "Path '$pathString' resolved to type ${currentPath.javaType} which is not assignable to the expected type",
                e,
            )
        }
    }
}
