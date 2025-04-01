/*
 *  Description: This file defines the SpaceUserRank entity and its repository.
 *               It stores the rank of a user in a space.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.space.models

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.user.User

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "space_id"), Index(columnList = "user_id")])
class SpaceUserRank(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val space: Space? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val user: User? = null,
    @Column(nullable = false) var rank: Int? = null,
) : BaseEntity()
