/*
 *  Description: This file defines the TaskSubmission entity and its repository.
 *               It stores the information of a task submission.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      HuanCheng65
 *      CH3COOH-JYR
 *
 */

package org.rucca.cheese.task

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.user.User

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "membership_id")])
class TaskSubmission(
    @JoinColumn(nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val membership: TaskMembership? = null,
    @Column(nullable = false) val version: Int? = null,
    @JoinColumn(name = "submitter_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val submitter: User? = null,
) : BaseEntity()
