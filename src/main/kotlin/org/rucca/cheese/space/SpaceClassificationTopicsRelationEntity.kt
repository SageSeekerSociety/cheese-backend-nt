/*
 *  Description: This file defines the SpaceClassificationTopicsRelation entity and its repository.
 *               It stores the relationship between a space and its classification topics.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.space

import jakarta.persistence.*
import java.util.Optional
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.topic.Topic
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "space_id"), Index(columnList = "topic_id")])
class SpaceClassificationTopicsRelation(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var space: Space? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var topic: Topic? = null,
) : BaseEntity()

interface SpaceClassificationTopicsRelationRepository :
    JpaRepository<SpaceClassificationTopicsRelation, IdType> {
    fun findAllBySpaceId(spaceId: IdType): List<SpaceClassificationTopicsRelation>

    @Query(
        "SELECT sctr FROM SpaceClassificationTopicsRelation sctr JOIN FETCH sctr.topic WHERE sctr.space.id = :spaceId AND sctr.deletedAt IS NULL"
    )
    fun findAllBySpaceIdFetchTopic(spaceId: IdType): List<SpaceClassificationTopicsRelation>

    fun findBySpaceIdAndTopicId(
        spaceId: IdType,
        topicId: IdType,
    ): Optional<SpaceClassificationTopicsRelation>
}
