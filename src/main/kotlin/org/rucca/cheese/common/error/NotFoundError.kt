package org.rucca.cheese.common.error

import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class NotFoundError(
        type: String,
        id: IdType,
) :
        BaseError(
                status = HttpStatus.NOT_FOUND,
                message = "$type with id $id was not found",
                data =
                        mapOf(
                                "type" to type,
                                "id" to id,
                        ),
        )
