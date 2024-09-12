package org.rucca.cheese.task

import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import org.rucca.cheese.attachment.Attachment
import org.rucca.cheese.common.persistent.BaseEntity

@Entity
class TaskSubmission(
        @ManyToOne val membership: TaskMembership,
        val version: Int,
        val index: Int,
        val contentText: String?,
        @ManyToOne val contentAttachment: Attachment?,
) : BaseEntity()
