package org.rucca.cheese.project.models

import jakarta.persistence.*
import java.time.OffsetDateTime
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    indexes =
        [
            Index(columnList = "name"),
            Index(columnList = "team_id"),
            Index(columnList = "leader_id"),
            Index(columnList = "parent_id"),
        ]
)
@DynamicInsert
@DynamicUpdate
class Project(
    @Column(nullable = false) var name: String,
    @Column(nullable = false, columnDefinition = "text") var description: String,
    @Column(nullable = false, length = 7) var colorCode: String,
    @Column(nullable = false) var startDate: OffsetDateTime,
    @Column(nullable = false) var endDate: OffsetDateTime,
    @Column(nullable = false) var teamId: IdType,
    @Column(nullable = false) var leaderId: IdType,
    @JoinColumn(nullable = true) @ManyToOne(fetch = FetchType.LAZY) var parent: Project? = null,
    @Column(nullable = true) var externalTaskId: IdType? = null,
    @Column(nullable = true) var githubRepo: String? = null,
    @Column(nullable = false, columnDefinition = "boolean default false")
    var archived: Boolean = false,
    @OneToMany(mappedBy = "project", cascade = [CascadeType.ALL], orphanRemoval = true)
    @BatchSize(size = 5)
    val memberships: MutableSet<ProjectMembership> = mutableSetOf(),
) : BaseEntity() {
    fun addMember(userId: IdType, role: ProjectMemberRole, notes: String? = null) {
        if (role == ProjectMemberRole.LEADER) {
            this.leaderId = userId
        }

        val existingMembership = memberships.find { it.userId == userId }
        if (existingMembership != null) {
            existingMembership.role = role
            existingMembership.notes = notes
            return
        }

        val membership = ProjectMembership(this, userId, role, notes)
        memberships.add(membership)
    }

    fun isAtLeast(userId: IdType, role: ProjectMemberRole, cascade: Boolean = true): Boolean {
        if (this.memberships.any { it.userId == userId && it.role.isAtLeast(role) }) return true
        if (
            cascade &&
                this.parent?.memberships?.any { it.userId == userId && it.role.isAtLeast(role) } ==
                    true
        )
            return true
        return false
    }

    fun isLeaderOrParentLeader(userId: IdType): Boolean {
        return this.leaderId == userId || this.parent?.leaderId == userId
    }
}
