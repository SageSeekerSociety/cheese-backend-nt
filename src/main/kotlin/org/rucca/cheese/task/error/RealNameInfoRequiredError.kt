/*
 *  Description: This file defines the RealNameInfoRequiredError class.
 *
 *  Author(s):
 *      Your Name <your.email@example.com>
 *
 */

package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class RealNameInfoRequiredError(entityId: IdType) :
    BaseError(
        status = HttpStatus.BAD_REQUEST,
        message = "Real name information is required for entity with ID $entityId",
        data = mapOf("entityId" to entityId),
    )
