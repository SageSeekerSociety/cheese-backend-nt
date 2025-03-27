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
    val querySpace: Boolean,
    val queryTeam: Boolean,
    val queryJoinability: Boolean,
    val querySubmittability: Boolean,
    val queryJoined: Boolean,
    val queryTopics: Boolean,
    val queryUserDeadline: Boolean,
) {
    companion object {
        val MINIMUM =
            TaskQueryOptions(
                querySpace = false,
                queryTeam = false,
                queryJoinability = false,
                querySubmittability = false,
                queryJoined = false,
                queryTopics = false,
                queryUserDeadline = false,
            )

        val MAXIMUM =
            TaskQueryOptions(
                querySpace = true,
                queryTeam = true,
                queryJoinability = true,
                querySubmittability = true,
                queryJoined = true,
                queryTopics = true,
                queryUserDeadline = true,
            )
    }
}
