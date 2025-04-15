/*
 *  Description: This file defines the SpaceQueryOptions data class.
 *               It is used to specify options when querying spaces or things containing spaces.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.space.option
data class SpaceQueryOptions(
    val queryMyRank: Boolean = false,
    val queryCategories: Boolean = false,
) {
    companion object {
        val MINIMUM = SpaceQueryOptions()

        val MAXIMUM = SpaceQueryOptions(queryMyRank = true, queryCategories = true)
    }
}
