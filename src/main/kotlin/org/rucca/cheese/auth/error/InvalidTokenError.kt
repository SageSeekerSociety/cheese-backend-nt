/*
 *  Description: This file defines the InvalidTokenError class.
 *               It is thrown when the token is invalid.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.auth.error

import org.rucca.cheese.common.error.BaseError
import org.springframework.http.HttpStatus

class InvalidTokenError(message: String = "Invalid token.") :
    BaseError(HttpStatus.UNAUTHORIZED, message)
