package org.rucca.cheese.llm.service

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import org.rucca.cheese.llm.config.LLMProperties
import org.springframework.stereotype.Service

@Service
class LLMService(private val llmClient: OpenAI, private val properties: LLMProperties) {
    suspend fun getCompletion(
        prompt: String,
        systemPrompt: String = "",
        jsonResponse: Boolean = false,
    ): String {
        val chatCompletionRequest =
            ChatCompletionRequest(
                model = ModelId(properties.model.name),
                messages =
                    listOf(
                        ChatMessage(role = ChatRole.System, content = systemPrompt),
                        ChatMessage(role = ChatRole.User, content = prompt),
                    ),
                temperature = properties.model.temperature,
                maxTokens = properties.model.maxTokens,
                responseFormat =
                    if (jsonResponse) ChatResponseFormat.JsonObject else ChatResponseFormat.Text,
            )

        val completion: ChatCompletion = llmClient.chatCompletion(chatCompletionRequest)
        return completion.choices.first().message.content
            ?: throw RuntimeException("No response from LLM service")
    }
}
