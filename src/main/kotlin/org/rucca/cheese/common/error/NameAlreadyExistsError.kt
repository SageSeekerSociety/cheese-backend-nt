package org.rucca.cheese.common.error

import org.springframework.http.HttpStatus

class NameAlreadyExistsError(
        type: String,
        name: String,
) :
        BaseError(
                status = HttpStatus.CONFLICT,
                name = "NameAlreadyExistsError",
                message = "$type with name $name already exists",
                data =
                        mapOf(
                                "type" to type,
                                "name" to name,
                        ),
        )
