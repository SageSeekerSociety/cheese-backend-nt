/*
 *  Description: This file defines the AlreadyBeSpaceOwnerError class.
 *               It is thrown when a user is already the owner of a space.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.space.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class AlreadyBeSpaceOwnerError(spaceId: IdType, userId: IdType) :
    BaseError(
        HttpStatus.CONFLICT,
        "$userId is already the owner of space $spaceId",
        mapOf("spaceId" to spaceId, "userId" to userId)
    )
