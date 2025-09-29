package org.rucca.cheese.common.persistent.spec

import jakarta.persistence.criteria.From
import jakarta.persistence.criteria.Path
import kotlin.reflect.KProperty1
import org.rucca.cheese.common.query.internal.spec.Ancestor as InternalAncestor
import org.rucca.cheese.common.query.internal.spec.ConditionalClause as InternalConditionalClause
import org.rucca.cheese.common.query.internal.spec.ExpressionFilteringScope as InternalExpressionFilteringScope
import org.rucca.cheese.common.query.internal.spec.FullFilteringScope as InternalFullFilteringScope
import org.rucca.cheese.common.query.internal.spec.PathRef as InternalPathRef
import org.rucca.cheese.common.query.internal.spec.SpecContext as InternalSpecContext
import org.rucca.cheese.common.query.internal.spec.SpecDsl as InternalSpecDsl
import org.rucca.cheese.common.query.internal.spec.SubWhereContext as InternalSubWhereContext
import org.rucca.cheese.common.query.internal.spec.SubqueryContext as InternalSubqueryContext
import org.rucca.cheese.common.query.internal.spec.WhereContext as InternalWhereContext
import org.rucca.cheese.common.query.internal.spec.div as internalDiv
import org.rucca.cheese.common.query.internal.spec.dot as internalDot
import org.springframework.data.jpa.domain.Specification

typealias SpecDsl = InternalSpecDsl

typealias PathRef<E, R> = InternalPathRef<E, R>

typealias SpecContext<T> = InternalSpecContext<T>

typealias WhereContext<T> = InternalWhereContext<T>

typealias SubWhereContext<S> = InternalSubWhereContext<S>

typealias SubqueryContext<S, P, R> = InternalSubqueryContext<S, P, R>

typealias ConditionalClause<E> = InternalConditionalClause<E>

typealias Ancestor = InternalAncestor

typealias ExpressionFilteringScope<T> = InternalExpressionFilteringScope<T>

typealias FullFilteringScope<T> = InternalFullFilteringScope<T>

@Suppress("NOTHING_TO_INLINE")
inline fun <reified T : Any> spec(noinline block: SpecContext<T>.() -> Unit): Specification<T> =
    org.rucca.cheese.common.query.internal.spec.spec(block)

@Suppress("NOTHING_TO_INLINE")
inline fun <E, R> prop(p: KProperty1<E, R>): PathRef<E, R> =
    org.rucca.cheese.common.query.internal.spec.prop(p)

@Suppress("NOTHING_TO_INLINE")
inline infix fun <E, A : Any, R> KProperty1<E, A?>.dot(next: KProperty1<A, R>): PathRef<E, R> =
    this internalDot next

@Suppress("NOTHING_TO_INLINE")
inline infix fun <E, A : Any, B> PathRef<E, A?>.dot(next: KProperty1<A, B>): PathRef<E, B> =
    this internalDot next

@Suppress("NOTHING_TO_INLINE")
inline infix operator fun <E, A : Any, R> KProperty1<E, A?>.div(
    next: KProperty1<A, R>
): PathRef<E, R> = this internalDiv next

@Suppress("NOTHING_TO_INLINE")
inline infix operator fun <E, A : Any, B> PathRef<E, A?>.div(
    next: KProperty1<A, B>
): PathRef<E, B> = this internalDiv next

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
inline fun <E, R> PathRef<E, R>.toPath(from: From<*, E>): Path<R> =
    (this as org.rucca.cheese.common.query.internal.spec.PathRef<E, R>).toPath(from)
