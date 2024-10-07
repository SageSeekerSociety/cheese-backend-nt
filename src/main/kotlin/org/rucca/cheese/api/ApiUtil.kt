package org.rucca.cheese.api

import java.io.IOException
import javax.servlet.http.HttpServletResponse
import org.springframework.web.context.request.NativeWebRequest

object ApiUtil {
    fun setExampleResponse(req: NativeWebRequest, contentType: String, example: String) {
        try {
            val res = req.getNativeResponse(HttpServletResponse::class.java)
            res?.characterEncoding = "UTF-8"
            res?.addHeader("Content-Type", contentType)
            res?.writer?.print(example)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
