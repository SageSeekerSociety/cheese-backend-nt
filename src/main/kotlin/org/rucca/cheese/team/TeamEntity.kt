package org.rucca.cheese.team

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.Avatar
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@SQLRestriction("deleted_at IS NULL")
class Team(
        val name: String,
        val description: String,
        @ManyToOne(fetch = FetchType.LAZY) val avatar: Avatar,
) : BaseEntity()

interface TeamRepository : JpaRepository<Team, IdType>
