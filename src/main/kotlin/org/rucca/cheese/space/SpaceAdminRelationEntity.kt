package org.rucca.cheese.space

import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.Optional
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.JpaRepository

enum class SpaceAdminRole {
    OWNER,
    ADMIN
}

@Entity
@Table(
        indexes =
                [
                        Index(columnList = "user_id, role", unique = true),
                        Index(columnList = "space_id"),
                ])
class SpaceAdminRelation(
        @ManyToOne val space: Space,
        @ManyToOne val user: User,
        val role: SpaceAdminRole,
) : BaseEntity()

interface SpaceAdminRelationRepository : JpaRepository<SpaceAdminRelation, IdType> {
    fun findBySpaceIdAndRole(userId: IdType, role: SpaceAdminRole): Optional<SpaceAdminRelation>

    fun existsBySpaceIdAndUserId(spaceId: IdType, userId: IdType): Boolean
}
