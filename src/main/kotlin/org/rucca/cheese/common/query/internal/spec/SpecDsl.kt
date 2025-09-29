package org.rucca.cheese.common.query.internal.spec

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.From
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery
import jakarta.persistence.metamodel.EntityType
import kotlin.reflect.KProperty1
import org.rucca.cheese.common.query.internal.search.ParadeSearchConverter
import org.rucca.cheese.common.query.internal.search.QueryBuilderScope
import org.rucca.cheese.common.query.internal.search.SearchQuery
import org.rucca.cheese.common.query.internal.search.SearchQueryBuilder
import org.springframework.data.jpa.domain.Specification

@DslMarker annotation class SpecDsl

// --------------------------------------------------------------------------------------
// 1) Type-safe path references
// --------------------------------------------------------------------------------------

/**
 * Represents a lazy, type-safe reference to a JPA path built from property segments.
 *
 * @param E entity type at the root
 * @param R property type of the referenced path
 */
class PathRef<E, R>(internal val segments: List<String>) {
    @Suppress("UNCHECKED_CAST")
    fun toPath(from: From<*, E>): Path<R> {
        var p: Path<*> = from
        for (name in segments) p = p.get<Any>(name)
        return p as Path<R>
    }
}

/** Create a [PathRef] from a Kotlin property. */
fun <E, R> prop(p: KProperty1<E, R>): PathRef<E, R> = PathRef(listOf(p.name))

infix fun <E, A : Any, R> KProperty1<E, A?>.dot(next: KProperty1<A, R>): PathRef<E, R> =
    PathRef(listOf(this.name, next.name))

infix fun <E, A : Any, B> PathRef<E, A?>.dot(next: KProperty1<A, B>): PathRef<E, B> =
    PathRef(this.segments + next.name)

infix operator fun <E, A : Any, R> KProperty1<E, A?>.div(next: KProperty1<A, R>): PathRef<E, R> =
    this dot next

infix operator fun <E, A : Any, B> PathRef<E, A?>.div(next: KProperty1<A, B>): PathRef<E, B> =
    this dot next

// --------------------------------------------------------------------------------------
// 2) Entry point & top-level query context
// --------------------------------------------------------------------------------------

/**
 * Build a Spring Data JPA [Specification] using a fluent Kotlin DSL.
 *
 * Example:
 * ```
 * val byName = spec<User> {
 *   where {
 *     User::name.contains("alice", caseSensitive = false)
 *   }
 *   orderBy(asc(User::name))
 * }
 * ```
 */
inline fun <reified T : Any> spec(crossinline block: SpecContext<T>.() -> Unit): Specification<T> =
    Specification { root, cq, cb ->
        requireNotNull(cq) { "CriteriaQuery must not be null" }
        val ctx = SpecContext(cb, root, cq)
        ctx.block()
        ctx.buildPredicate()
    }

/** Top-level DSL scope for configuring `where` / `having` clauses. */
@SpecDsl
class SpecContext<T : Any>(
    val cb: CriteriaBuilder,
    val root: Root<T>,
    val query: CriteriaQuery<*>,
) {
    private var wherePredicate: Predicate? = null
    internal var defaultIdProp: KProperty1<T, *>? = null

    /**
     * Set default entity id property used by helpers such as [orderByParadeScore] and `parade{}`.
     */
    fun useId(ref: KProperty1<T, *>) {
        defaultIdProp = ref
    }

    fun resolveIdProperty(candidate: KProperty1<T, *>?): KProperty1<T, *> {
        return candidate
            ?: defaultIdProp
            ?: error("An id property must be configured via useId before invoking this operation")
    }

    /** Configure WHERE predicates. */
    fun where(block: FullFilteringScope<T>.() -> Unit) {
        val context = WhereContext(query, cb, root, defaultIdProp)
        FullFilteringScopeAdapter(context).block()
        wherePredicate = context.build()
    }

    /** Configure HAVING predicates for aggregated queries. */
    fun having(block: ExpressionFilteringScope<T>.() -> Unit) {
        val context = WhereContext(query, cb, root, defaultIdProp)
        ExpressionFilteringScopeAdapter(context).block()
        context.build()?.let { query.having(it) }
    }

    @PublishedApi internal fun buildPredicate(): Predicate? = wherePredicate

    /** Enable/disable `SELECT DISTINCT`. */
    fun distinct(enable: Boolean = true) {
        if (query.isDistinct != enable) query.distinct(enable)
    }
}

