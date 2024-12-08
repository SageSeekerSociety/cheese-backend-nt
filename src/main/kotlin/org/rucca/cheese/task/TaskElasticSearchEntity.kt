/*
 *  Description: This file defines the TaskElasticSearchEntity class and its repository.
 *               It stores the information of a task in the Elasticsearch database.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

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

interface TaskElasticSearchRepository : ElasticsearchRepository<TaskElasticSearch, IdType>
