package org.rucca.cheese.task

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class TaskSubmissionSchema(
    @Column(nullable = false) val index: Int? = null,
    @Column(nullable = false) val description: String? = null,
    @Column(nullable = false) val type: TaskSubmissionEntryType? = null,
)

enum class TaskSubmissionEntryType {
    TEXT,
    ATTACHMENT,
}
