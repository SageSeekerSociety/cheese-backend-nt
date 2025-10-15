package org.rucca.cheese.common.query.internal.pagination

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Root
import kotlin.reflect.KProperty1

/**
 * Marker for cursor specifications that paginate by Parade relevance score, exposing the metadata
 * required to perform the legacy double-query tie-breaker.
 */
interface RelevanceCursorSupport<T : Any> {
    /** Primary key property used as deterministic tie-breaker (must implement Comparable). */
    val idProperty: KProperty1<T, out Comparable<*>?>

    /** Alias assigned to the computed Parade score in the tuple projection. */
    val scoreAlias: String

    /** Criteria expression that yields the Parade relevance score for the given root. */
    fun scoreExpression(root: Root<T>, criteriaBuilder: CriteriaBuilder): Expression<Double>
}
