package org.rucca.cheese.llm.service

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.flow.*
import org.rucca.cheese.llm.config.LLMProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LLMService(private val llmClient: OpenAI, private val properties: LLMProperties) {
    private val logger = LoggerFactory.getLogger(LLMService::class.java)

    val defaultModelType: String = properties.defaultModel.name

    /** 获取指定模型类型的ModelId */
    private fun getModelId(modelType: String?): ModelId {
        try {
            val modelConfig = properties.getModelConfig(modelType)
            return ModelId(modelConfig.name)
        } catch (e: Exception) {
            logger.error("Failed to get model ID for type: $modelType", e)
            throw e
        }
    }

    /** 获取指定模型类型的温度参数 */
    private fun getTemperature(modelType: String?): Double {
        try {
            val modelConfig = properties.getModelConfig(modelType)
            return modelConfig.temperature
        } catch (e: Exception) {
            logger.error("Failed to get temperature for model type: $modelType", e)
            throw e
        }
    }

    /** 获取指定模型类型的最大token数 */
    private fun getMaxTokens(modelType: String?): Int {
        try {
            val modelConfig = properties.getModelConfig(modelType)
            return modelConfig.maxTokens
        } catch (e: Exception) {
            logger.error("Failed to get max tokens for model type: $modelType", e)
            throw e
        }
    }

    suspend fun getCompletionWithTokenCount(
        prompt: String,
        systemPrompt: String = "",
        modelType: String? = null,
        jsonResponse: Boolean = false,
    ): Pair<String, Int> {
        val modelId = getModelId(modelType)
        val temperature = getTemperature(modelType)
        val maxTokens = getMaxTokens(modelType)

        val chatCompletionRequest =
            ChatCompletionRequest(
                model = modelId,
                messages =
                    listOf(
                        ChatMessage(role = ChatRole.System, content = systemPrompt),
                        ChatMessage(role = ChatRole.User, content = prompt),
                    ),
                temperature = temperature,
                maxTokens = maxTokens,
                responseFormat =
                    if (jsonResponse) ChatResponseFormat.JsonObject else ChatResponseFormat.Text,
            )

        val completion: ChatCompletion = llmClient.chatCompletion(chatCompletionRequest)
        val content =
            completion.choices.first().message.content
                ?: throw RuntimeException("No response from LLM service")

        val totalToken =
            completion.botUsage?.modelUsage?.sumOf { it.totalTokens ?: 0 }
                ?: completion.usage?.totalTokens
                ?: 0

        return Pair(content, totalToken)
    }

    /** 使用完整对话历史获取回复和token计数 */
    suspend fun getCompletionWithHistory(
        messages: List<ChatMessage>,
        modelType: String? = null,
        jsonResponse: Boolean = false,
    ): Pair<String, Int> {
        val modelId = getModelId(modelType)
        val temperature = getTemperature(modelType)
        val maxTokens = getMaxTokens(modelType)

        val chatCompletionRequest =
            ChatCompletionRequest(
                model = modelId,
                messages = messages,
                temperature = temperature,
                maxTokens = maxTokens,
                responseFormat =
                    if (jsonResponse) ChatResponseFormat.JsonObject else ChatResponseFormat.Text,
            )

        val completion: ChatCompletion = llmClient.chatCompletion(chatCompletionRequest)
        val content =
            completion.choices.first().message.content
                ?: throw RuntimeException("No response from LLM service")

        val totalToken =
            completion.botUsage?.modelUsage?.sumOf { it.totalTokens ?: 0 }
                ?: completion.usage?.totalTokens
                ?: 0

        return Pair(content, totalToken)
    }

    suspend fun getCompletion(
        prompt: String,
        systemPrompt: String = "",
        modelType: String? = null,
        jsonResponse: Boolean = false,
    ): String {
        return getCompletionWithTokenCount(prompt, systemPrompt, modelType, jsonResponse).first
    }

    suspend fun streamCompletion(
        prompt: String,
        systemPrompt: String = "",
        modelType: String? = null,
        jsonResponse: Boolean = true,
    ): Flow<ChatCompletionChunk> {
        val modelId = getModelId(modelType)
        val temperature = getTemperature(modelType)
        val maxTokens = getMaxTokens(modelType)

        val chatCompletionRequest =
            ChatCompletionRequest(
                model = modelId,
                messages =
                    listOf(
                        ChatMessage(role = ChatRole.System, content = systemPrompt),
                        ChatMessage(role = ChatRole.User, content = prompt),
                    ),
                temperature = temperature,
                maxTokens = maxTokens,
                responseFormat =
                    if (jsonResponse) ChatResponseFormat.JsonObject else ChatResponseFormat.Text,
                streamOptions = StreamOptions(includeUsage = true),
            )

        return llmClient.chatCompletions(chatCompletionRequest)
    }

    /** 使用完整对话历史流式获取回复 */
    suspend fun streamCompletionWithHistory(
        messages: List<ChatMessage>,
        modelType: String? = null,
        jsonResponse: Boolean = false,
    ): Flow<ChatCompletionChunk> {
        val modelId = getModelId(modelType)
        val temperature = getTemperature(modelType)
        val maxTokens = getMaxTokens(modelType)

        val chatCompletionRequest =
            ChatCompletionRequest(
                model = modelId,
                messages = messages,
                temperature = temperature,
                maxTokens = maxTokens,
                responseFormat =
                    if (jsonResponse) ChatResponseFormat.JsonObject else ChatResponseFormat.Text,
                streamOptions = StreamOptions(includeUsage = true),
            )

        return llmClient.chatCompletions(chatCompletionRequest)
    }

    @Deprecated(
        "使用指定modelType的新版本方法",
        ReplaceWith("getCompletionWithHistory(messages, null, jsonResponse)"),
    )
    suspend fun getCompletionWithHistory(
        messages: List<ChatMessage>,
        model: ModelId = ModelId(properties.defaultModel.name),
        jsonResponse: Boolean = false,
    ): Pair<String, Int> {
        return getCompletionWithHistory(messages, null, jsonResponse)
    }

    @Deprecated(
        "使用指定modelType的新版本方法",
        ReplaceWith("getCompletion(prompt, systemPrompt, null, jsonResponse)"),
    )
    suspend fun getCompletion(
        prompt: String,
        systemPrompt: String = "",
        model: ModelId = ModelId(properties.defaultModel.name),
        jsonResponse: Boolean = false,
    ): String {
        return getCompletion(prompt, systemPrompt, null, jsonResponse)
    }

    @Deprecated(
        "使用指定modelType的新版本方法",
        ReplaceWith("streamCompletion(prompt, systemPrompt, null, jsonResponse)"),
    )
    suspend fun streamCompletion(
        prompt: String,
        systemPrompt: String = "",
        model: ModelId = ModelId(properties.defaultModel.name),
        jsonResponse: Boolean = true,
    ): Flow<ChatCompletionChunk> {
        return streamCompletion(prompt, systemPrompt, null, jsonResponse)
    }

    @Deprecated(
        "使用指定modelType的新版本方法",
        ReplaceWith("streamCompletionWithHistory(messages, null, jsonResponse)"),
    )
    suspend fun streamCompletionWithHistory(
        messages: List<ChatMessage>,
        model: ModelId = ModelId(properties.defaultModel.name),
        jsonResponse: Boolean = false,
    ): Flow<ChatCompletionChunk> {
        return streamCompletionWithHistory(messages, null, jsonResponse)
    }
}
