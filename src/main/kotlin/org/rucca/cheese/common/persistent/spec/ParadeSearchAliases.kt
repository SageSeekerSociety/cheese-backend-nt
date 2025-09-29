package org.rucca.cheese.common.persistent.spec

import org.rucca.cheese.common.query.internal.search.BooleanQuery as InternalBooleanQuery
import org.rucca.cheese.common.query.internal.search.BooleanQueryBuilder as InternalBooleanQueryBuilder
import org.rucca.cheese.common.query.internal.search.BoostQuery as InternalBoostQuery
import org.rucca.cheese.common.query.internal.search.MatchQuery as InternalMatchQuery
import org.rucca.cheese.common.query.internal.search.MatchQueryBuilder as InternalMatchQueryBuilder
import org.rucca.cheese.common.query.internal.search.PhraseQuery as InternalPhraseQuery
import org.rucca.cheese.common.query.internal.search.PhraseQueryBuilder as InternalPhraseQueryBuilder
import org.rucca.cheese.common.query.internal.search.QueryBuilderScope as InternalQueryBuilderScope
import org.rucca.cheese.common.query.internal.search.RangeBuilder as InternalRangeBuilder
import org.rucca.cheese.common.query.internal.search.RangeQuery as InternalRangeQuery
import org.rucca.cheese.common.query.internal.search.SearchQuery as InternalSearchQuery
import org.rucca.cheese.common.query.internal.search.SearchQueryBuilder as InternalSearchQueryBuilder
import org.rucca.cheese.common.query.internal.search.TermQuery as InternalTermQuery
import org.rucca.cheese.common.query.internal.search.build as internalBuild

// Kotlin does not allow direct typealias to functions, so we delegate manually where needed.

typealias SearchQuery = InternalSearchQuery

typealias BooleanQuery = InternalBooleanQuery

typealias MatchQuery = InternalMatchQuery

typealias TermQuery = InternalTermQuery

typealias PhraseQuery = InternalPhraseQuery

typealias RangeQuery = InternalRangeQuery

typealias BoostQuery = InternalBoostQuery

typealias QueryBuilderScope<T> = InternalQueryBuilderScope<T>

typealias SearchQueryBuilder<T> = InternalSearchQueryBuilder<T>

typealias BooleanQueryBuilder<T> = InternalBooleanQueryBuilder<T>

typealias MatchQueryBuilder = InternalMatchQueryBuilder

typealias PhraseQueryBuilder = InternalPhraseQueryBuilder

typealias RangeBuilder = InternalRangeBuilder

@Suppress("NOTHING_TO_INLINE")
inline fun <T> paradeSearch(noinline block: SearchQueryBuilder<T>.() -> SearchQuery): SearchQuery =
    org.rucca.cheese.common.query.internal.search.paradeSearch(block)

@Suppress("NOTHING_TO_INLINE")
inline fun <T> SearchQueryBuilder<T>.build(
    noinline block: QueryBuilderScope<T>.() -> SearchQuery
): SearchQuery = this.internalBuild(block)
