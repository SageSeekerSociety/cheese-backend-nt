package org.rucca.cheese.common.query.model

import kotlin.reflect.KProperty1
import org.springframework.data.domain.Sort

sealed class SortDescriptor<T : Any>

data class PropertySort<T : Any>(
    val property: KProperty1<T, *>,
    val direction: Sort.Direction,
    val nullHandling: Sort.NullHandling? = null,
) : SortDescriptor<T>()

data class RelevanceSort<T : Any>(val direction: Sort.Direction, val scoreAlias: String? = null) :
    SortDescriptor<T>()

data class ExpressionSort<T : Any>(
    val descriptor: ExpressionDescriptor<T>,
    val direction: Sort.Direction,
) : SortDescriptor<T>()

data class ExpressionDescriptor<T : Any>(
    val function: String,
    val arguments: List<ExpressionArgument<T>> = emptyList(),
)

sealed interface ExpressionArgument<T : Any>

data class PropertyExpressionArgument<T : Any>(val property: KProperty1<T, *>) :
    ExpressionArgument<T>

data class LiteralExpressionArgument<T : Any>(val value: Any?) : ExpressionArgument<T>

data class NestedExpressionArgument<T : Any>(val descriptor: ExpressionDescriptor<T>) :
    ExpressionArgument<T>
