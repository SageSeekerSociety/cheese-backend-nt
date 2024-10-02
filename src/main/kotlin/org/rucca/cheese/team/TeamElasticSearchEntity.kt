package org.rucca.cheese.team

import jakarta.persistence.Id
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

@Document(indexName = "teams")
class TeamElasticSearch {
    @Id var id: IdType? = null
    var name: String? = null
}

interface TeamElasticSearchRepository : ElasticsearchRepository<TeamElasticSearch, IdType>
