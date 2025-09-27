package org.rucca.cheese.common.persistent.spec

import jakarta.persistence.Tuple
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Selection
import kotlin.reflect.KProperty1
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.rucca.cheese.common.pagination.model.TypedCompositeCursor
import org.rucca.cheese.common.pagination.model.doubleValue
import org.rucca.cheese.common.pagination.spec.CursorProjection
import org.rucca.cheese.common.pagination.spec.CursorProjectionSupport
import org.rucca.cheese.common.pagination.spec.CursorSpecification
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification

// =====================================================================================
// I. Core Models for ParadeDB Queries (using kotlinx.serialization)
// =====================================================================================

/**
 * Shared Json instance for serialization. `encodeDefaults = false` produces cleaner JSON by
 * omitting properties with default values.
 */
private val jsonMapper = Json { encodeDefaults = false }

/** A sealed interface representing all possible ParadeDB query clauses. */
@Serializable
sealed interface SearchQuery {
    /** Converts the query object to a kotlinx.serialization JsonElement. */
    fun toJsonElement(): JsonElement

    /** Serializes the query object to a JSON string. */
    fun toJsonString(): String = jsonMapper.encodeToString(toJsonElement())
}

/**
 * Represents a boolean combination of other query clauses.
 *
 * @see <https://docs.paradedb.com/documentation/advanced/compound/boolean>
 */
@Serializable
@SerialName("boolean")
data class BooleanQuery(
    @SerialName("must") val must: List<SearchQuery> = emptyList(),
    @SerialName("should") val should: List<SearchQuery> = emptyList(),
    @SerialName("must_not") val mustNot: List<SearchQuery> = emptyList(),
) : SearchQuery {
    override fun toJsonElement(): JsonElement = buildJsonObject {
        val boolContent = buildJsonObject {
            if (must.isNotEmpty()) put("must", JsonArray(must.map { it.toJsonElement() }))
            if (should.isNotEmpty()) put("should", JsonArray(should.map { it.toJsonElement() }))
            if (mustNot.isNotEmpty()) put("must_not", JsonArray(mustNot.map { it.toJsonElement() }))
        }
        put("boolean", boolContent)
    }
}

/**
 * Represents a full-text `match` query.
 *
 * @see <https://docs.paradedb.com/documentation/advanced/full-text/match>
 */
@Serializable
@SerialName("match")
data class MatchQuery(
    val field: String,
    val value: String,
    val distance: Int? = null,
    @SerialName("conjunction_mode") val conjunctionMode: Boolean? = null,
) : SearchQuery {
    override fun toJsonElement(): JsonElement = buildJsonObject {
        put("match", jsonMapper.encodeToJsonElement(this@MatchQuery))
    }
}

/**
 * Represents an exact `term` query.
 *
 * @see <https://docs.paradedb.com/documentation/advanced/term/term>
 */
@Serializable
@SerialName("term")
data class TermQuery(val field: String, val value: JsonPrimitive) : SearchQuery {
    override fun toJsonElement(): JsonElement = buildJsonObject {
        put("term", jsonMapper.encodeToJsonElement(this@TermQuery))
    }
}

/**
 * Represents a `phrase` query for matching sequences of terms.
 *
 * @see <https://docs.paradedb.com/documentation/advanced/phrase/phrase>
 */
@Serializable
@SerialName("phrase")
data class PhraseQuery(val field: String, val phrases: List<String>, val slop: Int? = null) :
    SearchQuery {
    override fun toJsonElement(): JsonElement = buildJsonObject {
        put("phrase", jsonMapper.encodeToJsonElement(this@PhraseQuery))
    }
}

/**
 * Represents a `range` query.
 *
 * @see <https://docs.paradedb.com/documentation/advanced/json/range>
 */
@Serializable
@SerialName("range")
data class RangeQuery(
    val field: String,
    val gte: JsonPrimitive? = null,
    val gt: JsonPrimitive? = null,
    val lte: JsonPrimitive? = null,
    val lt: JsonPrimitive? = null,
) : SearchQuery {
    override fun toJsonElement(): JsonElement = buildJsonObject {
        val rangeContent = buildJsonObject {
            put("field", JsonPrimitive(field))
            gte?.let { put("gte", it) }
            gt?.let { put("gt", it) }
            lte?.let { put("lte", it) }
            lt?.let { put("lt", it) }
        }
        put("range", rangeContent)
    }
}

