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
    val queryJoinedApproved: Boolean,
    val queryJoinedDisapproved: Boolean,
    val queryJoinedNotApprovedOrDisapproved: Boolean,
    val queryTopics: Boolean,
) {
    companion object {
        val MINIMUM =
            TaskQueryOptions(
                querySpace = false,
                queryTeam = false,
                queryJoinability = false,
                querySubmittability = false,
                queryJoined = false,
                queryJoinedApproved = false,
                queryJoinedDisapproved = false,
                queryJoinedNotApprovedOrDisapproved = false,
                queryTopics = false,
            )

        val MAXIMUM =
            TaskQueryOptions(
                querySpace = true,
                queryTeam = true,
                queryJoinability = true,
                querySubmittability = true,
                queryJoined = true,
                queryJoinedApproved = true,
                queryJoinedDisapproved = true,
                queryJoinedNotApprovedOrDisapproved = true,
                queryTopics = true,
            )
    }
}
