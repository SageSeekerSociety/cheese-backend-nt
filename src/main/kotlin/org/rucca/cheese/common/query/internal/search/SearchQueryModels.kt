package org.rucca.cheese.common.query.internal.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

private val jsonMapper = Json { encodeDefaults = false }

@Serializable
sealed interface SearchQuery {
    fun toJsonElement(): JsonElement

    fun toJsonString(): String = jsonMapper.encodeToString(toJsonElement())
}

@Serializable
@SerialName("boolean")
data class BooleanQuery(
    @SerialName("must") val must: List<SearchQuery> = emptyList(),
    @SerialName("should") val should: List<SearchQuery> = emptyList(),
    @SerialName("must_not") val mustNot: List<SearchQuery> = emptyList(),
) : SearchQuery {
    override fun toJsonElement(): JsonElement = buildJsonObject {
        val boolContent = buildJsonObject {
            if (must.isNotEmpty()) put("must", JsonArray(must.map { it.toJsonElement() }))
            if (should.isNotEmpty()) put("should", JsonArray(should.map { it.toJsonElement() }))
            if (mustNot.isNotEmpty()) put("must_not", JsonArray(mustNot.map { it.toJsonElement() }))
        }
        put("boolean", boolContent)
    }
}

@Serializable
@SerialName("match")
data class MatchQuery(
    val field: String,
    val value: String,
    val distance: Int? = null,
    @SerialName("conjunction_mode") val conjunctionMode: Boolean? = null,
) : SearchQuery {
    override fun toJsonElement(): JsonElement = buildJsonObject {
        put("match", jsonMapper.encodeToJsonElement(this@MatchQuery))
    }
}

@Serializable
@SerialName("term")
data class TermQuery(val field: String, val value: JsonPrimitive) : SearchQuery {
    override fun toJsonElement(): JsonElement = buildJsonObject {
        put("term", jsonMapper.encodeToJsonElement(this@TermQuery))
    }
}

@Serializable
@SerialName("phrase")
data class PhraseQuery(val field: String, val phrases: List<String>, val slop: Int? = null) :
    SearchQuery {
    override fun toJsonElement(): JsonElement = buildJsonObject {
        put("phrase", jsonMapper.encodeToJsonElement(this@PhraseQuery))
    }
}

@Serializable
@SerialName("range")
data class RangeQuery(
    val field: String,
    val gte: JsonPrimitive? = null,
    val gt: JsonPrimitive? = null,
    val lte: JsonPrimitive? = null,
    val lt: JsonPrimitive? = null,
) : SearchQuery {
    override fun toJsonElement(): JsonElement = buildJsonObject {
        val rangeContent = buildJsonObject {
            put("field", JsonPrimitive(field))
            gte?.let { put("gte", it) }
            gt?.let { put("gt", it) }
            lte?.let { put("lte", it) }
            lt?.let { put("lt", it) }
        }
        put("range", rangeContent)
    }
}

@Serializable
@SerialName("boost")
data class BoostQuery(val query: SearchQuery, val factor: Double) : SearchQuery {
    override fun toJsonElement(): JsonElement = buildJsonObject {
        put(
            "boost",
            buildJsonObject {
                put("query", query.toJsonElement())
                put("factor", JsonPrimitive(factor))
            },
        )
    }
}