// --------------------------------------------------------------------------------------
// 3) WHERE / JOIN / SUBQUERY contexts
// --------------------------------------------------------------------------------------

class ConditionalClause<E : Any>(
    private val context: WhereContext<E>,
    private val conditionMet: Boolean,
) {
    /** Executes the given block if the original condition was false. */
    infix fun orElse(block: FullFilteringScope<E>.() -> Unit) {
        if (!conditionMet) {
            FullFilteringScopeAdapter(context).block()
        }
    }
}

/**
 * WHERE-scope DSL with join caching, logical composition, common predicates, subquery helpers, and
 * a ParadeDB full-text gateway.
 */
@Suppress("UNCHECKED_CAST")
@SpecDsl
open class WhereContext<E : Any>(
    val query: CriteriaQuery<*>,
    val cb: CriteriaBuilder,
    val from: From<*, E>,
    private val defaultIdProp: KProperty1<E, *>? = null,
) {
    private val predicates = mutableListOf<Predicate>()
    private val joins: MutableMap<String, Join<*, *>> = mutableMapOf()

    /** Add a predicate if non-null. */
    fun add(predicate: Predicate?) {
        predicate?.let { predicates.add(it) }
    }

    private fun addParadePredicate(query: SearchQuery, idProperty: KProperty1<E, *>) {
        add(ParadeSearchConverter.predicate(cb, from, idProperty, query))
    }

    /**
     * Add a ParadeDB full-text predicate using `pg_search(id, to_jsonb(query))`.
     *
     * @param idProperty entity id property; defaults to parent context [SpecContext.useId]
     * @param build block to build a [SearchQuery]
     */
    fun parade(
        idProperty: KProperty1<E, *>? = defaultIdProp,
        build: QueryBuilderScope<E>.() -> SearchQuery,
    ) {
        val id = requireNotNull(idProperty) { "parade{} requires an id property" }
        val queryObj = SearchQueryBuilder<E>().build()
        addParadePredicate(queryObj, id)
    }

    /** Conditional variant of [parade]. */
    fun paradeIf(
        condition: Boolean,
        idProperty: KProperty1<E, *>? = defaultIdProp,
        build: QueryBuilderScope<E>.() -> SearchQuery,
    ) {
        if (condition) parade(idProperty, build)
    }

    @PublishedApi
    internal fun build(): Predicate? =
        if (predicates.isEmpty()) null else cb.and(*predicates.toTypedArray())

    // ----- Boolean logic -----
    fun and(block: WhereContext<E>.() -> Unit) {
        val context = WhereContext(query, cb, from)
        context.block()
        add(context.build())
    }

    fun or(block: WhereContext<E>.() -> Unit) {
        val context = WhereContext(query, cb, from)
        context.block()
        if (context.predicates.isNotEmpty()) add(cb.or(*context.predicates.toTypedArray()))
    }

    fun not(block: WhereContext<E>.() -> Unit) {
        val context = WhereContext(query, cb, from)
        context.block()
        add(context.build()?.let { cb.not(it) })
    }

    /**
     * Applies a block of predicates if the condition is true, and allows chaining an `orElse`
     * block.
     *
     * Example:
     * ```
     * whereIf(name != null) {
     * User::name eq name
     * } orElse {
     * User::status eq Status.INACTIVE
     * }
     * ```
     */
    inline fun whereIf(
        condition: Boolean,
        block: WhereContext<E>.() -> Unit,
    ): ConditionalClause<E> {
        if (condition) {
            this.block()
        }
        return ConditionalClause(this, condition)
    }

    // ----- JOIN (cached) -----
    private fun keyOf(type: JoinType, name: String) = "$type:$name"

    fun <J : Any> joinOnce(
        prop: KProperty1<E, J?>,
        type: JoinType = JoinType.INNER,
        block: WhereContext<J>.() -> Unit,
    ) {
        val key = keyOf(type, prop.name)
        val j = (joins.getOrPut(key) { from.join<E, J>(prop.name, type) } as Join<*, J>)
        val context = WhereContext(query, cb, j)
        context.block()
        add(context.build())
    }

    @JvmName("joinOnceCollection")
    fun <J : Any> joinOnce(
        prop: KProperty1<E, Collection<J>?>,
        type: JoinType = JoinType.INNER,
        block: WhereContext<J>.() -> Unit,
    ) {
        val key = keyOf(type, prop.name)
        val j = (joins.getOrPut(key) { from.join<E, J>(prop.name, type) } as Join<*, J>)
        val context = WhereContext(query, cb, j)
        context.block()
        add(context.build())
    }

    fun <J : Any> join(
        prop: KProperty1<E, J?>,
        type: JoinType = JoinType.INNER,
        block: WhereContext<J>.() -> Unit,
    ) = joinOnce(prop, type, block)

    @JvmName("joinCollection")
    fun <J : Any> join(
        prop: KProperty1<E, Collection<J>?>,
        type: JoinType = JoinType.INNER,
        block: WhereContext<J>.() -> Unit,
    ) = joinOnce(prop, type, block)

    // ----- FETCH (auto DISTINCT for collections) -----
    fun <J : Any> fetch(prop: KProperty1<E, J?>, type: JoinType = JoinType.LEFT) {
        from.fetch<E, J>(prop.name, type)
    }

    @JvmName("fetchCollection")
    fun <J : Any> fetch(prop: KProperty1<E, Collection<J>?>, type: JoinType = JoinType.LEFT) {
        from.fetch<E, J>(prop.name, type)
        if (!query.isDistinct) query.distinct(true)
    }

    // ----- Subqueries -----
    inline fun <reified S : Any> exists(crossinline block: SubWhereContext<S>.() -> Unit) {
        val sq = query.subquery(Int::class.java)
        val subRoot = sq.from(S::class.java)

        val chain: List<Ancestor> =
            when (this) {
                is SubWhereContext<*> -> this.ancestors + currentAncestor(this.from)
                else -> listOf(currentAncestor(this.from))
            }

        val subWhereCtx = SubWhereContext(query, cb, subRoot, chain)
        subWhereCtx.block()
        subWhereCtx.build()?.let { sq.where(it) }

        sq.select(cb.literal(1)) // Default SELECT 1 for EXISTS check
        add(cb.exists(sq))
    }

    inline fun <reified S : Any> notExists(crossinline block: SubWhereContext<S>.() -> Unit) {
        val sq = query.subquery(Int::class.java)
        val subRoot = sq.from(S::class.java)

        val chain: List<Ancestor> =
            when (this) {
                is SubWhereContext<*> -> this.ancestors + currentAncestor(this.from)
                else -> listOf(currentAncestor(this.from))
            }

        val subWhereCtx = SubWhereContext(query, cb, subRoot, chain)
        subWhereCtx.block()
        subWhereCtx.build()?.let { sq.where(it) }

        sq.select(cb.literal(1)) // Default SELECT 1 for NOT EXISTS check
        add(cb.not(cb.exists(sq)))
    }

    /**
     * Creates a scalar subquery that returns a single value Expression. This can be used in
     * comparisons like `eq`, `gt`, `inList`, etc.
     *
     * The user is responsible for calling `select`, `selectExpr`, or `selectOne` inside the block.
     *
     * @param S The entity type in the subquery's FROM clause.
     * @param R The return type of the subquery's selection.
     * @param block The configuration block for the subquery.
     * @return An Expression representing the configured subquery.
     */
    inline fun <reified S : Any, reified R : Any> subquery(
        crossinline block: SubqueryContext<S, E, R>.() -> Unit
    ): Expression<R> {
        val sq = query.subquery(S::class.java.classLoader.loadClass(R::class.java.name) as Class<R>)
        val subRoot = sq.from(S::class.java)

        val chain: List<Ancestor> =
            when (this) {
                is SubWhereContext<*> -> this.ancestors + currentAncestor(this.from)
                else -> listOf(currentAncestor(this.from))
            }

        val sctx =
            SubqueryContext<S, E, R>(
                mainQuery = query,
                cb = cb,
                parentFrom = this.from,
                sq = sq,
                from = subRoot,
                ancestors = chain,
            )

        sctx.block()

        require(sctx.hasSelect) {
            "A select call (select, selectExpr, or selectOne) is required inside a subquery block."
        }

        return sq
    }

    // ----- Equality / inequality -----
    infix fun <R : Any> KProperty1<E, R?>.eq(value: R?) {
        add(value?.let { cb.equal(prop(this).toPath(from), it) })
    }

    infix fun <R : Any> PathRef<E, R?>.eq(value: R?) {
        add(value?.let { cb.equal(this.toPath(from), it) })
    }

    infix fun <R> Expression<R>.eq(value: R?) {
        add(value?.let { cb.equal(this, it) })
    }

    infix fun Expression<*>.eq(other: Expression<*>) {
        add(cb.equal(this, other))
    }

    infix fun <R : Any> KProperty1<E, R?>.notEq(value: R?) {
        add(value?.let { cb.notEqual(prop(this).toPath(from), it) })
    }

    infix fun <R : Any> PathRef<E, R?>.notEq(value: R?) {
        add(value?.let { cb.notEqual(this.toPath(from), it) })
    }

    infix fun <R> Expression<R>.notEq(value: R?) {
        add(value?.let { cb.notEqual(this, it) })
    }

    infix fun Expression<*>.notEq(other: Expression<*>) {
        add(cb.notEqual(this, other))
    }

    // ----- Range comparisons -----
    infix fun <R : Comparable<R>> KProperty1<E, R?>.gt(value: R?) {
        add(value?.let { cb.greaterThan(prop(this).toPath(from) as Path<R>, it) })
    }

    infix fun <R : Comparable<R>> PathRef<E, R?>.gt(value: R?) {
        add(value?.let { cb.greaterThan(this.toPath(from) as Path<R>, it) })
    }

    infix fun <R : Comparable<R>> Expression<R>.gt(value: R?) {
        add(value?.let { cb.greaterThan(this, it) })
    }

    infix fun <R : Comparable<R>> KProperty1<E, R?>.gte(value: R?) {
        add(value?.let { cb.greaterThanOrEqualTo(prop(this).toPath(from) as Path<R>, it) })
    }

    infix fun <R : Comparable<R>> PathRef<E, R?>.gte(value: R?) {
        add(value?.let { cb.greaterThanOrEqualTo(this.toPath(from) as Path<R>, it) })
    }

    infix fun <R : Comparable<R>> Expression<R>.gte(value: R?) {
        add(value?.let { cb.greaterThanOrEqualTo(this, it) })
    }

    infix fun <R : Comparable<R>> KProperty1<E, R?>.lt(value: R?) {
        add(value?.let { cb.lessThan(prop(this).toPath(from) as Path<R>, it) })
    }

    infix fun <R : Comparable<R>> PathRef<E, R?>.lt(value: R?) {
        add(value?.let { cb.lessThan(this.toPath(from) as Path<R>, it) })
    }

    infix fun <R : Comparable<R>> Expression<R>.lt(value: R?) {
        add(value?.let { cb.lessThan(this, it) })
    }

    infix fun <R : Comparable<R>> KProperty1<E, R?>.lte(value: R?) {
        add(value?.let { cb.lessThanOrEqualTo(prop(this).toPath(from) as Path<R>, it) })
    }

    infix fun <R : Comparable<R>> PathRef<E, R?>.lte(value: R?) {
        add(value?.let { cb.lessThanOrEqualTo(this.toPath(from) as Path<R>, it) })
    }

    infix fun <R : Comparable<R>> Expression<R>.lte(value: R?) {
        add(value?.let { cb.lessThanOrEqualTo(this, it) })
    }

    // ----- BETWEEN / half-open range helpers -----
    infix fun <R : Comparable<R>> KProperty1<E, R?>.between(range: ClosedRange<R>?) {
        if (range != null)
            add(cb.between(prop(this).toPath(from) as Path<R>, range.start, range.endInclusive))
    }

    fun <R : Comparable<R>> KProperty1<E, R?>.between(lo: R?, hi: R?) {
        if (lo != null && hi != null) add(cb.between(prop(this).toPath(from) as Path<R>, lo, hi))
        else if (lo != null) this gte lo else if (hi != null) this lte hi
    }

    infix fun <R : Comparable<R>> PathRef<E, R?>.between(range: ClosedRange<R>?) {
        if (range != null)
            add(cb.between(this.toPath(from) as Path<R>, range.start, range.endInclusive))
    }

    fun <R : Comparable<R>> PathRef<E, R?>.between(lo: R?, hi: R?) {
        if (lo != null && hi != null) add(cb.between(this.toPath(from) as Path<R>, lo, hi))
        else if (lo != null) this gte lo else if (hi != null) this lte hi
    }

    // ----- NULL checks -----
    val KProperty1<E, *>.isNull: Unit
        get() {
            add(cb.isNull(prop(this).toPath(from)))
        }

    val PathRef<E, *>.isNull: Unit
        get() {
            add(cb.isNull(this.toPath(from)))
        }

    val Expression<*>.isNull: Unit
        get() {
            add(cb.isNull(this))
        }

    val KProperty1<E, *>.isNotNull: Unit
        get() {
            add(cb.isNotNull(prop(this).toPath(from)))
        }

    val PathRef<E, *>.isNotNull: Unit
        get() {
            add(cb.isNotNull(this.toPath(from)))
        }

    val Expression<*>.isNotNull: Unit
        get() {
            add(cb.isNotNull(this))
        }

    // ----- Boolean helpers -----
    val KProperty1<E, Boolean?>.isTrue: Unit
        get() {
            add(cb.isTrue(prop(this).toPath(from) as Expression<Boolean>))
        }

    val KProperty1<E, Boolean?>.isFalse: Unit
        get() {
            add(cb.isFalse(prop(this).toPath(from) as Expression<Boolean>))
        }

    // ----- IN / NOT IN -----
    infix fun <R : Any> KProperty1<E, R?>.inList(values: Collection<R>?) {
        if (!values.isNullOrEmpty()) add((prop(this).toPath(from) as Expression<R>).`in`(values))
    }

    infix fun <R : Any> PathRef<E, R?>.inList(values: Collection<R>?) {
        if (!values.isNullOrEmpty()) add((this.toPath(from) as Expression<R>).`in`(values))
    }

    infix fun <R : Any> Expression<R?>.inList(values: Collection<R>?) {
        if (!values.isNullOrEmpty()) add(this.`in`(values))
    }

    fun <R : Any> KProperty1<E, R?>.oneOf(vararg values: R?) {
        val vs = values.filterNotNull()
        if (vs.isNotEmpty()) add((prop(this).toPath(from) as Expression<R>).`in`(vs))
    }

    fun <R : Any> PathRef<E, R?>.oneOf(vararg values: R?) {
        val vs = values.filterNotNull()
        if (vs.isNotEmpty()) add((this.toPath(from) as Expression<R>).`in`(vs))
    }

    infix fun <R : Any> KProperty1<E, R?>.notInList(values: Collection<R>?) {
        if (!values.isNullOrEmpty())
            add(cb.not((prop(this).toPath(from) as Expression<R>).`in`(values)))
    }

    infix fun <R : Any> PathRef<E, R?>.notInList(values: Collection<R>?) {
        if (!values.isNullOrEmpty()) add(cb.not((this.toPath(from) as Expression<R>).`in`(values)))
    }

    infix fun <R : Any> Expression<R?>.notInList(values: Collection<R>?) {
        if (!values.isNullOrEmpty()) add(cb.not(this.`in`(values)))
    }

    // ----- LIKE / ILIKE -----
    infix fun KProperty1<E, String?>.like(pattern: String?) {
        add(pattern?.let { cb.like(prop(this).toPath(from) as Path<String>, it) })
    }

    infix fun PathRef<E, String?>.like(pattern: String?) {
        add(pattern?.let { cb.like(this.toPath(from) as Path<String>, it) })
    }

    infix fun Expression<String>.like(pattern: String?) {
        add(pattern?.let { cb.like(this, it) })
    }

    infix fun KProperty1<E, String?>.notLike(pattern: String?) {
        add(pattern?.let { cb.notLike(prop(this).toPath(from) as Path<String>, it) })
    }

    infix fun PathRef<E, String?>.notLike(pattern: String?) {
        add(pattern?.let { cb.notLike(this.toPath(from) as Path<String>, it) })
    }

    infix fun Expression<String>.notLike(pattern: String?) {
        add(pattern?.let { cb.notLike(this, it) })
    }

    infix fun KProperty1<E, String?>.ilike(pattern: String?) {
        add(
            pattern?.let {
                cb.like(cb.lower(prop(this).toPath(from) as Path<String>), it.lowercase())
            }
        )
    }

    infix fun PathRef<E, String?>.ilike(pattern: String?) {
        add(pattern?.let { cb.like(cb.lower(this.toPath(from) as Path<String>), it.lowercase()) })
    }

    infix fun Expression<String>.ilike(pattern: String?) {
        add(pattern?.let { cb.like(cb.lower(this), it.lowercase()) })
    }

    // String helpers
    fun KProperty1<E, String?>.contains(s: String?, caseSensitive: Boolean = true) {
        if (s == null) return
        if (caseSensitive) this like "%$s%" else this ilike "%$s%"
    }

    fun PathRef<E, String?>.contains(s: String?, caseSensitive: Boolean = true) {
        if (s == null) return
        if (caseSensitive) this like "%$s%" else this ilike "%$s%"
    }

    fun Expression<String>.contains(s: String?, caseSensitive: Boolean = true) {
        if (s == null) return
        if (caseSensitive) this like "%$s%" else this ilike "%$s%"
    }

    fun KProperty1<E, String?>.startsWith(s: String?, caseSensitive: Boolean = true) {
        if (s == null) return
        if (caseSensitive) this like "$s%" else this ilike "$s%"
    }

    fun KProperty1<E, String?>.endsWith(s: String?, caseSensitive: Boolean = true) {
        if (s == null) return
        if (caseSensitive) this like "%$s" else this ilike "%$s"
    }

    // Collection emptiness
    val KProperty1<E, Collection<*>?>.isEmpty: Unit
        get() {
            @Suppress("UNCHECKED_CAST")
            add(cb.isEmpty(prop(this).toPath(from) as Expression<Collection<*>>))
        }

    val KProperty1<E, Collection<*>?>.isNotEmpty: Unit
        get() {
            @Suppress("UNCHECKED_CAST")
            add(cb.isNotEmpty(prop(this).toPath(from) as Expression<Collection<*>>))
        }

    // Common SQL function gateways
    fun <X : Any> fn(name: String, type: Class<X>, vararg args: Expression<*>): Expression<X> =
        cb.function(name, type, *args)

    fun lower(ref: KProperty1<E, String?>): Expression<String> =
        cb.lower(prop(ref).toPath(from) as Path<String>)

    fun lower(ref: PathRef<E, String?>): Expression<String> =
        cb.lower(ref.toPath(from) as Path<String>)

    fun lower(e: Expression<String>): Expression<String?> = cb.lower(e)

    fun upper(ref: KProperty1<E, String?>): Expression<String> =
        cb.upper(prop(ref).toPath(from) as Path<String>)

    fun upper(ref: PathRef<E, String?>): Expression<String> =
        cb.upper(ref.toPath(from) as Path<String>)

    fun upper(e: Expression<String>): Expression<String?> = cb.upper(e)

    fun length(e: Expression<String>): Expression<Int?> = cb.length(e)
}

