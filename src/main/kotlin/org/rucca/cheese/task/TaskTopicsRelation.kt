/*
 *  Description: This file defines the TaskTopicsRelation entity and its repository.
 *               It stores the relation between a task and a topic.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.task

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.topic.Topic

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "task_id"), Index(columnList = "topic_id")])
class TaskTopicsRelation(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val task: Task? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val topic: Topic? = null,
) : BaseEntity()
