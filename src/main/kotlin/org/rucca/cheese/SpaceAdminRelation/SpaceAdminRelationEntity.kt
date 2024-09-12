package org.rucca.cheese.SpaceAdminRelation

import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.space.Space
import org.rucca.cheese.user.UserEntity

enum class MemberRole {
    ADMIN,
    MEMBER
}

@Entity
class SpaceAdminRelationEntity(
        @ManyToOne val space: Space,
        @ManyToOne val user: UserEntity,
        val memberRole: MemberRole,
) : BaseEntity()
