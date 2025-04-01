/*
 *  Description: This file defines the Space entity and its repository.
 *               It stores the information of a space.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      CH3COOH-JYR
 *
 */

package org.rucca.cheese.space.models

import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
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
    @OneToMany(
        mappedBy = "space", // References the 'space' field in SpaceCategory
        cascade = [CascadeType.ALL], // When Space is deleted, its Categories are also deleted
        orphanRemoval =
            true, // When a Category is removed from the categories collection, it's also deleted
        // from the database
        fetch = FetchType.LAZY, // Default lazy loading to avoid unnecessary queries
    )
    @OrderBy("displayOrder ASC, name ASC") // Specifies default sorting when loading categories
    var categories: MutableList<SpaceCategory> = mutableListOf(),
    @ManyToOne(
        fetch = FetchType.LAZY
    ) // Eager fetch might be okay if always needed with Space, but LAZY is safer
    @JoinColumn(
        name = "default_category_id",
        nullable = true,
    ) // Nullable initially during creation process
    var defaultCategory: SpaceCategory? = null,
) : BaseEntity()
