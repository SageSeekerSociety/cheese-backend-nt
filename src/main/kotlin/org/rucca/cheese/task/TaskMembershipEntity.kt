package org.rucca.cheese.task

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity

@Entity
@SQLDelete(sql = "UPDATE ${'$'}{hbm_dialect.table_name} SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
class TaskMembership(
        @ManyToOne(fetch = FetchType.LAZY) val task: Task,
        val memberId: Int,
) : BaseEntity()
