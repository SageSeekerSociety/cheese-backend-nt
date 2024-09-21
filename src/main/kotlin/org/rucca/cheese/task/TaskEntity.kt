package org.rucca.cheese.task

import jakarta.persistence.*
import java.time.LocalDate
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

enum class TaskSubmissionEntryType {
    TEXT,
    ATTACHMENT,
}

@Embeddable
class TaskSubmissionSchema(
        @Column(nullable = false) val index: Int? = null,
        @Column(nullable = false) val description: String? = null,
        @Column(nullable = false) val type: TaskSubmissionEntryType? = null,
)

@Entity
@SQLRestriction("deleted_at IS NULL")
class Task(
        @Column(nullable = false) val name: String? = null,
        @Column(nullable = false) val submitterType: TaskSubmitterType? = null,
        @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val creator: User? = null,
        @Column(nullable = false) val deadline: LocalDate? = null,
        @Column(nullable = false) val resubmittable: Boolean? = null,
        @Column(nullable = false) val editable: Boolean? = null,
        @ManyToOne(fetch = FetchType.LAZY) val team: Team? = null, // nullable
        @ManyToOne(fetch = FetchType.LAZY) val space: Space? = null, // nullable
        @Column(nullable = false) val description: String? = null,
        @ElementCollection val submissionSchema: List<TaskSubmissionSchema>? = null,
) : BaseEntity()

interface TaskRepository : JpaRepository<Task, IdType>
