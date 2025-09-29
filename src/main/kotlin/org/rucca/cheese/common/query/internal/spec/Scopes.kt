package org.rucca.cheese.common.query.internal.spec

import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Path
import kotlin.reflect.KProperty1
import org.rucca.cheese.common.query.internal.search.QueryBuilderScope
import org.rucca.cheese.common.query.internal.search.SearchQuery

interface ExpressionFilteringScope<T : Any> {
    val ctx: WhereContext<T>

    infix fun <R> Expression<R>.eq(value: R?) {
        ctx.run { this@eq eq value }
    }

    infix fun Expression<*>.eq(other: Expression<*>) {
        ctx.run { this@eq eq other }
    }

    infix fun <R> Expression<R>.notEq(value: R?) {
        ctx.run { this@notEq notEq value }
    }

    infix fun Expression<*>.notEq(other: Expression<*>) {
        ctx.run { this@notEq notEq other }
    }

    infix fun <R : Comparable<R>> Expression<R>.gt(value: R?) {
        ctx.run { this@gt gt value }
    }

    infix fun <R : Comparable<R>> Expression<R>.gte(value: R?) {
        ctx.run { this@gte gte value }
    }

    infix fun <R : Comparable<R>> Expression<R>.lt(value: R?) {
        ctx.run { this@lt lt value }
    }

    infix fun <R : Comparable<R>> Expression<R>.lte(value: R?) {
        ctx.run { this@lte lte value }
    }

    val Expression<*>.isNull: Unit
        get() {
            ctx.run { this@isNull.isNull }
        }

    val Expression<*>.isNotNull: Unit
        get() {
            ctx.run { this@isNotNull.isNotNull }
        }

    infix fun <R : Any> Expression<R?>.inList(values: Collection<R>?) {
        ctx.run { this@inList.inList(values) }
    }

    infix fun <R : Any> Expression<R?>.notInList(values: Collection<R>?) {
        ctx.run { this@notInList.notInList(values) }
    }

    infix fun Expression<String>.like(pattern: String?) {
        ctx.run { this@like like pattern }
    }

    infix fun Expression<String>.notLike(pattern: String?) {
        ctx.run { this@notLike.notLike(pattern) }
    }

    infix fun Expression<String>.ilike(pattern: String?) {
        ctx.run { this@ilike.ilike(pattern) }
    }

    fun Expression<String>.contains(text: String?, caseSensitive: Boolean = true) {
        ctx.run { this@contains.contains(text, caseSensitive) }
    }

    fun <X : Any> fn(name: String, type: Class<X>, vararg args: Expression<*>): Expression<X> =
        ctx.fn(name, type, *args)

    fun lower(expression: Expression<String>): Expression<String?> = ctx.lower(expression)

    fun upper(expression: Expression<String>): Expression<String?> = ctx.upper(expression)

    fun length(expression: Expression<String>): Expression<Int?> = ctx.length(expression)
}

interface PropertyFilteringScope<T : Any> : ExpressionFilteringScope<T> {
    infix fun <R : Any> KProperty1<T, R?>.eq(value: R?) {
        ctx.run { this@eq eq value }
    }

    infix fun <R : Any> PathRef<T, R?>.eq(value: R?) {
        ctx.run { this@eq eq value }
    }

    infix fun <R : Any> KProperty1<T, R?>.notEq(value: R?) {
        ctx.run { this@notEq notEq value }
    }

    infix fun <R : Any> PathRef<T, R?>.notEq(value: R?) {
        ctx.run { this@notEq.notEq(value) }
    }

    infix fun <R : Comparable<R>> KProperty1<T, R?>.gt(value: R?) {
        ctx.run { this@gt gt value }
    }

    infix fun <R : Comparable<R>> PathRef<T, R?>.gt(value: R?) {
        ctx.run { this@gt gt value }
    }

    infix fun <R : Comparable<R>> KProperty1<T, R?>.gte(value: R?) {
        ctx.run { this@gte gte value }
    }

    infix fun <R : Comparable<R>> PathRef<T, R?>.gte(value: R?) {
        ctx.run { this@gte gte value }
    }

    infix fun <R : Comparable<R>> KProperty1<T, R?>.lt(value: R?) {
        ctx.run { this@lt lt value }
    }

    infix fun <R : Comparable<R>> PathRef<T, R?>.lt(value: R?) {
        ctx.run { this@lt lt value }
    }

    infix fun <R : Comparable<R>> KProperty1<T, R?>.lte(value: R?) {
        ctx.run { this@lte lte value }
    }

    infix fun <R : Comparable<R>> PathRef<T, R?>.lte(value: R?) {
        ctx.run { this@lte lte value }
    }

    infix fun <R : Comparable<R>> KProperty1<T, R?>.between(range: ClosedRange<R>?) {
        ctx.run { this@between between range }
    }

