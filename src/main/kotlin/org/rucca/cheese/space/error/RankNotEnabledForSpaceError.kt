/*
 *  Description: This file defines the RankNotEnabledForSpaceError class.
 *               It is thrown when rank is not enabled for a space.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.space.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class RankNotEnabledForSpaceError(spaceId: IdType) :
    BaseError(
        HttpStatus.BAD_REQUEST,
        "Rank is not enabled for space $spaceId",
        mapOf("spaceId" to spaceId)
    )
