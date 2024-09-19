package org.rucca.cheese.space

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.Optional
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.JpaRepository

enum class SpaceAdminRole {
    OWNER,
    ADMIN
}

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
        indexes =
                [
                        Index(columnList = "space_id"),
                        Index(columnList = "user_id"),
                        Index(columnList = "space_id, user_id", unique = true),
                ])
class SpaceAdminRelation(
        @ManyToOne(fetch = FetchType.LAZY) val space: Space,
        @ManyToOne(fetch = FetchType.LAZY) val user: User,
        var role: SpaceAdminRole,
) : BaseEntity()

interface SpaceAdminRelationRepository : JpaRepository<SpaceAdminRelation, IdType> {
    fun findAllBySpaceId(spaceId: IdType): List<SpaceAdminRelation>

    fun findBySpaceIdAndRole(spaceId: IdType, role: SpaceAdminRole): Optional<SpaceAdminRelation>

    fun findBySpaceIdAndUserId(spaceId: IdType, userId: IdType): Optional<SpaceAdminRelation>

    fun existsBySpaceIdAndUserId(spaceId: IdType, userId: IdType): Boolean

    fun existsBySpaceIdAndUserIdAndRole(spaceId: IdType, userId: IdType, role: SpaceAdminRole): Boolean
}
