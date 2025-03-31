/*
 *  Description: This file defines the SpaceAdminRelation entity and its repository.
 *               It stores the relationship between a space and its admin.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      CH3COOH-JYR
 *
 */

package org.rucca.cheese.space.models

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.user.User

enum class SpaceAdminRole {
    OWNER,
    ADMIN,
}

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "space_id"), Index(columnList = "user_id")])
class SpaceAdminRelation(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val space: Space? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val user: User? = null,
    @Column(nullable = false) var role: SpaceAdminRole? = null,
) : BaseEntity()
