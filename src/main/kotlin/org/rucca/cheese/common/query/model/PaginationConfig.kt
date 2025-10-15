package org.rucca.cheese.common.query.model

/**
 * Declarative pagination preferences accompanying a query blueprint. Defaults mirror current
 * repository behavior.
 */
data class PaginationConfig(
    val pageSize: Int = 20,
    val maxPageSize: Int = 100,
    val includeTotalCount: Boolean = false,
    val cursorMode: CursorMode = CursorMode.AUTO,
)

enum class CursorMode {
    AUTO,
    ID_SEEK,
}
