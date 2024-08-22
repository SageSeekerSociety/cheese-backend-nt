package org.rucca.cheese.common

import org.springframework.http.HttpStatus

class NotFoundError(
        type: String,
        id: org.rucca.cheese.common.IdType,
) :
        org.rucca.cheese.common.BaseError(
                status = HttpStatus.NOT_FOUND,
                name = "NotFoundError",
                message = "$type with id $id was not found",
                data =
                        mapOf(
                                "type" to type,
                                "id" to id,
                        ),
        )
