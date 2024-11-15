package org.rucca.cheese.space

import jakarta.persistence.*
import java.util.Optional
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.topic.Topic
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    indexes =
        [
            Index(columnList = "space_id"),
            Index(columnList = "topic_id"),
        ]
)
class SpaceClassificationTopicsRelation(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var space: Space? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var topic: Topic? = null,
) : BaseEntity()

interface SpaceClassificationTopicsRelationRepository :
    JpaRepository<SpaceClassificationTopicsRelation, IdType> {
    fun findAllBySpaceId(spaceId: IdType): List<SpaceClassificationTopicsRelation>

    fun findBySpaceIdAndTopicId(
        spaceId: IdType,
        topicId: IdType
    ): Optional<SpaceClassificationTopicsRelation>
}