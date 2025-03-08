package org.rucca.cheese.utils

import java.time.LocalDateTime
import kotlin.math.floor
import org.json.JSONObject
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@Service
class TaskCreatorService(
    private val mockMvc: MockMvc,
    private val userCreatorService: UserCreatorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun testTaskName(): String {
        return "Test Task (${floor(Math.random() * 10000000000).toLong()})"
    }

    fun testTaskDeadline(): Long {
        return LocalDateTime.now().plusDays(7).toEpochMilli()
    }

    fun testTaskDefaultDeadline(): Long {
        return LocalDateTime.now().plusDays(7).toEpochMilli()
    }

    fun createTask(
        creatorToken: String,
        name: String = testTaskName(),
        submitterType: String = "USER",
        deadline: Long? = testTaskDeadline(),
        defaultDeadline: Long? = testTaskDefaultDeadline(),
        resubmittable: Boolean = true,
        editable: Boolean = true,
        intro: String = "This is a test task.",
        description: String = "Description of task",
        submissionSchema: List<Pair<String, String>> = listOf(Pair("Text Entry", "TEXT")),
        team: IdType? = null,
        space: IdType? = null,
        rank: Int? = null,
        topics: List<IdType> = emptyList(),
    ): IdType {
        val request =
            MockMvcRequestBuilders.post("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "name": "$name",
                  "submitterType": "$submitterType",
                  "deadline": "$deadline",
                  "defaultDeadline": $defaultDeadline,
                  "resubmittable": $resubmittable,
                  "editable": $editable,
                  "intro": "$intro",
                  "description": "$description",
                  "submissionSchema": [
                    ${
                        submissionSchema
                            .map {
                                """
                                {
                                  "prompt": "${it.first}",
                                  "type": "${it.second}"
                                }
                            """
                            }
                            .joinToString(",\n")
                    }
                  ],
                  "team": ${team ?: "null"},
                  "space": ${space ?: "null"},
                  "rank": ${rank ?: "null"},
                  "topics": [${topics.joinToString(",")}]
                }
            """
                )
        val response =
            mockMvc
                .perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(jsonPath("$.data.task.name").value(name))
                .andExpect(jsonPath("$.data.task.submitterType").value(submitterType))
                .andExpect(jsonPath("$.data.task.deadline").value(deadline))
                .andExpect(jsonPath("$.data.task.defaultDeadline").value(defaultDeadline))
                .andExpect(jsonPath("$.data.task.resubmittable").value(resubmittable))
                .andExpect(jsonPath("$.data.task.editable").value(editable))
                .andExpect(jsonPath("$.data.task.intro").value(intro))
                .andExpect(jsonPath("$.data.task.description").value(description))
        val json = JSONObject(response.andReturn().response.contentAsString)
        for (entry in submissionSchema) {
            val schema =
                json.getJSONObject("data").getJSONObject("task").getJSONArray("submissionSchema")
            val found = JsonArrayUtil.toArray(schema).find { it.getString("prompt") == entry.first }
            assert(found != null)
            assert(found!!.getString("type") == entry.second)
        }
        val taskId = json.getJSONObject("data").getJSONObject("task").getLong("id")
        logger.info("Created task: $taskId")
        return taskId
    }
}
