package org.rucca.cheese.task

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity

@Entity
@SQLRestriction("deleted_at IS NULL")
class TaskMembership(
        @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val task: Task? = null,
        @Column(nullable = false) val memberId: Int? = null,
) : BaseEntity()
