package org.rucca.cheese.task

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.attachment.Attachment
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "task_submission_id")])
class TaskSubmissionEntry(
    @JoinColumn(nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val taskSubmission: TaskSubmission? = null,
    @Column(nullable = false) val index: Int? = null,
    val contentText: String? = null,
    @ManyToOne(fetch = FetchType.LAZY) val contentAttachment: Attachment? = null, // nullable
) : BaseEntity()

interface TaskSubmissionEntryRepository : JpaRepository<TaskSubmissionEntry, IdType> {
    fun findAllByTaskSubmissionId(taskSubmissionId: IdType): List<TaskSubmissionEntry>
}
