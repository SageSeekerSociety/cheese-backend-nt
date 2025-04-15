/*
 *  Description: This file defines the TaskSubmissionReview entity and its repository.
 *               It stores the information of the review of a submission.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.task

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "submission_id")])
class TaskSubmissionReview(
    @JoinColumn(nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val submission: TaskSubmission? = null,
    @Column(nullable = false) var accepted: Boolean? = null,
    @Column(nullable = false) var score: Int? = null,
    @Column(nullable = false) var comment: String? = null,
) : BaseEntity()
