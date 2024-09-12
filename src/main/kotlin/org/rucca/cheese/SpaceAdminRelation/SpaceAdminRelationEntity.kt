package org.rucca.cheese.SpaceAdminRelation

import jakarta.persistence.Entity
import org.rucca.cheese.common.BaseEntity
import org.rucca.cheese.space.Space
import org.rucca.cheese.user.User

enum class MemberRole {
    ADMIN,
    MEMBER
}

@Entity
class SpaceAdminRelationEntity(
        val space: Space,
        val user: User,
        val memberRole: MemberRole,
) : BaseEntity()
