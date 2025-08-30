/*
 *  Description: This file defines the SpaceClassificationTopicsRelation entity and its repository.
 *               It stores the relationship between a space and its classification topics.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.space.models

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.topic.Topic

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "space_id"), Index(columnList = "topic_id")])
class SpaceClassificationTopicsRelation(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var space: Space? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var topic: Topic? = null,
) : BaseEntity()
