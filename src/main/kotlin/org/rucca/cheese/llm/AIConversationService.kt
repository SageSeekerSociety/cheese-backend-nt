package org.rucca.cheese.llm

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatReference
import com.aallam.openai.api.chat.ChatRole
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.RoundingMode
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.llm.error.ConversationNotFoundError
import org.rucca.cheese.llm.processor.ResponseProcessorRegistry
import org.rucca.cheese.llm.service.LLMService
import org.rucca.cheese.llm.service.UserQuotaService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AIConversationService(
    private val conversationRepository: AIConversationRepository,
    private val messageRepository: AIMessageRepository,
    private val llmService: LLMService,
    private val userQuotaService: UserQuotaService,
    private val objectMapper: ObjectMapper,
    private val responseProcessorRegistry: ResponseProcessorRegistry,
) {
    private val logger = LoggerFactory.getLogger(AIConversationService::class.java)

    fun findByConversationId(conversationId: String): AIConversationEntity? {
        return conversationRepository.findByConversationId(conversationId)
    }

    @Transactional
    fun createConversation(
        conversationId: String,
        moduleType: String,
        ownerId: IdType,
        title: String? = null,
        contextId: IdType? = null,
    ): AIConversationEntity {
        val conversation =
            AIConversationEntity().apply {
                this.conversationId = conversationId
                this.moduleType = moduleType
                this.ownerId = ownerId
                this.title = title
                this.contextId = contextId
            }
        return conversationRepository.save(conversation)
    }

    fun getMessageHistoryPath(messageId: IdType?, maxDepth: Int = 5): List<AIMessageEntity> {
        if (messageId == null) {
            return emptyList()
        }

        val path = mutableListOf<AIMessageEntity>()
        var currentMessageId = messageId

        // Traverse from the specified message up to the root message
        while (currentMessageId != null && path.size < maxDepth) {
            val message = messageRepository.findById(currentMessageId).orElse(null) ?: break
            path.add(0, message) // Add at the beginning of list to maintain oldest to newest order
            currentMessageId = message.parentId
        }

        return path
    }

    fun createMessage(
        conversationId: IdType,
        role: String,
        modelType: String,
        content: String,
        parentId: IdType? = null,
        reasoningContent: String? = null,
        reasoningTimeMs: Long? = null,
        metadata: AIMessageMetadata? = null,
        tokensUsed: Int? = null,
        seuConsumed: java.math.BigDecimal? = null,
    ): AIMessageEntity {
        val message =
            AIMessageEntity().apply {
                this.conversation = conversationRepository.getReferenceById(conversationId)
                this.role = role
                this.modelType = modelType
                this.content = content
                this.parentId = parentId
                this.reasoningContent = reasoningContent
                this.reasoningTimeMs = reasoningTimeMs
                this.metadata = metadata
                this.tokensUsed = tokensUsed
                this.seuConsumed = seuConsumed
            }

        return messageRepository.save(message)
    }

    suspend fun streamConversation(
        systemPrompt: String,
        userMessage: String,
        historyMessages: List<AIMessageEntity> = emptyList(),
        conversationId: IdType? = null,
        parentMessageId: IdType? = null,
        userId: IdType,
        modelType: String? = null,
        moduleType: String = "default",
    ): Flow<String> {
        return flow {
                val responseProcessor = responseProcessorRegistry.getProcessor(moduleType)
                logger.debug(
                    "Using response processor: ${responseProcessor::class.simpleName} for module $moduleType"
                )

                val currentResponse = StringBuilder()

                val chatMessages = buildChatMessages(systemPrompt, userMessage, historyMessages)

                var currentReasoning = false
                val currentReasoningContent = StringBuilder()
                var tokensUsed = 0

                var reasoningStartTime: Long? = null
                var reasoningEndTime: Long? = null
                var totalReasoningTimeMs: Long = 0

                logger.info(
                    "Starting conversation stream for user $userId, moduleType: $moduleType, modelType: $modelType"
                )

                var references: List<ChatReference>? = null
                var followupQuestions: List<String>? = null

                try {
                    llmService
                        .streamCompletionWithHistory(
                            messages = chatMessages,
                            modelType = modelType,
                            jsonResponse = false,
                        )
                        .collect { completion ->
                            if (!completion.references.isNullOrEmpty()) {
                                references = completion.references!!
                                emit(
                                    "[REFERENCES]${objectMapper.writeValueAsString(completion.references)}"
                                )
                                logger.debug("References: {}", completion.references)
                            }
                            if (completion.choices.isEmpty()) {
                                val totalToken =
                                    completion.botUsage?.modelUsage?.sumOf { it.totalTokens ?: 0 }
                                        ?: completion.usage?.totalTokens
                                        ?: 0
                                tokensUsed += totalToken
                                logger.debug("Bot usage tokens: $totalToken")
                            } else {
                                val content = completion.choices.first().delta?.content ?: ""
                                val reasoningContent =
                                    completion.choices.first().delta?.reasoningContent

                                if (!reasoningContent.isNullOrEmpty()) {
                                    logger.debug("Reasoning content: $reasoningContent")
                                    if (!currentReasoning) {
                                        currentReasoning = true
                                        reasoningStartTime = System.currentTimeMillis()
                                        emit("[REASONING_START]")
                                    }
                                    currentReasoningContent.append(reasoningContent)
                                    emit("[REASONING_PARTIAL]$reasoningContent")
                                } else {
                                    logger.debug("Response content: $content")
                                    if (currentReasoning) {
                                        currentReasoning = false
                                        reasoningEndTime = System.currentTimeMillis()
                                        if (reasoningStartTime != null) {
                                            totalReasoningTimeMs =
                                                reasoningEndTime - reasoningStartTime
                                            emit("[REASONING_TIME]$totalReasoningTimeMs")
                                        }
                                        emit("[REASONING_END]$currentReasoningContent")
                                    }

                                    val result =
                                        responseProcessor.processStreamChunk(
                                            content,
                                            currentResponse,
                                        )

                                    if (!result.shouldSkip && result.content.isNotEmpty()) {
                                        emit("[${result.eventType}]${result.content}")
                                    }
                                }
                            }
                        }

                    val cacheKey = "ai_conversation:$userId:${System.currentTimeMillis()}"
                    val resourceType = llmService.getModelResourceType(modelType)
                    val quotaConsumption =
                        userQuotaService.checkAndDeductQuota(
                            userId = userId,
                            resourceType = resourceType,
                            tokensUsed = tokensUsed,
                            cacheKey = cacheKey,
                        )

                    val processedResponse =
                        responseProcessor.finalizeResponse(currentResponse.toString())
                    emit("[RESPONSE]${processedResponse.mainContent}")

                    processedResponse.metadata["followupQuestions"]
                        ?.takeIf { it is List<*> }
                        ?.let { followupQuestions = (it as List<*>).filterIsInstance<String>() }
                    processedResponse.metadata.forEach { (key, value) ->
                        emit("[${key.uppercase()}]${objectMapper.writeValueAsString(value)}")
                    }

                    emit("[TOKENS_USED]${tokensUsed}")
                    emit(
                        "[SEU_CONSUMED]${quotaConsumption.seuConsumed.setScale(2, RoundingMode.HALF_UP).toDouble()}"
                    )

                    if (conversationId != null) {
                        val userMessageEntity =
                            createMessage(
                                conversationId = conversationId,
                                role = "user",
                                modelType = modelType ?: llmService.defaultModelType,
                                content = userMessage,
                                parentId = parentMessageId,
                                reasoningContent = currentReasoningContent.toString(),
                                reasoningTimeMs = totalReasoningTimeMs,
                                metadata =
                                    AIMessageMetadata(
                                        followupQuestions = followupQuestions,
                                        references = references,
                                    ),
                                tokensUsed = tokensUsed,
                                seuConsumed = quotaConsumption.seuConsumed,
                            )

                        val assistantMessage =
                            createMessage(
                                conversationId = conversationId,
                                role = "assistant",
                                modelType = modelType ?: llmService.defaultModelType,
                                content = processedResponse.mainContent,
                                parentId = userMessageEntity.id,
                                reasoningContent = currentReasoningContent.toString(),
                                reasoningTimeMs = totalReasoningTimeMs,
                                metadata =
                                    AIMessageMetadata(
                                        followupQuestions = followupQuestions,
                                        references = references,
                                    ),
                                tokensUsed = tokensUsed,
                                seuConsumed = quotaConsumption.seuConsumed,
                            )

                        emit("[MESSAGE_ID]${assistantMessage.id}")
                    }
                } catch (e: Exception) {
                    logger.error("Error in streamConversation: ${e.message}", e)
                    emit("[ERROR]${e.message}")
                }
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun createConversationMessage(
        systemPrompt: String,
        userMessage: String,
        historyMessages: List<AIMessageEntity> = emptyList(),
        conversationId: IdType,
        parentMessageId: IdType? = null,
        userId: IdType,
        modelType: String? = null,
        moduleType: String = "default",
    ): AIMessageEntity {
        val responseProcessor = responseProcessorRegistry.getProcessor(moduleType)

        val chatMessages = buildChatMessages(systemPrompt, userMessage, historyMessages)

        val startTime = System.currentTimeMillis()
        val (response, tokensUsed) =
            llmService.getCompletionWithHistory(
                messages = chatMessages,
                modelType = modelType,
                jsonResponse = false,
            )
        val endTime = System.currentTimeMillis()
        val reasoningTimeMs = endTime - startTime

        val cacheKey = "ai_conversation:$userId:${System.currentTimeMillis()}"
        val resourceType = llmService.getModelResourceType(modelType)
        val quotaConsumption =
            userQuotaService.checkAndDeductQuota(
                userId = userId,
                resourceType = resourceType,
                tokensUsed = tokensUsed,
                cacheKey = cacheKey,
            )

        val processedResponse = responseProcessor.process(response)
        val followupQuestions =
            processedResponse.metadata["followupQuestions"]
                ?.takeIf { it is List<*> }
                ?.let { it as List<*> }
                ?.filterIsInstance<String>()

        val userMessageEntity =
            createMessage(
                conversationId = conversationId,
                role = "user",
                modelType = modelType ?: llmService.defaultModelType,
                content = userMessage,
                parentId = parentMessageId,
                reasoningContent = null,
                reasoningTimeMs = reasoningTimeMs,
                metadata = AIMessageMetadata(followupQuestions = followupQuestions),
                tokensUsed = tokensUsed,
                seuConsumed = quotaConsumption.seuConsumed,
            )

        return createMessage(
            conversationId = conversationId,
            role = "assistant",
            modelType = modelType ?: llmService.defaultModelType,
            content = processedResponse.mainContent,
            parentId = userMessageEntity.id,
            reasoningContent = null, // No reasoning content in non-streaming mode
            reasoningTimeMs = reasoningTimeMs,
            metadata = AIMessageMetadata(followupQuestions = followupQuestions),
            tokensUsed = tokensUsed,
            seuConsumed = quotaConsumption.seuConsumed,
        )
    }

    private fun buildChatMessages(
        systemPrompt: String,
        userMessage: String,
        historyMessages: List<AIMessageEntity>,
    ): List<ChatMessage> {
        val messages =
            mutableListOf<ChatMessage>(ChatMessage(role = ChatRole.System, content = systemPrompt))

        messages.addAll(
            historyMessages.map { message ->
                val role =
                    when (message.role) {
                        "user" -> ChatRole.User
                        "assistant" -> ChatRole.Assistant
                        else -> ChatRole.User
                    }
                ChatMessage(role = role, content = message.content)
            }
        )

        messages.add(ChatMessage(role = ChatRole.User, content = userMessage))

        return messages
    }

    suspend fun generateAndUpdateConversationTitle(
        conversationId: IdType,
        userQuestion: String,
        aiResponse: String,
        userId: IdType,
    ): String {
        try {
            val prompt =
                """
                请根据以下用户问题和AI回答的内容，生成一个简短的标题（10-15字以内）。标题应该准确反映对话的主题。
                只返回标题文本，不要添加任何其他内容或标点符号。
    
                用户问题：${userQuestion.take(200)}${if (userQuestion.length > 200) "..." else ""}
                
                AI回答：${aiResponse.take(300)}${if (aiResponse.length > 300) "..." else ""}
                """
                    .trimIndent()

            val (titleResponse, _) =
                llmService.getCompletionWithTokenCount(
                    prompt = prompt,
                    systemPrompt = "你是一个对话标题生成助手，只输出简短、准确的标题，不包含任何额外内容。",
                    modelType = "light",
                    jsonResponse = false,
                )

            val title = titleResponse.trim().take(30)
            withContext(Dispatchers.IO) {
                conversationRepository.updateTitleByIdAndOwnerId(conversationId, userId, title)
            }

            logger.debug("Generated title for conversation $conversationId: $title")
            return title
        } catch (e: Exception) {
            logger.error("Error generating title for conversation $conversationId: ${e.message}", e)
            return ""
        }
    }

    data class ConversationSummary(
        val conversationId: String,
        val title: String?,
        val messageCount: Int,
        val createdAt: OffsetDateTime,
        val updatedAt: OffsetDateTime,
        val latestUserAssistantPair: UserAssistantPair?,
    )

    data class UserAssistantPair(
        val userMessage: AIMessageEntity,
        val assistantMessage: AIMessageEntity,
    )

    fun getConversationSummariesByContextIds(
        moduleType: String,
        contextIds: List<IdType>,
        ownerId: IdType,
    ): List<ConversationSummary> {
        val conversations =
            conversationRepository.findByModuleTypeAndContextIdInAndOwnerId(
                moduleType = moduleType,
                contextIds = contextIds,
                ownerId = ownerId,
            )

        if (conversations.isEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<ConversationSummary>()

        for (conversation in conversations) {
            val messageCount = messageRepository.countByConversationId(conversation.id!!)

            val messages =
                messageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.id!!)

            if (messages.size >= 2) {
                val assistantMessage = messages.find { it.role == "assistant" }
                val userMessage =
                    messages.find { it.role == "user" && (assistantMessage?.parentId == it.id) }

                if (assistantMessage != null && userMessage != null) {
                    val summary =
                        ConversationSummary(
                            conversationId = conversation.conversationId,
                            title = conversation.title,
                            messageCount = messageCount.toInt(),
                            createdAt =
                                OffsetDateTime.of(
                                    conversation.createdAt,
                                    OffsetDateTime.now().offset,
                                ),
                            updatedAt =
                                OffsetDateTime.of(
                                    conversation.updatedAt ?: conversation.createdAt,
                                    OffsetDateTime.now().offset,
                                ),
                            latestUserAssistantPair =
                                UserAssistantPair(
                                    userMessage = userMessage,
                                    assistantMessage = assistantMessage,
                                ),
                        )
                    result.add(summary)
                    continue
                }
            }

            val summary =
                ConversationSummary(
                    conversationId = conversation.conversationId,
                    title = conversation.title,
                    messageCount = messageCount.toInt(),
                    createdAt =
                        OffsetDateTime.of(conversation.createdAt, OffsetDateTime.now().offset),
                    updatedAt =
                        OffsetDateTime.of(
                            conversation.updatedAt ?: conversation.createdAt,
                            OffsetDateTime.now().offset,
                        ),
                    latestUserAssistantPair = null,
                )
            result.add(summary)
        }

        return result.sortedByDescending { it.updatedAt }
    }

    fun deleteConversationByConversationId(conversationId: String) {
        val conversation =
            conversationRepository.findByConversationId(conversationId)
                ?: throw ConversationNotFoundError(conversationId)

        conversationRepository.delete(conversation)
    }
}
