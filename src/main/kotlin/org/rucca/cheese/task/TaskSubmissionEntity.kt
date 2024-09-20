package org.rucca.cheese.task

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.attachment.Attachment
import org.rucca.cheese.common.persistent.BaseEntity

@Entity
@SQLRestriction("deleted_at IS NULL")
class TaskSubmission(
        @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val membership: TaskMembership? = null,
        @Column(nullable = false) val version: Int? = null,
        @Column(nullable = false) val index: Int? = null,
        val contentText: String? = null, // nullable
        @ManyToOne(fetch = FetchType.LAZY) val contentAttachment: Attachment? = null, // nullable
) : BaseEntity()
