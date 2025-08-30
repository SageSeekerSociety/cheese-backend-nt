package org.rucca.cheese.common.persistent

import jakarta.persistence.criteria.*
import kotlin.reflect.KProperty1
import org.springframework.data.jpa.domain.Specification

/** DSL helper for building JPA specifications in a more Kotlin idiomatic way. */
fun <T> buildSpecification(block: SpecificationBuilder<T>.() -> Unit): Specification<T> {
    val builder = SpecificationBuilder<T>()
    builder.block()
    return builder.build()
}

/** Builder class for constructing JPA specifications. */
class SpecificationBuilder<T> {
    private var spec: Specification<T>? = null

    fun where(
        predicate: (Root<T>, CriteriaQuery<*>, CriteriaBuilder) -> Predicate?
    ): SpecificationBuilder<T> {
        spec = Specification.where { root, query, cb -> predicate(root, query!!, cb) }
        return this
    }

    fun and(
        predicate: (Root<T>, CriteriaQuery<*>, CriteriaBuilder) -> Predicate?
    ): SpecificationBuilder<T> {
        val newPart = Specification<T> { root, query, cb -> predicate(root, query!!, cb) }
        spec = if (spec != null) spec!!.and(newPart) else newPart
        return this
    }

    fun or(
        predicate: (Root<T>, CriteriaQuery<*>, CriteriaBuilder) -> Predicate?
    ): SpecificationBuilder<T> {
        val newPart = Specification<T> { root, query, cb -> predicate(root, query!!, cb) }
        spec = if (spec != null) spec!!.or(newPart) else newPart
        return this
    }

    fun build(): Specification<T> = spec ?: Specification.where(null)
}

/** Extensions */
fun <X, Y : Any> Root<X>.getProperty(property: KProperty1<X, Y?>): Path<Y> = get<Y>(property.name)

fun <X, Y : Any> Path<X>.getProperty(property: KProperty1<X, Y?>): Path<Y> = get<Y>(property.name)