/**
 * Subquery builder scope. Provides access to:
 * - `from`: subquery root entity
 * - `parentFrom`: direct parent `From` of the enclosing context
 * - `ancestors`: chain of all ancestors to support deep nesting
 */
@SpecDsl
class SubqueryContext<E : Any, P : Any, R : Any>(
    val mainQuery: CriteriaQuery<*>,
    val cb: CriteriaBuilder,
    val parentFrom: From<*, P>,
    val sq: Subquery<R>,
    val from: Root<E>,
    val ancestors: List<Ancestor> = emptyList(),
) {
    @PublishedApi
    internal var hasSelect: Boolean = false
        private set

    /** Select a literal value. */
    fun selectOne(value: R) {
        sq.select(cb.literal(value))
        hasSelect = true
    }

    /** Select a column of the subquery root. */
    fun select(ref: KProperty1<E, R>) {
        @Suppress("UNCHECKED_CAST") sq.select(prop(ref).toPath(from) as Expression<R>)
        hasSelect = true
    }

    /** Select an arbitrary expression. */
    fun selectExpr(expr: Expression<R>) {
        sq.select(expr)
        hasSelect = true
    }

    /** Configure subquery WHERE predicates. */
    fun where(block: SubWhereContext<E>.() -> Unit) {
        val ctx = SubWhereContext<E>(mainQuery, cb, from, ancestors)
        ctx.block()
        ctx.build()?.let { sq.where(it) }
    }

    // Column accessors
    fun <X> col(ref: KProperty1<E, X>): Path<X> = prop(ref).toPath(from)

    fun <X> col(ref: PathRef<E, X>): Path<X> = ref.toPath(from)

    fun <X> parent(ref: KProperty1<P, X>): Path<X> = prop(ref).toPath(parentFrom)

    fun <X> parent(ref: PathRef<P, X>): Path<X> = ref.toPath(parentFrom)

    fun <N : Number> KProperty1<E, N?>.max(): Expression<N> = cb.max(col(this))

    fun <N : Number> KProperty1<E, N?>.min(): Expression<N> = cb.min(col(this))

    fun <N : Number> KProperty1<E, N?>.avg(): Expression<Double> = cb.avg(col(this))

    fun <N : Number> KProperty1<E, N?>.sum(): Expression<N> = cb.sum(col(this))

    fun KProperty1<E, *>.count(): Expression<Long> = cb.count(col(this))

    fun KProperty1<E, *>.countDistinct(): Expression<Long> = cb.countDistinct(col(this))
}

