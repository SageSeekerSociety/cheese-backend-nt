package org.rucca.cheese.task

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.attachment.Attachment
import org.rucca.cheese.common.persistent.BaseEntity

@Entity
@SQLRestriction("deleted_at IS NULL")
class TaskSubmission(
        @ManyToOne(fetch = FetchType.LAZY) val membership: TaskMembership,
        val version: Int,
        val index: Int,
        val contentText: String?,
        @ManyToOne(fetch = FetchType.LAZY) val contentAttachment: Attachment?,
) : BaseEntity()
