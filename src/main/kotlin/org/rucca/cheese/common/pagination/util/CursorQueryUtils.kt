package org.rucca.cheese.common.pagination.util

import jakarta.persistence.Tuple
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Selection

/** Helper to apply tuple projections while keeping generics happy. */
object CursorQueryUtils {
    @Suppress("UNCHECKED_CAST")
    fun <T> applyProjection(
        query: CriteriaQuery<Tuple>,
        root: Root<T>,
        extras: List<Selection<*>>,
    ) {
        require(extras.isNotEmpty()) { "extras must not be empty" }

        val selections = java.util.ArrayList<Selection<*>>(1 + extras.size)
        selections.add(root as Selection<*>)
        selections.addAll(extras)
        query.multiselect(selections)
    }
}
