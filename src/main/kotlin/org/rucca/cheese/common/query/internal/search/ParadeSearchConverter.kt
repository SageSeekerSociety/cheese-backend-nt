package org.rucca.cheese.common.query.internal.search

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.From
import jakarta.persistence.criteria.Predicate
import kotlin.reflect.KProperty1
import org.springframework.data.jpa.domain.Specification

object ParadeSearchConverter {
    fun <T> predicate(
        cb: CriteriaBuilder,
        from: From<*, T>,
        idProperty: KProperty1<T, *>,
        searchQuery: SearchQuery,
    ): Predicate {
        val jsonbExpression =
            cb.function("to_jsonb", Any::class.java, cb.literal(searchQuery.toJsonString()))
        return cb.isTrue(
            cb.function(
                "pg_search",
                Boolean::class.javaObjectType,
                from.get<Any>(idProperty.name),
                jsonbExpression,
            )
        )
    }

    fun <T> specification(
        searchQuery: SearchQuery,
        idProperty: KProperty1<T, *>,
    ): Specification<T> = Specification { root, _, criteriaBuilder ->
        predicate(criteriaBuilder, root, idProperty, searchQuery)
    }
}
