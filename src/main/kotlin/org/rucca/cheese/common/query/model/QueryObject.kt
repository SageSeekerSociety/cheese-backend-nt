package org.rucca.cheese.common.query.model

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import org.springframework.data.jpa.domain.Specification

/**
 * Immutable blueprint produced by the QueryObject DSL. It captures declarative query intent that
 * later gets materialized by runtime strategies.
 */
data class QueryObject<T : Any>(
    val entityClass: KClass<T>,
    val idProperty: KProperty1<T, Comparable<*>?>,
    val filter: Specification<T>,
    val search: SearchClause<T>?,
    val groups: List<KProperty1<T, *>>,
    val having: Specification<T>?,
    val sorts: List<SortDescriptor<T>>,
    val pagination: PaginationConfig,
)
