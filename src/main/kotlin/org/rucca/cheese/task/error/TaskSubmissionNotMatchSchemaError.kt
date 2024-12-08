/*
 *  Description: This file defines the TaskSubmissionNotMatchSchemaError class.
 *               It is thrown when a submission does not match schema.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.springframework.http.HttpStatus

class TaskSubmissionNotMatchSchemaError() :
    BaseError(
        HttpStatus.BAD_REQUEST,
        "Task submission does not match schema",
    )