    fun <R : Comparable<R>> KProperty1<T, R?>.between(lower: R?, upper: R?) {
        ctx.run { this@between.between(lower, upper) }
    }

    infix fun <R : Comparable<R>> PathRef<T, R?>.between(range: ClosedRange<R>?) {
        ctx.run { this@between between range }
    }

    fun <R : Comparable<R>> PathRef<T, R?>.between(lower: R?, upper: R?) {
        ctx.run { this@between.between(lower, upper) }
    }

    val KProperty1<T, *>.isNull: Unit
        get() {
            ctx.run { this@isNull.isNull }
        }

    val PathRef<T, *>.isNull: Unit
        get() {
            ctx.run { this@isNull.isNull }
        }

    val KProperty1<T, *>.isNotNull: Unit
        get() {
            ctx.run { this@isNotNull.isNotNull }
        }

    val PathRef<T, *>.isNotNull: Unit
        get() {
            ctx.run { this@isNotNull.isNotNull }
        }

    val KProperty1<T, Boolean?>.isTrue: Unit
        get() {
            ctx.run { this@isTrue.isTrue }
        }

    val KProperty1<T, Boolean?>.isFalse: Unit
        get() {
            ctx.run { this@isFalse.isFalse }
        }

    infix fun <R : Any> KProperty1<T, R?>.inList(values: Collection<R>?) {
        ctx.run { this@inList.inList(values) }
    }

    infix fun <R : Any> PathRef<T, R?>.inList(values: Collection<R>?) {
        ctx.run { this@inList.inList(values) }
    }

    infix fun <R : Any> KProperty1<T, R?>.notInList(values: Collection<R>?) {
        ctx.run { this@notInList.notInList(values) }
    }

    infix fun <R : Any> PathRef<T, R?>.notInList(values: Collection<R>?) {
        ctx.run { this@notInList.notInList(values) }
    }

    fun <R : Any> KProperty1<T, R?>.oneOf(vararg values: R?) {
        ctx.run { this@oneOf.oneOf(*values) }
    }

    fun <R : Any> PathRef<T, R?>.oneOf(vararg values: R?) {
        ctx.run { this@oneOf.oneOf(*values) }
    }

    infix fun KProperty1<T, String?>.like(pattern: String?) {
        ctx.run { this@like like pattern }
    }

    infix fun PathRef<T, String?>.like(pattern: String?) {
        ctx.run { this@like like pattern }
    }

    infix fun KProperty1<T, String?>.notLike(pattern: String?) {
        ctx.run { this@notLike.notLike(pattern) }
    }

    infix fun PathRef<T, String?>.notLike(pattern: String?) {
        ctx.run { this@notLike.notLike(pattern) }
    }

    infix fun KProperty1<T, String?>.ilike(pattern: String?) {
        ctx.run { this@ilike.ilike(pattern) }
    }

    infix fun PathRef<T, String?>.ilike(pattern: String?) {
        ctx.run { this@ilike.ilike(pattern) }
    }

    fun KProperty1<T, String?>.contains(text: String?, caseSensitive: Boolean = true) {
        ctx.run { this@contains.contains(text, caseSensitive) }
    }

    fun PathRef<T, String?>.contains(text: String?, caseSensitive: Boolean = true) {
        ctx.run { this@contains.contains(text, caseSensitive) }
    }

    fun KProperty1<T, String?>.startsWith(prefix: String?, caseSensitive: Boolean = true) {
        ctx.run { this@startsWith.startsWith(prefix, caseSensitive) }
    }

    fun KProperty1<T, String?>.endsWith(suffix: String?, caseSensitive: Boolean = true) {
        ctx.run { this@endsWith.endsWith(suffix, caseSensitive) }
    }

    fun KProperty1<T, Collection<*>?>.isEmpty() {
        ctx.run { this@isEmpty.isEmpty }
    }

    fun KProperty1<T, Collection<*>?>.isNotEmpty() {
        ctx.run { this@isNotEmpty.isNotEmpty }
    }

    fun and(block: FullFilteringScope<T>.() -> Unit) {
        ctx.and { FullFilteringScopeAdapter<T>(this).block() }
    }

    fun or(block: FullFilteringScope<T>.() -> Unit) {
        ctx.or { FullFilteringScopeAdapter<T>(this).block() }
    }

    fun not(block: FullFilteringScope<T>.() -> Unit) {
        ctx.not { FullFilteringScopeAdapter<T>(this).block() }
    }

    fun whereIf(condition: Boolean, block: FullFilteringScope<T>.() -> Unit): ConditionalClause<T> {
        return ctx.whereIf(condition) { FullFilteringScopeAdapter<T>(this).block() }
    }

    fun parade(
        idProperty: KProperty1<T, *>? = null,
        build: QueryBuilderScope<T>.() -> SearchQuery,
    ) {
        ctx.parade(idProperty, build)
    }

