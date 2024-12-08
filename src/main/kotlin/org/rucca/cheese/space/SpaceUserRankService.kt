/*
 *  Description: This file implements the SpaceService class.
 *               It is responsible for managing a user's rank in a space.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.space

import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.space.error.RankNotEnabledForSpaceError
import org.rucca.cheese.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SpaceUserRankService(
    private val spaceRepository: SpaceRepository,
    private val spaceUserRankRepository: SpaceUserRankRepository,
) {
    private fun getSpace(spaceId: IdType): Space {
        return spaceRepository.findById(spaceId).orElseThrow { NotFoundError("space", spaceId) }
    }

    private fun ensureRankEnabled(spaceId: IdType) {
        if (!getSpace(spaceId).enableRank!!) {
            throw RankNotEnabledForSpaceError(spaceId)
        }
    }

    fun getRank(spaceId: IdType, userId: IdType): Int {
        ensureRankEnabled(spaceId)
        return spaceUserRankRepository
            .findBySpaceIdAndUserId(spaceId, userId)
            .map { it.rank!! }
            .orElse(0)
    }

    fun upgradeRank(spaceId: IdType, userId: IdType, rank: Int): Boolean {
        ensureRankEnabled(spaceId)
        val spaceUserRankOpt = spaceUserRankRepository.findBySpaceIdAndUserId(spaceId, userId)
        if (spaceUserRankOpt.isPresent) {
            val spaceUserRank = spaceUserRankOpt.get()
            if (rank > spaceUserRank.rank!!) {
                spaceUserRank.rank = Math.max(spaceUserRank.rank!!, rank)
                spaceUserRankRepository.save(spaceUserRank)
                return true
            } else return false
        } else {
            val spaceUserRank =
                SpaceUserRank(
                    space = Space().apply { id = spaceId },
                    user = User().apply { id = userId.toInt() },
                    rank = Math.max(0, rank)
                )
            spaceUserRankRepository.save(spaceUserRank)
            return rank > 0
        }
    }
}
