package org.rucca.cheese.task

import jakarta.persistence.Entity
import java.sql.Date
import org.rucca.cheese.common.BaseEntity
import org.rucca.cheese.common.IdType
import org.rucca.cheese.space.Space
import org.rucca.cheese.team.Team
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.JpaRepository

@Entity
class Task(
        val name: String,
        val submitterType: IdType,
        val creator: User,
        val deadline: Date,
        val resubmittable: Boolean,
        val editable: Boolean,
        val team: Team?,
        val space: Space?,
        val description: String,
        val submissionSchema: List<Pair<String, Int>>,
) : BaseEntity()

interface TaskRepository : JpaRepository<Task, IdType>
