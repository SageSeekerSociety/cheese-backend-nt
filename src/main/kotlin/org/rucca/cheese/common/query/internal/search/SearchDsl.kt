package org.rucca.cheese.common.query.internal.search

import kotlin.reflect.KProperty1
import kotlinx.serialization.json.JsonPrimitive

fun <T> paradeSearch(block: SearchQueryBuilder<T>.() -> SearchQuery): SearchQuery {
    return SearchQueryBuilder<T>().block()
}

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

fun <T> SearchQueryBuilder<T>.build(block: QueryBuilderScope<T>.() -> SearchQuery): SearchQuery =
    this.block()
