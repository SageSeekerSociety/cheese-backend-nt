package org.rucca.cheese.task

import jakarta.persistence.*
import java.util.Optional
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    indexes =
        [
            Index(columnList = "task_id"),
            Index(columnList = "member_id"),
        ]
)
class TaskMembership(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val task: Task? = null,
    @Column(nullable = false) val memberId: IdType? = null,
) : BaseEntity()

interface taskMembershipRepository : JpaRepository<TaskMembership, IdType> {
    fun findAllByTaskId(taskId: IdType): List<TaskMembership>

    fun findByTaskIdAndMemberId(taskId: IdType, memberId: IdType): Optional<TaskMembership>

    fun existsByTaskIdAndMemberId(taskId: IdType, memberId: IdType): Boolean

    fun existsByTaskId(taskId: IdType): Boolean

    @Query(
        "SELECT tm FROM TaskMembership tm WHERE tm.task.id = :taskId AND EXISTS (SELECT 1 FROM TaskSubmission ts WHERE ts.membership.id = tm.id)"
    )
    fun findByTaskIdWhereMemberHasSubmitted(taskId: IdType): List<TaskMembership>
}
