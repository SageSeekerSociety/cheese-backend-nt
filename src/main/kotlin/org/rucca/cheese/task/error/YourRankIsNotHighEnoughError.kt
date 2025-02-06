/*
 *  Description: This file defines the YourRankIsNotHighEnoughError class.
 *               It is thrown when a user's rank is not high enough to perform an action.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.springframework.http.HttpStatus

class YourRankIsNotHighEnoughError(yourRank: Int, requiredRank: Int) :
    BaseError(
        HttpStatus.BAD_REQUEST,
        "Your rank is not enough. Your rank is $yourRank, while rank $requiredRank is required",
        mapOf("yourRank" to yourRank, "requiredRank" to requiredRank),
    )
