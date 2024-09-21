package org.rucca.cheese.utils

import org.json.JSONArray
import org.json.JSONObject

object JsonArrayUtil {
    fun toArray(jsonArray: JSONArray): List<JSONObject> {
        val array: MutableList<JSONObject> = mutableListOf()
        for (i in 0 until jsonArray.length()) {
            array.add(jsonArray.get(i) as JSONObject)
        }
        return array
    }
}