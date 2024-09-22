package org.rucca.cheese.task

import jakarta.persistence.*
import java.util.Optional
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.attachment.Attachment
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
        indexes =
                [
                        Index(columnList = "membership_id"),
                ])
class TaskSubmission(
        @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val membership: TaskMembership? = null,
        @Column(nullable = false) val version: Int? = null,
        @Column(nullable = false) val index: Int? = null,
        val contentText: String? = null, // nullable
        @ManyToOne(fetch = FetchType.LAZY) val contentAttachment: Attachment? = null, // nullable
) : BaseEntity()

interface taskSubmissionRepository : JpaRepository<TaskSubmission, IdType> {
    fun existsByMembershipId(membershipId: IdType): Boolean

    @Query("SELECT MAX(ts.version) FROM TaskSubmission ts WHERE ts.membership.id = :membershipId")
    fun findVersionNumberByMembershipId(membershipId: IdType): Optional<Int>
}
