package org.rucca.cheese.space.option

data class SpaceQueryOptions(val queryMyRank: Boolean) {
    companion object {
        val MINIMUM = SpaceQueryOptions(queryMyRank = false)

        val MAXIMUM = SpaceQueryOptions(queryMyRank = true)
    }
}