/** Records an ancestor `From` chain node to enable deep subquery parent access. */
data class Ancestor(val type: Class<*>, val from: From<*, *>)

@Suppress("UNCHECKED_CAST")
fun currentAncestor(from: From<*, *>): Ancestor {
    val et = (from.model as EntityType<*>).javaType
    return Ancestor(et, from)
}

/**
 * WHERE scope specialized for subqueries. It exposes:
 * - `col(...)` to access columns of the subquery root
 * - `parent(...)` to access the direct parent `From`
 * - `parentOf<A>(...)` to access any ancestor `From` by entity type
 */
@SpecDsl
class SubWhereContext<S : Any>(
    query: CriteriaQuery<*>,
    cb: CriteriaBuilder,
    val fromS: From<*, S>,
    val ancestors: List<Ancestor>,
) : WhereContext<S>(query, cb, fromS) {

    fun <X> col(ref: KProperty1<S, X>): Path<X> = prop(ref).toPath(fromS)

    fun <X> col(ref: PathRef<S, X>): Path<X> = ref.toPath(fromS)

    inline fun <reified A : Any, X> parent(ref: KProperty1<A, X>): Path<X> = parentOf(ref)

    inline fun <reified A : Any, X> parent(ref: PathRef<A, X>): Path<X> = parentOf(ref)

    inline fun <reified A : Any, X> parentOf(ref: KProperty1<A, X>): Path<X> =
        prop(ref).toPath(findAncestor<A>())

    inline fun <reified A : Any, X> parentOf(ref: PathRef<A, X>): Path<X> =
        ref.toPath(findAncestor<A>())

    @Suppress("UNCHECKED_CAST")
    inline fun <reified A : Any> findAncestor(): From<*, A> {
        val cls = A::class.java
        val hit =
            ancestors.firstOrNull { it.type == cls }
                ?: error("No ancestor of type ${cls.name} in subquery chain")
        return hit.from as From<*, A>
    }
}
