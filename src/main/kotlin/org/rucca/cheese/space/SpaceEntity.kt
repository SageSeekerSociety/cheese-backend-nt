/*
 *  Description: This file defines the Space entity and its repository.
 *               It stores the information of a space.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      CH3COOH-JYR
 *
 */

package org.rucca.cheese.space

import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.Avatar

@DynamicUpdate
@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "name")])
class Space(
    @Column(nullable = false) var name: String? = null,
    @Column(nullable = false) var intro: String? = null,
    @Column(nullable = false, columnDefinition = "TEXT") var description: String? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var avatar: Avatar? = null,
    @Column(nullable = false) var enableRank: Boolean? = null,
    @Column(nullable = false, columnDefinition = "TEXT") var announcements: String? = null,
    @Column(nullable = false, columnDefinition = "TEXT") var taskTemplates: String? = null,
) : BaseEntity()

interface SpaceRepository : CursorPagingRepository<Space, IdType> {
    fun existsByName(name: String): Boolean
}
