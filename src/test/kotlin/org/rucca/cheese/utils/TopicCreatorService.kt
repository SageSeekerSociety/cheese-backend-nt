/*
 *  Description: Responsible for creating topic in the legacy system.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.utils

import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.MediaType
import org.json.JSONObject
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.persistent.IdType
import org.springframework.stereotype.Service

@Service
class TopicCreatorService(
    private val applicationConfig: ApplicationConfig,
) {
    fun createTopic(token: String, name: String): IdType {
        val client = ClientBuilder.newBuilder().build()
        val target = client.target(applicationConfig.legacyUrl).path("/topics")
        val jsonBody = JSONObject().put("name", name)
        val response =
            target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer $token")
                .post(Entity.json(jsonBody.toString()))
        val json = JSONObject(response.readEntity(String::class.java))
        return json.getJSONObject("data").getLong("id")
    }
}
