/*
 *  Description: This file defines the Task entity and its repository.
 *               It stores the information of a task.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      HuanCheng65
 *      nameisyui
 *      CH3COOH-JYR
 *
 */

package org.rucca.cheese.task

import jakarta.persistence.*
import java.time.LocalDateTime
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.space.Space
import org.rucca.cheese.team.Team
import org.rucca.cheese.user.User

enum class TaskSubmitterType {
    USER,
    TEAM,
}

enum class TaskSubmissionEntryType {
    TEXT,
    ATTACHMENT,
}

@Embeddable
class TaskSubmissionSchema(
    @Column(nullable = false) val index: Int? = null,
    @Column(nullable = false) val description: String? = null,
    @Column(nullable = false) val type: TaskSubmissionEntryType? = null,
)

@Entity
@SQLRestriction("deleted_at IS NULL")
@EntityListeners(TaskElasticSearchSyncListener::class)
class Task(
    @Column(nullable = false) var name: String? = null,
    @Column(nullable = false) val submitterType: TaskSubmitterType? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val creator: User? = null,
    @Column(nullable = true) var deadline: LocalDateTime? = null,
    @Column(nullable = true) var participantLimit: Int? = null,
    @Column(nullable = false) var defaultDeadline: Long? = null,
    @Column(nullable = false) var resubmittable: Boolean? = null,
    @Column(nullable = false) var editable: Boolean? = null,
    @ManyToOne(fetch = FetchType.LAZY) val team: Team? = null, // nullable
    @ManyToOne(fetch = FetchType.LAZY) val space: Space? = null, // nullable
    @Column(nullable = false) var intro: String? = null,
    @Column(nullable = false, columnDefinition = "TEXT") var description: String? = null,
    @ElementCollection var submissionSchema: List<TaskSubmissionSchema>? = null,
    @Column(nullable = true) var rank: Int? = null,
    @Column(nullable = false) var approved: ApproveType? = null,
    @Column(nullable = false) var rejectReason: String? = null,
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    var requireRealName: Boolean = false,
) : BaseEntity()
