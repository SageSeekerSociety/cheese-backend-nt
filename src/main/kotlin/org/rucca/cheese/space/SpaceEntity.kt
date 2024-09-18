package org.rucca.cheese.space

import jakarta.persistence.*
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.Avatar
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@SQLDelete(sql = "UPDATE ${'$'}{hbm_dialect.table_name} SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
class Space(
        val name: String,
        val description: String,
        @ManyToOne(fetch = FetchType.LAZY) val avatar: Avatar,
) : BaseEntity()

interface SpaceRepository : JpaRepository<Space, IdType>