/**
 * Represents a `boost` query to influence the relevance score.
 *
 * @see <https://docs.paradedb.com/documentation/full-text/boosting>
 */
@Serializable
@SerialName("boost")
data class BoostQuery(val query: SearchQuery, val factor: Double) : SearchQuery {
    override fun toJsonElement(): JsonElement = buildJsonObject {
        put(
            "boost",
            buildJsonObject {
                put("query", query.toJsonElement())
                put("factor", JsonPrimitive(factor))
            },
        )
    }
}

// =====================================================================================
// II. Kotlin DSL Builders
// =====================================================================================

/** Top-level entry point for the ParadeDB search DSL. */
fun <T> paradeSearch(block: SearchQueryBuilder<T>.() -> SearchQuery): SearchQuery {
    return SearchQueryBuilder<T>().block()
}

/** Defines the available functions within the DSL scope for a given entity type `T`. */
interface QueryBuilderScope<T> {
    fun match(
        field: KProperty1<T, Any?>,
        value: String,
        block: MatchQueryBuilder.() -> Unit = {},
    ): MatchQuery

    fun term(field: KProperty1<T, Any?>, value: String): TermQuery

    fun term(field: KProperty1<T, Any?>, value: Number): TermQuery

    fun term(field: KProperty1<T, Any?>, value: Boolean): TermQuery

    fun phrase(
        field: KProperty1<T, Any?>,
        vararg terms: String,
        block: PhraseQueryBuilder.() -> Unit = {},
    ): PhraseQuery

    fun range(field: KProperty1<T, Any?>, block: RangeBuilder.() -> Unit): RangeQuery

    fun bool(block: BooleanQueryBuilder<T>.() -> Unit): BooleanQuery

    infix fun SearchQuery.boost(factor: Number): BoostQuery
}

/** Collects query clauses for `must`, `should`, and `mustNot` lists. */
class SearchQueryBuilder<T> : QueryBuilderScope<T> {
    internal val queries = mutableListOf<SearchQuery>()

    private fun <Q : SearchQuery> add(query: Q): Q {
        queries.add(query)
        return query
    }

    override fun match(
        field: KProperty1<T, Any?>,
        value: String,
        block: MatchQueryBuilder.() -> Unit,
    ): MatchQuery {
        val builder = MatchQueryBuilder(value).apply(block)
        return add(MatchQuery(field.name, builder.value, builder.distance, builder.conjunctionMode))
    }

    override fun term(field: KProperty1<T, Any?>, value: String): TermQuery =
        add(TermQuery(field.name, JsonPrimitive(value)))

    override fun term(field: KProperty1<T, Any?>, value: Number): TermQuery =
        add(TermQuery(field.name, JsonPrimitive(value)))

    override fun term(field: KProperty1<T, Any?>, value: Boolean): TermQuery =
        add(TermQuery(field.name, JsonPrimitive(value)))

    override fun phrase(
        field: KProperty1<T, Any?>,
        vararg terms: String,
        block: PhraseQueryBuilder.() -> Unit,
    ): PhraseQuery {
        val builder = PhraseQueryBuilder(terms.toList()).apply(block)
        return add(PhraseQuery(field.name, builder.terms, builder.slop))
    }

    override fun range(field: KProperty1<T, Any?>, block: RangeBuilder.() -> Unit): RangeQuery {
        val builder = RangeBuilder().apply(block)
        return add(RangeQuery(field.name, builder.gte, builder.gt, builder.lte, builder.lt))
    }

    override fun bool(block: BooleanQueryBuilder<T>.() -> Unit): BooleanQuery =
        add(BooleanQueryBuilder<T>().apply(block).build())

    override infix fun SearchQuery.boost(factor: Number): BoostQuery {
        queries.remove(this)
        val boostedQuery = BoostQuery(this, factor.toDouble())
        return add(boostedQuery)
    }
}

/** Builder for constructing a `BooleanQuery` with `must`, `should`, and `mustNot` clauses. */
class BooleanQueryBuilder<T> {
    private val mustClauses = mutableListOf<SearchQuery>()
    private val shouldClauses = mutableListOf<SearchQuery>()
    private val mustNotClauses = mutableListOf<SearchQuery>()

    fun must(block: SearchQueryBuilder<T>.() -> Unit) {
        mustClauses.addAll(SearchQueryBuilder<T>().apply(block).queries)
    }