    fun paradeIf(
        condition: Boolean,
        idProperty: KProperty1<T, *>? = null,
        build: QueryBuilderScope<T>.() -> SearchQuery,
    ) {
        ctx.paradeIf(condition, idProperty, build)
    }
}

interface FullFilteringScope<T : Any> : PropertyFilteringScope<T>

internal class ExpressionFilteringScopeAdapter<T : Any>(override val ctx: WhereContext<T>) :
    ExpressionFilteringScope<T>

@PublishedApi
internal class FullFilteringScopeAdapter<T : Any>(override val ctx: WhereContext<T>) :
    FullFilteringScope<T>

inline fun <T : Any, J : Any> PropertyFilteringScope<T>.joinOnce(
    property: KProperty1<T, J?>,
    type: JoinType = JoinType.INNER,
    crossinline block: FullFilteringScope<J>.() -> Unit,
) {
    val whereBlock: WhereContext<J>.() -> Unit = { FullFilteringScopeAdapter<J>(this).block() }
    ctx.joinOnce(property, type, whereBlock)
}

@JvmName("joinOnceCollection")
inline fun <T : Any, J : Any> PropertyFilteringScope<T>.joinOnce(
    property: KProperty1<T, Collection<J>?>,
    type: JoinType = JoinType.INNER,
    crossinline block: FullFilteringScope<J>.() -> Unit,
) {
    val whereBlock: WhereContext<J>.() -> Unit = { FullFilteringScopeAdapter<J>(this).block() }
    ctx.joinOnce(property, type, whereBlock)
}

inline fun <T : Any, J : Any> PropertyFilteringScope<T>.join(
    property: KProperty1<T, J?>,
    type: JoinType = JoinType.INNER,
    crossinline block: FullFilteringScope<J>.() -> Unit,
) {
    val whereBlock: WhereContext<J>.() -> Unit = { FullFilteringScopeAdapter<J>(this).block() }
    ctx.join(property, type, whereBlock)
}

@JvmName("joinCollection")
inline fun <T : Any, J : Any> PropertyFilteringScope<T>.join(
    property: KProperty1<T, Collection<J>?>,
    type: JoinType = JoinType.INNER,
    crossinline block: FullFilteringScope<J>.() -> Unit,
) {
    val whereBlock: WhereContext<J>.() -> Unit = { FullFilteringScopeAdapter<J>(this).block() }
    ctx.join(property, type, whereBlock)
}

fun <T : Any, J : Any> PropertyFilteringScope<T>.fetch(
    property: KProperty1<T, J?>,
    type: JoinType = JoinType.LEFT,
) {
    ctx.fetch(property, type)
}

@JvmName("fetchCollection")
fun <T : Any, J : Any> PropertyFilteringScope<T>.fetch(
    property: KProperty1<T, Collection<J>?>,
    type: JoinType = JoinType.LEFT,
) {
    ctx.fetch(property, type)
}

inline fun <reified S : Any> PropertyFilteringScope<*>.exists(
    noinline block: FullFilteringScope<S>.() -> Unit
) {
    this.ctx.exists<S> { FullFilteringScopeAdapter(this).block() }
}

inline fun <reified S : Any> PropertyFilteringScope<*>.notExists(
    noinline block: FullFilteringScope<S>.() -> Unit
) {
    this.ctx.notExists<S> { FullFilteringScopeAdapter(this).block() }
}

inline fun <reified S : Any, reified R : Any, T : Any> PropertyFilteringScope<T>.subquery(
    crossinline block: SubqueryContext<S, T, R>.() -> Unit
): Expression<R> {
    return ctx.subquery<S, R> { this.block() }
}

inline fun <S : Any, X> FullFilteringScope<S>.col(ref: KProperty1<S, X>): Path<X> {
    return prop(ref).toPath(ctx.from)
}

fun <S : Any, X> FullFilteringScope<S>.col(ref: PathRef<S, X>): Path<X> {
    return ref.toPath(ctx.from)
}

inline fun <S : Any, reified A : Any, X> FullFilteringScope<S>.parent(
    ref: KProperty1<A, X>
): Path<X> = asSubWhere().parent(ref)

inline fun <S : Any, reified A : Any, X> FullFilteringScope<S>.parent(ref: PathRef<A, X>): Path<X> =
    asSubWhere().parent(ref)

inline fun <S : Any, reified A : Any, X> FullFilteringScope<S>.parentOf(
    ref: KProperty1<A, X>
): Path<X> = asSubWhere().parentOf(ref)

inline fun <S : Any, reified A : Any, X> FullFilteringScope<S>.parentOf(
    ref: PathRef<A, X>
): Path<X> = asSubWhere().parentOf(ref)

@PublishedApi
internal inline fun <S : Any> FullFilteringScope<S>.asSubWhere(): SubWhereContext<S> {
    return ctx as? SubWhereContext<S>
        ?: error("This operation is only available inside a subquery block")
}
