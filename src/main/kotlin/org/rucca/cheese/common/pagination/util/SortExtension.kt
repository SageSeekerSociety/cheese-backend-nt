package org.rucca.cheese.common.pagination.util

import org.hibernate.query.SortDirection
import org.springframework.data.domain.Sort
import kotlin.reflect.KProperty1

/**
 * Create an ascending sort property pair.
 *
 * @return Pair of property and ASC direction
 */
fun <T> KProperty1<T, *>.asc(): Pair<KProperty1<T, *>, Sort.Direction> {
    return this to Sort.Direction.ASC
}

/**
 * Create a descending sort property pair.
 *
 * @return Pair of property and DESC direction
 */
fun <T> KProperty1<T, *>.desc(): Pair<KProperty1<T, *>, Sort.Direction> {
    return this to Sort.Direction.DESC
}

// 带参数的中缀函数
infix fun <T> KProperty1<T, *>.sortedBy(
    direction: Sort.Direction
): Pair<KProperty1<T, *>, Sort.Direction> {
    return this to direction
}

/**
 * Convert Hibernate SortDirection to Spring Sort.Direction.
 *
 * @return Equivalent Spring Sort.Direction
 */
fun SortDirection.toJpaDirection(): Sort.Direction {
    return when (this) {
        SortDirection.ASCENDING -> Sort.Direction.ASC
        SortDirection.DESCENDING -> Sort.Direction.DESC
    }
}