    fun should(block: SearchQueryBuilder<T>.() -> Unit) {
        shouldClauses.addAll(SearchQueryBuilder<T>().apply(block).queries)
    }

    fun mustNot(block: SearchQueryBuilder<T>.() -> Unit) {
        mustNotClauses.addAll(SearchQueryBuilder<T>().apply(block).queries)
    }

    internal fun build() = BooleanQuery(mustClauses, shouldClauses, mustNotClauses)
}

// Auxiliary builders for optional parameters
class MatchQueryBuilder(val value: String) {
    var distance: Int? = null
    var conjunctionMode: Boolean? = null
}

class PhraseQueryBuilder(val terms: List<String>) {
    var slop: Int? = null
}

class RangeBuilder {
    var gte: JsonPrimitive? = null
    var gt: JsonPrimitive? = null
    var lte: JsonPrimitive? = null
    var lt: JsonPrimitive? = null

    fun gte(value: String) {
        this.gte = JsonPrimitive(value)
    }

    fun gte(value: Number) {
        this.gte = JsonPrimitive(value)
    }

    fun gt(value: String) {
        this.gt = JsonPrimitive(value)
    }

    fun gt(value: Number) {
        this.gt = JsonPrimitive(value)
    }

    fun lte(value: String) {
        this.lte = JsonPrimitive(value)
    }

    fun lte(value: Number) {
        this.lte = JsonPrimitive(value)
    }

    fun lt(value: String) {
        this.lt = JsonPrimitive(value)
    }

    fun lt(value: Number) {
        this.lt = JsonPrimitive(value)
    }
}

// =====================================================================================
// III. Spring Data JPA Integration
// =====================================================================================

/**
 * Converts a `SearchQuery` into a JPA `Specification`.
 *
 * @param query The `SearchQuery` object built by the DSL.
 * @param idProperty A property reference to the entity's primary key (e.g., `Entity::id`).
 * @return A `Specification` for use with a `JpaSpecificationExecutor`.
 */
fun <T> paradeSpecification(query: SearchQuery, idProperty: KProperty1<T, *>): Specification<T> {
    return Specification { root, _, criteriaBuilder ->
        val jsonbExpression =
            criteriaBuilder.function(
                "to_jsonb",
                Object::class.java,
                criteriaBuilder.literal(query.toJsonString()),
            )
        criteriaBuilder.isTrue(
            criteriaBuilder.function(
                "pg_search",
                Boolean::class.javaObjectType,
                root.get<Any>(idProperty.name),
                jsonbExpression,
            )
        )
    }
}

/**
 * An extension function to apply relevance-based sorting to an existing `Specification`. This
 * provides a clean, compositional API for adding the `ORDER BY paradedb.score()` clause.
 */
fun <T> Specification<T>.withParadeOrder(
    idProperty: KProperty1<T, *>,
    direction: Sort.Direction = Sort.Direction.DESC,
): Specification<T> {
    return this.and { root, criteriaQuery, criteriaBuilder ->
        // Use a null-safe call on criteriaQuery, as it can be null during count projections.
        criteriaQuery?.let { query ->
            // Do not apply ordering to count queries.
            if (
                query.resultType != Long::class.java &&
                    query.resultType != Long::class.javaObjectType
            ) {
                val scoreExpression =
                    criteriaBuilder.function(
                        "pdb_score",
                        Number::class.java,
                        root.get<Any>(idProperty.name),
                    )
                val order: Order =
                    if (direction.isAscending) {
                        criteriaBuilder.asc(scoreExpression)
                    } else {
                        criteriaBuilder.desc(scoreExpression)
                    }
                query.orderBy(order)
            }
        }
        // This predicate is conjunctive and does not add any filtering conditions.
        criteriaBuilder.conjunction()
    }
}

/**
 * Convenience helper to build a ParadeDB cursor specification from a search query and optional
 * additional filters.
 */
fun <T, ID : Comparable<ID>> paradeCursorSpecification(
    domainClass: Class<T>,
    idProperty: KProperty1<T, ID?>,
    searchQuery: SearchQuery,
    additionalSpec: Specification<T>? = null,
    direction: Sort.Direction = Sort.Direction.DESC,
    scoreAlias: String = ParadeCursorSpecification.SCORE_FIELD,
): ParadeCursorSpecification<T, ID> {
    val searchSpec = paradeSpecification<T>(searchQuery, idProperty)
    val combined = additionalSpec?.let { searchSpec.and(it) } ?: searchSpec
    return ParadeCursorSpecification(
        domainClass = domainClass,
        idProperty = idProperty,
        baseSpec = combined,
        direction = direction,
        scoreAlias = scoreAlias,
    )
}

