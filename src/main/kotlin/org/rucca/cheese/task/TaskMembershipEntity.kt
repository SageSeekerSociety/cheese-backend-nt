package org.rucca.cheese.task

import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import org.rucca.cheese.common.persistent.BaseEntity

@Entity
class TaskMembership(
        @ManyToOne val task: Task,
        val memberId: Int,
) : BaseEntity()
