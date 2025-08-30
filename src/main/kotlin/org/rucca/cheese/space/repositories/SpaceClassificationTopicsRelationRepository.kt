package org.rucca.cheese.space.repositories

import java.util.*
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.space.models.SpaceClassificationTopicsRelation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

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
