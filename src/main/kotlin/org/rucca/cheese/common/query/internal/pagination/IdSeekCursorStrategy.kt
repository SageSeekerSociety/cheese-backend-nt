package org.rucca.cheese.common.query.internal.pagination

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.io.Serializable
import kotlin.reflect.KProperty1
import org.rucca.cheese.common.query.model.PropertySort
import org.rucca.cheese.common.query.model.SortDescriptor
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification

internal class IdSeekCursorStrategy<T : Any, ID>(private val entityResolver: (ID) -> T?) :
    CursorStrategy<T, SimpleCursor<T, ID>> where ID : Serializable, ID : Comparable<ID> {

    override fun build(
        entityClass: Class<T>,
        idProperty: KProperty1<T, Comparable<*>?>,
        sorts: List<SortDescriptor<T>>,
        baseSpecification: Specification<T>,
    ): CursorSpecification<T, SimpleCursor<T, ID>> {
        require(sorts.size == 1) { "ID seek mode requires exactly one property sort descriptor" }
        val sortDescriptor = sorts.first()
        require(sortDescriptor is PropertySort<T>) {
            "ID seek mode requires property sort descriptor"
        }

        @Suppress("UNCHECKED_CAST") val typedIdProperty = idProperty as KProperty1<T, ID?>
        @Suppress("UNCHECKED_CAST")
        val typedSortProperty = sortDescriptor.property as KProperty1<T, Comparable<*>?>

        return IdSeekSpecification(
            idProperty = typedIdProperty,
            sortProperty = typedSortProperty,
            direction = sortDescriptor.direction,
            baseSpecification = baseSpecification,
            entityResolver = entityResolver,
        )
    }

    private class IdSeekSpecification<T : Any, ID : Comparable<ID>>(
        private val idProperty: KProperty1<T, ID?>,
        private val sortProperty: KProperty1<T, Comparable<*>?>,
        private val direction: Sort.Direction,
        private val baseSpecification: Specification<T>,
        private val entityResolver: (ID) -> T?,
    ) : CursorSpecification<T, SimpleCursor<T, ID>> {

        override fun toPredicate(
            root: Root<T>,
            query: CriteriaQuery<*>,
            criteriaBuilder: CriteriaBuilder,
        ): Predicate {
            return baseSpecification.toPredicate(root, query, criteriaBuilder)
                ?: criteriaBuilder.conjunction()
        }

        override fun toCursorPredicate(
            root: Root<T>,
            query: CriteriaQuery<*>,
            criteriaBuilder: CriteriaBuilder,
            cursor: SimpleCursor<T, ID>?,
        ): Predicate? {
            val cursorId = cursor?.value ?: return null
            val reference = entityResolver(cursorId) ?: return null
            val sortValue = sortProperty.get(reference)
            val sortPath = root.get<Comparable<Any?>>(sortProperty.name) as Path<Comparable<Any?>>
            val idPath = root.get<ID>(idProperty.name)

            val tieBreaker =
                when (direction) {
                    Sort.Direction.ASC -> criteriaBuilder.greaterThan(idPath, cursorId)
                    Sort.Direction.DESC -> criteriaBuilder.lessThan(idPath, cursorId)
                }

            return when (direction) {
                // ASC (PostgreSQL default: NULLS LAST) — after a NULL-valued row, stay within NULL
                // group; there is nothing "beyond" NULLs.
                Sort.Direction.ASC -> {
                    if (sortValue != null) {
                        @Suppress("UNCHECKED_CAST")
                        val comparable = sortValue as? Comparable<Any?> ?: return null
                        criteriaBuilder.or(
                            criteriaBuilder.greaterThan(sortPath, comparable),
                            criteriaBuilder.and(
                                criteriaBuilder.equal(sortPath, sortValue),
                                tieBreaker,
                            ),
                        )
                    } else {
                        criteriaBuilder.and(criteriaBuilder.isNull(sortPath), tieBreaker)
                    }
                }
                // DESC (PostgreSQL default: NULLS FIRST) — after a NULL-valued row, include
                // remaining NULLs by tie-breaker and then all non-NULLs.
                Sort.Direction.DESC -> {
                    if (sortValue != null) {
                        @Suppress("UNCHECKED_CAST")
                        val comparable = sortValue as? Comparable<Any?> ?: return null
                        criteriaBuilder.or(
                            criteriaBuilder.lessThan(sortPath, comparable),
                            criteriaBuilder.and(
                                criteriaBuilder.equal(sortPath, sortValue),
                                tieBreaker,
                            ),
                        )
                    } else {
                        criteriaBuilder.or(
                            criteriaBuilder.and(criteriaBuilder.isNull(sortPath), tieBreaker),
                            criteriaBuilder.isNotNull(sortPath),
                        )
                    }
                }
            }
        }

        override fun getSort(): Sort {
            val primary =
                Sort.Order(direction, sortProperty.name).let {
                    if (direction == Sort.Direction.ASC) it.nullsLast() else it.nullsFirst()
                }
            val tieBreak = Sort.Order(direction, idProperty.name)
            return Sort.by(primary, tieBreak)
        }

        override fun extractCursor(entity: T): SimpleCursor<T, ID>? {
            val id = idProperty.get(entity) ?: return null
            return SimpleCursor.of(id)
        }
    }
}