/**
 * Cursor specification tailored for ParadeDB full-text search. It captures both the BM25 score and
 * the entity id in the cursor, ensuring deterministic ordering between pages without relying on
 * entity properties for the score.
 */
class ParadeCursorSpecification<T, ID : Comparable<ID>>(
    private val domainClass: Class<T>,
    internal val idProperty: KProperty1<T, ID?>,
    private val baseSpec: Specification<T>,
    private val direction: Sort.Direction = Sort.Direction.DESC,
    internal val scoreAlias: String = SCORE_FIELD,
) :
    CursorSpecification<T, TypedCompositeCursor<T>>,
    CursorProjectionSupport<T, TypedCompositeCursor<T>> {

    companion object {
        const val SCORE_FIELD = "pdbscore"
    }

    override fun toPredicate(
        root: Root<T>,
        query: CriteriaQuery<*>,
        criteriaBuilder: CriteriaBuilder,
    ): Predicate {
        return baseSpec.toPredicate(root, query, criteriaBuilder) ?: criteriaBuilder.conjunction()
    }

    override fun toCursorPredicate(
        root: Root<T>,
        query: CriteriaQuery<*>,
        criteriaBuilder: CriteriaBuilder,
        cursor: TypedCompositeCursor<T>?,
    ): Predicate? {
        if (cursor == null) return null

        val cursorScore = cursor.values[scoreAlias]?.doubleValue ?: return null
        val scoreExpr = scoreExpression(root, criteriaBuilder)

        return when (direction) {
            Sort.Direction.DESC -> criteriaBuilder.lt(scoreExpr, cursorScore)
            Sort.Direction.ASC -> criteriaBuilder.gt(scoreExpr, cursorScore)
        }
    }

    override fun getSort(): Sort = Sort.unsorted()

    override fun extractCursor(entity: T): TypedCompositeCursor<T>? = null

    override fun buildProjection(
        root: Root<T>,
        query: CriteriaQuery<Tuple>,
        criteriaBuilder: CriteriaBuilder,
    ): CursorProjection<T, TypedCompositeCursor<T>> {
        @Suppress("UNCHECKED_CAST")
        val scoreSelection =
            scoreExpression(root, criteriaBuilder).alias(scoreAlias) as Selection<*>

        return CursorProjection(
            additionalSelections = listOf(scoreSelection),
            entityExtractor = { tuple -> tuple.get(0, domainClass) },
            cursorExtractor = { tuple ->
                val entity = tuple.get(0, domainClass)
                val idValue = idProperty.get(entity)
                val scoreValue =
                    tuple.getOrNull(scoreAlias, Number::class.java)
                        ?: tuple.getOrNull(1, Number::class.java)
                val scoreAsString = scoreValue?.toString()
                TypedCompositeCursor.of(scoreAlias to scoreAsString, idProperty.name to idValue)
            },
        )
    }

    override fun toJpaOrders(root: Root<T>, criteriaBuilder: CriteriaBuilder): List<Order> {
        val scoreExpr = scoreExpression(root, criteriaBuilder)
        val idPath = root.get<ID>(idProperty.name)

        val scoreOrder =
            if (direction.isAscending) criteriaBuilder.asc(scoreExpr)
            else criteriaBuilder.desc(scoreExpr)
        val tieBreakerOrder = criteriaBuilder.asc(idPath)

        return listOf(scoreOrder, tieBreakerOrder)
    }

    internal fun scoreExpression(root: Root<T>, cb: CriteriaBuilder): Expression<Double> =
        cb.function("pdb_score", Double::class.java, root.get<Any>(idProperty.name))
}

fun <T> SearchQueryBuilder<T>.build(block: QueryBuilderScope<T>.() -> SearchQuery): SearchQuery =
    this.block()

fun <X> Tuple.getOrNull(alias: String, type: Class<X>): X? =
    runCatching { get(alias, type) }.getOrNull()

fun <X> Tuple.getOrNull(index: Int, type: Class<X>): X? =
    runCatching { get(index, type) }.getOrNull()
