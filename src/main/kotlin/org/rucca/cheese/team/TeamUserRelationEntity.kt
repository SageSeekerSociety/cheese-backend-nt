package org.rucca.cheese.team

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.user.User

enum class TeamMemberRole {
    OWNER,
    ADMIN,
    MEMBER,
}

@Entity
@SQLRestriction("deleted_at IS NULL")
class TeamUserRelation(
        @ManyToOne(fetch = FetchType.LAZY) val user: User,
        @ManyToOne(fetch = FetchType.LAZY) val team: Team,
        val role: TeamMemberRole,
) : BaseEntity()
