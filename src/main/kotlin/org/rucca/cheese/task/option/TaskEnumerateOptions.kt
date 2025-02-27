/*
 *  Description: This file defines the TaskEnumerateOptions data class.
 *               It is used to specify options when enumerating tasks.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.task.option

import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType

data class TaskEnumerateOptions(
    val space: IdType?,
    val team: IdType?,
    val approved: ApproveType?,
    val owner: IdType?,
    val joined: Boolean?,
    val topics: List<IdType>?,
)
