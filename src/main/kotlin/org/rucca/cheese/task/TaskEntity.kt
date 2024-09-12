package org.rucca.cheese.task

import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import java.sql.Date
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.space.Space
import org.rucca.cheese.team.Team
import org.rucca.cheese.user.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

@Entity
class Task(
        val name: String,
        val submitterType: IdType,
        @ManyToOne val creator: UserEntity,
        val deadline: Date,
        val resubmittable: Boolean,
        val editable: Boolean,
        @ManyToOne val team: Team?,
        @ManyToOne val space: Space?,
        val description: String,
        @ElementCollection val submissionSchema: List<SubmissionSchema>,
) : BaseEntity()

@Embeddable class SubmissionSchema(val key: String, val value: Int)

interface TaskRepository : JpaRepository<Task, IdType>