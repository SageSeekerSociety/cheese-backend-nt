package org.rucca.cheese.team

import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.user.User

enum class TeamMemberRole {
    OWNER,
    ADMIN,
    MEMBER,
}

@Entity
class TeamUserRelation(
        @ManyToOne val user: User,
        @ManyToOne val team: Team,
        val role: TeamMemberRole,
) : BaseEntity()
