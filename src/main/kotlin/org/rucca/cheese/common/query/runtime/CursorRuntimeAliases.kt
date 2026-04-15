package org.rucca.cheese.common.query.runtime

import org.rucca.cheese.common.pagination.model.Cursor as LegacyCursor
import org.rucca.cheese.common.pagination.model.CursorPage as LegacyCursorPage
import org.rucca.cheese.common.pagination.repository.CursorPagingRepository as LegacyCursorPagingRepository
import org.rucca.cheese.common.pagination.spec.CursorSpecification as LegacyCursorSpecification

/** Wrapper aliases that decouple the new query runtime from the legacy pagination package. */
typealias Cursor<T> = LegacyCursor<T>

typealias CursorPage<T, C> = LegacyCursorPage<T, C>

typealias CursorPagingRepository<T, ID> = LegacyCursorPagingRepository<T, ID>

typealias CursorSpecification<T, C> = LegacyCursorSpecification<T, C>
