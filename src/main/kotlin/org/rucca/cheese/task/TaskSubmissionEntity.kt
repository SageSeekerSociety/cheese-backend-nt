package org.rucca.cheese.task

import jakarta.persistence.*
import java.util.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.attachment.Attachment
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
        indexes =
                [
                        Index(columnList = "membership_id"),
                ])
data class TaskSubmission(
        @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.EAGER) val membership: TaskMembership? = null,
        @Column(nullable = false) val version: Int? = null,
        @Column(nullable = false) val index: Int? = null,
        @JoinColumn(name = "submitter_id", nullable = false)
        @ManyToOne(fetch = FetchType.EAGER)
        val submitter: User? = null,
        val contentText: String? = null,
        @ManyToOne(fetch = FetchType.EAGER) val contentAttachment: Attachment? = null, // nullable
) : BaseEntity()

interface TaskSubmissionRepository : JpaRepository<TaskSubmission, IdType> {
    fun findAllByMembershipId(membershipId: IdType): List<TaskSubmission>

    fun findAllByMembershipIdAndVersion(membershipId: IdType, version: Int): List<TaskSubmission>

    @Query("SELECT MAX(ts.version) FROM TaskSubmission ts WHERE ts.membership.id = :membershipId")
    fun findVersionNumberByMembershipId(membershipId: IdType): Optional<Int>
}
