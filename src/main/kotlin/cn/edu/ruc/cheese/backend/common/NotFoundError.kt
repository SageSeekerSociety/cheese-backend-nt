package cn.edu.ruc.cheese.backend.common

import org.springframework.http.HttpStatus

class NotFoundError(
        type: String,
        id: IdType,
) :
        BaseError(
                status = HttpStatus.NOT_FOUND,
                name = "NotFoundError",
                message = "$type with id $id was not found",
                data =
                        mapOf(
                                "type" to type,
                                "id" to id,
                        ),
        )
