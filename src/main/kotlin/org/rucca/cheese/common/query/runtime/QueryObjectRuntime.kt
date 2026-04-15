package org.rucca.cheese.common.query.runtime

import java.io.Serializable
import kotlin.reflect.KProperty1
import org.rucca.cheese.common.query.internal.pagination.CursorStrategy
import org.rucca.cheese.common.query.internal.pagination.IdSeekCursorStrategy
import org.rucca.cheese.common.query.internal.pagination.PropertyBasedCursorStrategy
import org.rucca.cheese.common.query.internal.pagination.RelevanceCursorStrategy
import org.rucca.cheese.common.query.internal.search.ParadeSearchConverter
import org.rucca.cheese.common.query.model.CursorMode
import org.rucca.cheese.common.query.model.PropertySort
import org.rucca.cheese.common.query.model.QueryObject
import org.rucca.cheese.common.query.model.RelevanceSort
import org.rucca.cheese.common.query.model.SortDescriptor
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification

object QueryObjectRuntime {
    fun <T : Any, ID> toCursorSpecification(
        query: QueryObject<T>,
        entityResolver: ((ID) -> T?)? = null,
    ): CursorSpecification<T, *> where ID : Serializable, ID : Comparable<ID> {
        val idProperty = query.idProperty
        val baseSpec = combineSpecifications(query, idProperty)
        val descriptors = effectiveSorts(query, idProperty)
        val strategy = selectStrategy(query.pagination.cursorMode, descriptors, entityResolver)
        return strategy.build(query.entityClass.java, idProperty, descriptors, baseSpec)
    }

    private fun <T : Any> combineSpecifications(
        query: QueryObject<T>,
        idProperty: KProperty1<T, Comparable<*>?>,
    ): Specification<T> {
        var combined: Specification<T> = query.filter
        query.search?.let { clause ->
            val searchSpec = ParadeSearchConverter.specification(clause.searchQuery, idProperty)
            combined = combined.and(searchSpec)
        }
        query.having?.let { havingSpec -> combined = combined.and(havingSpec) }
        return combined
    }

    private fun <T : Any> effectiveSorts(
        query: QueryObject<T>,
        idProperty: KProperty1<T, Comparable<*>?>,
    ): List<SortDescriptor<T>> {
        if (query.sorts.isNotEmpty()) return query.sorts
        return listOf(PropertySort(property = idProperty, direction = Sort.Direction.ASC))
    }

    private fun <T : Any, ID> selectStrategy(
        cursorMode: CursorMode,
        descriptors: List<SortDescriptor<T>>,
        entityResolver: ((ID) -> T?)?,
    ): CursorStrategy<T, *> where ID : Serializable, ID : Comparable<ID> {
        return when {
            cursorMode == CursorMode.ID_SEEK -> {
                requireNotNull(entityResolver) { "ID seek mode requires an entity resolver" }
                IdSeekCursorStrategy<T, ID>(entityResolver)
            }
            descriptors.any { it is RelevanceSort<*> } -> RelevanceCursorStrategy<T>()
            else -> PropertyBasedCursorStrategy<T>()
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : Any, ID, C : Cursor<T>> CursorPagingRepository<T, ID>.findWithQueryObject(
    queryObject: QueryObject<T>,
    cursor: C? = null,
    pageSize: Int,
): CursorPage<T, C> where ID : Serializable, ID : Comparable<ID> {
    val resolver: (ID) -> T? = { id -> this.findById(id).orElse(null) }
    val cursorSpec = QueryObjectRuntime.toCursorSpecification<T, ID>(queryObject, resolver)
    return findAllWithCursor(cursorSpec as CursorSpecification<T, C>, cursor, pageSize)
}
