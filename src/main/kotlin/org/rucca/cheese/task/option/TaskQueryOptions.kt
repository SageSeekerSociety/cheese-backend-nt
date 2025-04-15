/*
 *  Description: This file defines the TaskQueryOptions data class.
 *               It is used to specify options when querying tasks or things containing tasks.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.task.option

data class TaskQueryOptions(
    val querySpace: Boolean = false,
    val queryJoinability: Boolean = false,
    val querySubmittability: Boolean = false,
    val queryJoined: Boolean = false,
    val queryTopics: Boolean = false,
    val queryUserDeadline: Boolean = false,
) {
    companion object {
        val MINIMUM =
            TaskQueryOptions(
                querySpace = false,
                queryJoinability = false,
                querySubmittability = false,
                queryJoined = false,
                queryTopics = false,
                queryUserDeadline = false,
            )

        val MAXIMUM =
            TaskQueryOptions(
                querySpace = true,
                queryJoinability = true,
                querySubmittability = true,
                queryJoined = true,
                queryTopics = true,
                queryUserDeadline = true,
            )
    }
}
