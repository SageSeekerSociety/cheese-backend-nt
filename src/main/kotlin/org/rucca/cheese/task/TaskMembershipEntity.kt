package org.rucca.cheese.task

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
        indexes =
                [
                        Index(columnList = "task_id"),
                        Index(columnList = "member_id"),
                ])
class TaskMembership(
        @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val task: Task? = null,
        @Column(nullable = false) val memberId: Int? = null,
) : BaseEntity()

interface taskMembershipRepository : JpaRepository<TaskMembership, IdType> {
    fun findAllByTaskId(taskId: IdType): List<TaskMembership>

    fun existsByTaskIdAndMemberId(taskId: IdType, memberId: IdType): Boolean
}
