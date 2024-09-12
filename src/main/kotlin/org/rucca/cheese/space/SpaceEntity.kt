package org.rucca.cheese.space

import jakarta.persistence.*
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository

@Entity
class Space(
        val name: String,
        val description: String,
        val avatar_id: Integer,
) : BaseEntity()

interface SpaceRepository : JpaRepository<Space, IdType>
