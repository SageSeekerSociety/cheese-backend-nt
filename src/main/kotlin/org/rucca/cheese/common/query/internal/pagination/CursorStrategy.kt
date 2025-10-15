package org.rucca.cheese.common.query.internal.pagination

import kotlin.reflect.KProperty1
import org.rucca.cheese.common.query.model.SortDescriptor
import org.springframework.data.jpa.domain.Specification

/**
 * Internal abstraction responsible for translating high-level sort descriptors into concrete cursor
 * specifications. Individual implementations encapsulate the nuances between property-based keyset
 * pagination and relevance-based (Parade) cursors.
 */
interface CursorStrategy<T : Any, C : Cursor<T>> {
    fun build(
        entityClass: Class<T>,
        idProperty: KProperty1<T, Comparable<*>?>,
        sorts: List<SortDescriptor<T>>,
        baseSpecification: Specification<T>,
    ): CursorSpecification<T, C>
}
