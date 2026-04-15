package org.rucca.cheese.common.query.dsl

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import org.rucca.cheese.common.query.internal.search.QueryBuilderScope
import org.rucca.cheese.common.query.internal.search.SearchQuery
import org.rucca.cheese.common.query.internal.search.SearchQueryBuilder
import org.rucca.cheese.common.query.internal.search.build
import org.rucca.cheese.common.query.internal.spec.ExpressionFilteringScope
import org.rucca.cheese.common.query.internal.spec.FullFilteringScope
import org.rucca.cheese.common.query.internal.spec.SpecContext
import org.rucca.cheese.common.query.model.CursorMode
import org.rucca.cheese.common.query.model.ExpressionArgument
import org.rucca.cheese.common.query.model.ExpressionDescriptor
import org.rucca.cheese.common.query.model.ExpressionSort
import org.rucca.cheese.common.query.model.LiteralExpressionArgument
import org.rucca.cheese.common.query.model.NestedExpressionArgument
import org.rucca.cheese.common.query.model.PaginationConfig
import org.rucca.cheese.common.query.model.PropertyExpressionArgument
import org.rucca.cheese.common.query.model.PropertySort
import org.rucca.cheese.common.query.model.QueryObject
import org.rucca.cheese.common.query.model.RelevanceSort
import org.rucca.cheese.common.query.model.SearchClause
import org.rucca.cheese.common.query.model.SortDescriptor
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification

/** Entry point for the QueryObject DSL. */
inline fun <reified T : Any> queryFor(
    noinline block: QueryObjectBuilder<T>.() -> Unit
): QueryObject<T> {
    val builder = QueryObjectBuilder(T::class)
    builder.block()
    return builder.build()
}

class QueryObjectBuilder<T : Any>(private val entityClass: KClass<T>) {
    private var idProperty: KProperty1<T, Comparable<*>?>? = null
    private val filterBlocks = mutableListOf<SpecContext<T>.() -> Unit>()
    private var searchClause: SearchClause<T>? = null
    private val groupByProperties = mutableListOf<KProperty1<T, *>>()
    private val havingBlocks = mutableListOf<SpecContext<T>.() -> Unit>()
    private val sortDescriptors = mutableListOf<SortDescriptor<T>>()
    private var paginationConfig: PaginationConfig = PaginationConfig()

    fun id(property: KProperty1<T, Comparable<*>?>) {
        check(idProperty == null) { "QueryObject id() may only be configured once" }
        idProperty = property
    }

    fun configure(block: SpecContext<T>.() -> Unit) {
        filterBlocks += block
    }

    fun filters(block: FullFilteringScope<T>.() -> Unit) {
        filterBlocks += { where(block) }
    }

    fun search(block: QueryBuilderScope<T>.() -> SearchQuery) {
        val searchQuery = SearchQueryBuilder<T>().build(block)
        searchClause = SearchClause(searchQuery)
    }

    fun groupBy(block: GroupByContext<T>.() -> Unit) {
        val ctx = GroupByContext<T>()
        ctx.block()
        groupByProperties += ctx.properties
    }

    fun having(block: ExpressionFilteringScope<T>.() -> Unit) {
        havingBlocks += { having(block) }
    }

    fun sort(block: SortContext.() -> Unit) {
        val ctx = SortContext()
        ctx.block()
        sortDescriptors += ctx.descriptors
    }

    fun paginate(block: PaginationContext.() -> Unit) {
        val ctx = PaginationContext(paginationConfig)
        ctx.block()
        paginationConfig = ctx.toConfig()
    }

    fun build(): QueryObject<T> {
        val id = requireNotNull(idProperty) { "id() must be declared for QueryObject" }
        val filterSpec = buildFilterSpecification(id)
        val havingSpec = buildHavingSpecification(id)
        return QueryObject(
            entityClass = entityClass,
            idProperty = id,
            filter = filterSpec,
            search = searchClause,
            groups = groupByProperties.toList(),
            having = havingSpec,
            sorts = sortDescriptors.toList(),
            pagination = paginationConfig,
        )
    }

    private fun buildFilterSpecification(
        idProperty: KProperty1<T, Comparable<*>?>
    ): Specification<T> = createSpecification(idProperty, filterBlocks) ?: emptySpecification()

    private fun buildHavingSpecification(
        idProperty: KProperty1<T, Comparable<*>?>
    ): Specification<T>? = createSpecification(idProperty, havingBlocks)

    private fun createSpecification(
        idProperty: KProperty1<T, Comparable<*>?>,
        blocks: List<SpecContext<T>.() -> Unit>,
    ): Specification<T>? {
        if (blocks.isEmpty()) return null
        var combined: Specification<T>? = null
        for (block in blocks) {
            val next =
                Specification<T> { root, query, cb ->
                    val criteriaQuery = query ?: return@Specification null
                    val context = SpecContext(cb, root, criteriaQuery)
                    context.useId(idProperty)
                    context.block()
                    context.buildPredicate()
                }
            combined = combined?.and(next) ?: next
        }
        return combined
    }

    private fun emptySpecification(): Specification<T> = Specification { _, _, _ -> null }

    class GroupByContext<T : Any> internal constructor() {
        internal val properties = mutableListOf<KProperty1<T, *>>()

        fun by(property: KProperty1<T, *>) {
            properties += property
        }
    }

    inner class SortContext internal constructor() {
        internal val descriptors = mutableListOf<SortDescriptor<T>>()

        fun by(
            property: KProperty1<T, *>,
            direction: Sort.Direction = Sort.Direction.ASC,
            nullHandling: Sort.NullHandling? = null,
        ) {
            descriptors += PropertySort(property, direction, nullHandling)
        }

        fun by(
            expression: ExpressionDescriptor<T>,
            direction: Sort.Direction = Sort.Direction.ASC,
        ) {
            descriptors += ExpressionSort(expression, direction)
        }

        fun relevance(direction: Sort.Direction = Sort.Direction.DESC, scoreAlias: String? = null) {
            descriptors += RelevanceSort(direction, scoreAlias)
        }

        fun fn(name: String, vararg arguments: ExpressionArgument<T>): ExpressionDescriptor<T> =
            ExpressionDescriptor(function = name, arguments = arguments.toList())

        fun col(property: KProperty1<T, *>): ExpressionArgument<T> =
            PropertyExpressionArgument(property)

        fun literal(value: Any?): ExpressionArgument<T> = LiteralExpressionArgument(value)

        fun expr(descriptor: ExpressionDescriptor<T>): ExpressionArgument<T> =
            NestedExpressionArgument(descriptor)
    }

    class PaginationContext internal constructor(config: PaginationConfig) {
        var pageSize: Int = config.pageSize
        var maxPageSize: Int = config.maxPageSize
        var includeTotalCount: Boolean = config.includeTotalCount
        var cursorMode: CursorMode = config.cursorMode

        internal fun toConfig(): PaginationConfig =
            PaginationConfig(pageSize, maxPageSize, includeTotalCount, cursorMode)
    }
}
