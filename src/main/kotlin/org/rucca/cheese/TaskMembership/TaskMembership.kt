package org.rucca.cheese.TaskMembership

import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.task.Task

@Entity
class TaskMembership(
        @ManyToOne val task: Task,
        val memberId: Integer,
) : BaseEntity()
