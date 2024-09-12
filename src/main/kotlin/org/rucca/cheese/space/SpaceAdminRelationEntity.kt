package org.rucca.cheese.space

import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.user.User

enum class SpaceMemberRole {
    ADMIN,
    MEMBER
}

@Entity
class SpaceAdminRelation(
        @ManyToOne val space: Space,
        @ManyToOne val user: User,
        val role: SpaceMemberRole,
) : BaseEntity()
