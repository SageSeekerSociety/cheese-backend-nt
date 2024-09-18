package org.rucca.cheese.task

import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import java.sql.Date
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.space.Space
import org.rucca.cheese.team.Team
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.JpaRepository

enum class TaskSubmitterType {
    USER,
    TEAM,
}

@Embeddable class TaskSubmissionSchema(val key: String, val value: Int)

@Entity
@SQLRestriction("deleted_at IS NULL")
class Task(
        val name: String,
        val submitterType: TaskSubmitterType,
        @ManyToOne(fetch = FetchType.LAZY) val creator: User,
        val deadline: Date,
        val resubmittable: Boolean,
        val editable: Boolean,
        @ManyToOne(fetch = FetchType.LAZY) val team: Team?,
        @ManyToOne(fetch = FetchType.LAZY) val space: Space?,
        val description: String,
        @ElementCollection val submissionSchema: List<TaskSubmissionSchema>,
) : BaseEntity()

interface TaskRepository : JpaRepository<Task, IdType>
