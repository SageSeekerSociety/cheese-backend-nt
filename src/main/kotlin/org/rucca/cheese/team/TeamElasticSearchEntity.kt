/*
 *  Description: This file defines the TeamElasticSearch entity and its repository.
 *               It stores the information of a team in the Elasticsearch database.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

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
