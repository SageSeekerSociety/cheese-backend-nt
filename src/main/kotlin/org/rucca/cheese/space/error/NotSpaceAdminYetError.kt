/*
 *  Description: This file defines the NotSpaceAdminYetError class.
 *               It is thrown when a user is not yet an admin of a space.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.space.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class NotSpaceAdminYetError(spaceId: IdType, userId: IdType) :
    BaseError(
        HttpStatus.NOT_FOUND,
        "User $userId is not a space admin of space $spaceId yet",
        mapOf("spaceId" to spaceId, "userId" to userId),
    )
