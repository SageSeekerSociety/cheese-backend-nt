package org.rucca.cheese.common.persistent.spec

import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Order
import kotlin.reflect.KProperty1
import org.rucca.cheese.common.query.internal.spec.PathRef
import org.rucca.cheese.common.query.internal.spec.prop
import org.springframework.data.domain.Sort

fun <T : Any> SpecContext<T>.orderBy(vararg orders: Order) {
    if (orders.isNotEmpty()) query.orderBy(*orders)
}

fun <T : Any, R> SpecContext<T>.asc(ref: KProperty1<T, R>): Order = cb.asc(prop(ref).toPath(root))

fun <T : Any, R> SpecContext<T>.desc(ref: KProperty1<T, R>): Order = cb.desc(prop(ref).toPath(root))

fun <T : Any, R> SpecContext<T>.asc(ref: PathRef<T, R>): Order = cb.asc(ref.toPath(root))

fun <T : Any, R> SpecContext<T>.desc(ref: PathRef<T, R>): Order = cb.desc(ref.toPath(root))

fun <T : Any> SpecContext<T>.ascIgnoreCase(ref: KProperty1<T, String?>): Order =
    cb.asc(cb.lower(prop(ref).toPath(root)))

fun <T : Any> SpecContext<T>.descIgnoreCase(ref: KProperty1<T, String?>): Order =
    cb.desc(cb.lower(prop(ref).toPath(root)))

fun <T : Any, R> SpecContext<T>.ascNullsLast(ref: KProperty1<T, R>): Array<Order> {
    val expr = prop(ref).toPath(root) as Expression<*>
    val nulls = cb.asc(cb.selectCase<Int>().`when`(cb.isNull(expr), 1).otherwise(0))
    return arrayOf(nulls, cb.asc(expr))
}

fun <T : Any, R> SpecContext<T>.descNullsLast(ref: KProperty1<T, R>): Array<Order> {
    val expr = prop(ref).toPath(root) as Expression<*>
    val nulls = cb.asc(cb.selectCase<Int>().`when`(cb.isNull(expr), 1).otherwise(0))
    return arrayOf(nulls, cb.desc(expr))
}

fun <T : Any, R> SpecContext<T>.ascNullsFirst(ref: KProperty1<T, R>): Array<Order> {
    val expr = prop(ref).toPath(root) as Expression<*>
    val nulls = cb.asc(cb.selectCase<Int>().`when`(cb.isNull(expr), 0).otherwise(1))
    return arrayOf(nulls, cb.asc(expr))
}

fun <T : Any, R> SpecContext<T>.descNullsFirst(ref: KProperty1<T, R>): Array<Order> {
    val expr = prop(ref).toPath(root) as Expression<*>
    val nulls = cb.asc(cb.selectCase<Int>().`when`(cb.isNull(expr), 0).otherwise(1))
    return arrayOf(nulls, cb.desc(expr))
}

fun <T : Any> SpecContext<T>.groupBy(vararg refs: KProperty1<T, *>) {
    query.groupBy(*refs.map { prop(it).toPath(root) as Expression<*> }.toTypedArray())
}

fun <T : Any> SpecContext<T>.groupByRefs(vararg refs: PathRef<T, *>) {
    query.groupBy(*refs.map { it.toPath(root) as Expression<*> }.toTypedArray())
}

fun <T : Any, X : Any> SpecContext<T>.fn(
    name: String,
    type: Class<X>,
    vararg args: Expression<*>,
): Expression<X> = cb.function(name, type, *args)

fun <T : Any> SpecContext<T>.orderByParadeScore(
    idProperty: KProperty1<T, *>? = null,
    direction: Sort.Direction = Sort.Direction.DESC,
) {
    val id = resolveIdProperty(idProperty)
    val isCount =
        (query.resultType == java.lang.Long::class.java || query.resultType == Long::class.java)
    if (isCount) return
    val scoreExpr = cb.function("pdb_score", Number::class.java, root.get<Any>(id.name))
    val order = if (direction.isAscending) cb.asc(scoreExpr) else cb.desc(scoreExpr)
    query.orderBy(order)
}
