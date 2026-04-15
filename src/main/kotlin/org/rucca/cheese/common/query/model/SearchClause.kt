package org.rucca.cheese.common.query.model

import org.rucca.cheese.common.query.internal.search.SearchQuery

/** Wrapper around Parade search definitions that keeps the model layer immutable. */
data class SearchClause<T : Any>(val searchQuery: SearchQuery)
