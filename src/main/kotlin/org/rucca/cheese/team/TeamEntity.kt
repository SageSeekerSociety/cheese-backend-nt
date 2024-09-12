package org.rucca.cheese.team

import jakarta.persistence.Entity
import org.rucca.cheese.common.BaseEntity
import org.rucca.cheese.common.IdType
import org.springframework.data.jpa.repository.JpaRepository

@Entity
class Team(
        val name: String,
        val description: String,
        val avatar_id: Integer,
) : BaseEntity()

interface TeamRepository : JpaRepository<Team, IdType>
