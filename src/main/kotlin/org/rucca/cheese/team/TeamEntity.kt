package org.rucca.cheese.team

import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.Avatar
import org.springframework.data.jpa.repository.JpaRepository

@Entity
class Team(
        val name: String,
        val description: String,
        @ManyToOne val avatar: Avatar,
) : BaseEntity()

interface TeamRepository : JpaRepository<Team, IdType>
