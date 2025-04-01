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
import org.rucca.cheese.space.models.Space
import org.rucca.cheese.space.models.SpaceCategory
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
    @Column(nullable = false) var name: String,
    @Column(nullable = false) val submitterType: TaskSubmitterType,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    val creator: User,
    @Column(nullable = true) var deadline: LocalDateTime? = null,
    @Column(nullable = true) var participantLimit: Int? = null,
    @Column(nullable = false) var defaultDeadline: Long,
    @Column(nullable = false) var resubmittable: Boolean,
    @Column(nullable = false) var editable: Boolean,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    val space: Space,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    var category: SpaceCategory,
    @Column(nullable = false) var intro: String,
    @Column(nullable = false, columnDefinition = "TEXT") var description: String,
    @ElementCollection var submissionSchema: List<TaskSubmissionSchema>? = null,
    @Column(nullable = true) var rank: Int? = null,
    @Column(nullable = false) var approved: ApproveType = ApproveType.NONE,
    @Column(nullable = true) var rejectReason: String? = null,
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    var requireRealName: Boolean = false,
    /**
     * Minimum number of members required for a team to participate. Only applicable when
     * submitterType is TEAM.
     */
    @Column(nullable = true) var minTeamSize: Int? = null,
    /**
     * Maximum number of members allowed for a team to participate. Only applicable when
     * submitterType is TEAM.
     */
    @Column(nullable = true) var maxTeamSize: Int? = null,
) : BaseEntity() {
    @PrePersist
    @PreUpdate
    fun validateEntityState() {
        checkCategorySpaceConsistency()
        validateTeamSizeConstraints()
    }

    /** Validates team size constraints based on the submitter type. */
    private fun validateTeamSizeConstraints() {
        if (submitterType == TaskSubmitterType.USER) {
            // For USER tasks, team size constraints must be null
            if (minTeamSize != null || maxTeamSize != null) {
                throw IllegalStateException(
                    "minTeamSize and maxTeamSize must be null when submitterType is USER."
                )
            }
        } else if (submitterType == TaskSubmitterType.TEAM) {
            // For TEAM tasks, validate the constraints if they are set
            val minSize = minTeamSize
            val maxSize = maxTeamSize

            if (minSize != null && minSize < 1) { // Assuming team size must be at least 1
                throw IllegalStateException("minTeamSize must be a positive integer.")
            }
            if (maxSize != null && maxSize < 1) {
                throw IllegalStateException("maxTeamSize must be a positive integer.")
            }

            if (minSize != null && maxSize != null && minSize > maxSize) {
                throw IllegalStateException(
                    "minTeamSize ($minSize) cannot be greater than maxTeamSize ($maxSize)."
                )
            }
        }
    }

    private fun checkCategorySpaceConsistency() {
        if (category.space.id != space.id) {
            throw IllegalStateException(
                "Task's category must belong to the same space as the task."
            )
        }
    }
}
