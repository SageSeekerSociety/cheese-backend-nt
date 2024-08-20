package cn.edu.ruc.cheese.backend.space

import cn.edu.ruc.cheese.backend.common.BaseEntity
import cn.edu.ruc.cheese.backend.common.IdType
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository

@Entity
class Space(
        val name: String,
        val description: String,
        val createdBy: Long,
) : BaseEntity()

interface SpaceRepository : JpaRepository<Space, IdType>
