package org.rucca.cheese.task

import jakarta.persistence.Id
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

@Document(indexName = "tasks")
class TaskElasticSearch {
    @Id var id: IdType? = null
    var name: String? = null
}

interface TaskElasticSearchRepository : ElasticsearchRepository<TaskElasticSearch, IdType> {
    fun findByNameContaining(name: String): List<TaskElasticSearch>

    fun findByIdOrNameContaining(id: IdType, name: String): List<TaskElasticSearch>
}
