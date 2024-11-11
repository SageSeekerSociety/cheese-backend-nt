package org.rucca.cheese.space.option

data class SpaceQueryOptions(
    val queryMyRank: Boolean,
    val queryClassificationTopics: Boolean,
) {
    companion object {
        val MINIMUM =
            SpaceQueryOptions(
                queryMyRank = false,
                queryClassificationTopics = false,
            )

        val MAXIMUM =
            SpaceQueryOptions(
                queryMyRank = true,
                queryClassificationTopics = true,
            )
    }
}
