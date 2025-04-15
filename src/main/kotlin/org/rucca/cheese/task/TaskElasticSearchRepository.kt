package org.rucca.cheese.task

import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

interface TaskElasticSearchRepository : ElasticsearchRepository<TaskElasticSearch, IdType>
