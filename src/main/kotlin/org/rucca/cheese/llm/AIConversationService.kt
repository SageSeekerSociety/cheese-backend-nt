package org.rucca.cheese.llm

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
import java.time.OffsetDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.llm.error.ConversationNotFoundError
import org.rucca.cheese.llm.model.AIResourceType
import org.rucca.cheese.llm.processor.ResponseProcessorRegistry
import org.rucca.cheese.llm.service.LLMService
import org.rucca.cheese.llm.service.UserQuotaService
import org.rucca.cheese.model.AIConversationDTO
import org.rucca.cheese.model.AIMessageDTO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 通用AI对话服务，处理对话和消息的创建、查询和流式输出 */
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
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** 创建新的对话 */
    @Transactional
    fun createConversation(
        conversationId: String,
        moduleType: String,
        ownerId: IdType,
        modelType: String,
        title: String? = null,
        contextId: IdType? = null,
    ): AIConversationEntity {
        val conversation =
            AIConversationEntity().apply {
                this.conversationId = conversationId
                this.moduleType = moduleType
                this.ownerId = ownerId
                this.modelType = modelType
                this.title = title
                this.contextId = contextId
            }
        return conversationRepository.save(conversation)
    }

    /** 获取消息的历史路径（从根到特定消息的所有祖先消息） */
    fun getMessageHistoryPath(messageId: IdType?, maxDepth: Int = 5): List<AIMessageEntity> {
        if (messageId == null) {
            return emptyList()
        }

        val path = mutableListOf<AIMessageEntity>()
        var currentMessageId = messageId

        // 从指定消息向上追溯到根消息
        while (currentMessageId != null && path.size < maxDepth) {
            val message = messageRepository.findById(currentMessageId).orElse(null) ?: break
            path.add(0, message) // 在列表头部添加，保持从老到新的顺序
            currentMessageId = message.parentId
        }

        return path
    }

    /** 创建新消息 */
    @Transactional
    fun createMessage(
        conversationId: IdType,
        role: String,
        content: String,
        parentId: IdType? = null,
        reasoningContent: String? = null,
        reasoningTimeMs: Long? = null,
        metadata: Map<String, Any> = mapOf(),
    ): AIMessageEntity {
        val conversation =
            conversationRepository.findById(conversationId).orElseThrow {
                EntityNotFoundException("对话不存在: $conversationId")
            }

        val message =
            AIMessageEntity().apply {
                this.conversation = conversation
                this.role = role
                this.content = content
                this.parentId = parentId
                this.reasoningContent = reasoningContent
                this.reasoningTimeMs = reasoningTimeMs
                this.metadata = metadata
            }

        return messageRepository.save(message)
    }

    /** 对话流接口 */
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
                // 获取适合该模块的响应处理器
                val responseProcessor = responseProcessorRegistry.getProcessor(moduleType)
                logger.debug("使用响应处理器: ${responseProcessor.javaClass.simpleName} 处理模块: $moduleType")

                // 用于累积响应内容的缓冲区
                val currentResponse = StringBuilder()

                // 1. 构建对话消息
                val chatMessages = buildChatMessages(systemPrompt, userMessage, historyMessages)

                // 2. 收集响应
                var currentReasoning = false
                val currentReasoningContent = StringBuilder()
                var tokensUsed = 0

                // 添加推理时间计时器
                var reasoningStartTime: Long? = null
                var reasoningEndTime: Long? = null
                var totalReasoningTimeMs: Long = 0

                logger.info(
                    "Starting conversation stream for user $userId, moduleType: $moduleType, modelType: $modelType"
                )

                try {
                    // 3. 调用LLM服务
                    llmService
                        .streamCompletionWithHistory(
                            messages = chatMessages,
                            modelType = modelType,
                            jsonResponse = false,
                        )
                        .collect { completion ->
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
                                                reasoningEndTime!! - reasoningStartTime!!
                                            emit("[REASONING_TIME]$totalReasoningTimeMs")
                                        }
                                        emit("[REASONING_END]$currentReasoningContent")
                                    }

                                    // 使用响应处理器处理内容块
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

                    // 4. 处理配额
                    val cacheKey = "ai_conversation:$userId:${System.currentTimeMillis()}"
                    userQuotaService.checkAndDeductQuota(
                        userId = userId,
                        resourceType = AIResourceType.STANDARD,
                        tokensUsed = tokensUsed,
                        cacheKey = cacheKey,
                    )

                    // 5. 处理最终响应
                    val processedResponse =
                        responseProcessor.finalizeResponse(currentResponse.toString())
                    emit("[RESPONSE]${processedResponse.mainContent}")

                    // 6. 处理元数据
                    processedResponse.metadata.forEach { (key, value) ->
                        emit("[${key.uppercase()}]${objectMapper.writeValueAsString(value)}")
                    }

                    // 7. 保存消息
                    if (conversationId != null) {
                        // 创建用户消息
                        val userMessageEntity =
                            createMessage(
                                conversationId = conversationId,
                                role = "user",
                                content = userMessage,
                                parentId = parentMessageId,
                            )

                        // 创建助手消息
                        val assistantMessage =
                            createMessage(
                                conversationId = conversationId,
                                role = "assistant",
                                content = processedResponse.mainContent,
                                parentId = userMessageEntity.id,
                                reasoningContent = currentReasoningContent.toString(),
                                reasoningTimeMs = totalReasoningTimeMs,
                                metadata = processedResponse.metadata,
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

    /** 非流式对话 */
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
        // 获取适合该模块的响应处理器
        val responseProcessor = responseProcessorRegistry.getProcessor(moduleType)

        // 1. 构建对话消息
        val chatMessages = buildChatMessages(systemPrompt, userMessage, historyMessages)

        // 2. 调用 LLM 服务
        val startTime = System.currentTimeMillis()
        val (response, tokensUsed) =
            llmService.getCompletionWithHistory(
                messages = chatMessages,
                modelType = modelType,
                jsonResponse = false,
            )
        val endTime = System.currentTimeMillis()
        val reasoningTimeMs = endTime - startTime

        // 3. 处理配额
        val cacheKey = "ai_conversation:$userId:${System.currentTimeMillis()}"
        userQuotaService.checkAndDeductQuota(
            userId = userId,
            resourceType = AIResourceType.STANDARD,
            tokensUsed = tokensUsed,
            cacheKey = cacheKey,
        )

        // 4. 处理响应
        val processedResponse = responseProcessor.process(response)

        // 5. 创建用户消息
        val userMessageEntity =
            createMessage(
                conversationId = conversationId,
                role = "user",
                content = userMessage,
                parentId = parentMessageId,
            )

        // 6. 创建助手消息
        return createMessage(
            conversationId = conversationId,
            role = "assistant",
            content = processedResponse.mainContent,
            parentId = userMessageEntity.id,
            reasoningContent = null, // 非流式对话没有推理内容
            reasoningTimeMs = reasoningTimeMs,
            metadata = processedResponse.metadata,
        )
    }

    /** 构建聊天消息 */
    private fun buildChatMessages(
        systemPrompt: String,
        userMessage: String,
        historyMessages: List<AIMessageEntity>,
    ): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        // 添加系统提示
        messages.add(ChatMessage(role = ChatRole.System, content = systemPrompt))

        // 添加历史消息
        historyMessages.forEach { message ->
            val role =
                when (message.role) {
                    "user" -> ChatRole.User
                    "assistant" -> ChatRole.Assistant
                    else -> ChatRole.User // 默认为用户
                }
            messages.add(ChatMessage(role = role, content = message.content))
        }

        // 添加当前用户消息
        messages.add(ChatMessage(role = ChatRole.User, content = userMessage))

        return messages
    }

    /** 获取对话DTO */
    fun getConversationDTO(conversationId: String): AIConversationDTO {
        val conversation =
            conversationRepository.findByConversationId(conversationId)
                ?: throw EntityNotFoundException("对话不存在: $conversationId")

        val messageCount = messageRepository.countByConversationId(conversation.id!!)

        return AIConversationDTO(
            id = conversation.id!!,
            conversationId = conversation.conversationId,
            title = conversation.title!!,
            moduleType = conversation.moduleType,
            contextId = conversation.contextId!!,
            ownerId = conversation.ownerId,
            modelType = conversation.modelType,
            messageCount = messageCount.toInt(),
            createdAt = OffsetDateTime.of(conversation.createdAt, OffsetDateTime.now().offset),
            updatedAt =
                OffsetDateTime.of(
                    conversation.updatedAt ?: conversation.createdAt,
                    OffsetDateTime.now().offset,
                ),
        )
    }

    /** 获取消息列表 */
    fun getConversationMessages(conversationId: String): List<AIMessageDTO> {
        val conversation =
            conversationRepository.findByConversationId(conversationId)
                ?: throw EntityNotFoundException("对话不存在: $conversationId")

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.id!!).map {
            it.toDTO()
        }
    }

    /** 为对话生成并更新标题 */
    suspend fun generateAndUpdateConversationTitle(
        conversationId: IdType,
        userQuestion: String,
        aiResponse: String,
        userId: IdType,
    ): String {
        try {
            // 查找对话实体
            val conversation =
                withContext(Dispatchers.IO) { conversationRepository.findById(conversationId) }
                    .orElseThrow { EntityNotFoundException("对话不存在: $conversationId") }

            // 如果已经有标题，则跳过
            if (!conversation.title.isNullOrBlank()) {
                return conversation.title!!
            }

            // 构建提示词
            val prompt =
                """
            请根据以下用户问题和AI回答的内容，生成一个简短的标题（10-15字以内）。标题应该准确反映对话的主题。
            只返回标题文本，不要添加任何其他内容或标点符号。

            用户问题：${userQuestion.take(200)}${if (userQuestion.length > 200) "..." else ""}
            
            AI回答：${aiResponse.take(300)}${if (aiResponse.length > 300) "..." else ""}
            """
                    .trimIndent()

            // 使用 light 模型生成标题
            val (titleResponse, _) =
                llmService.getCompletionWithTokenCount(
                    prompt = prompt,
                    systemPrompt = "你是一个对话标题生成助手，只输出简短、准确的标题，不包含任何额外内容。",
                    modelType = "light", // 使用轻量模型
                    jsonResponse = false,
                )

            // 清理标题（移除多余的引号、空白等）
            val title = titleResponse.trim().take(30)

            // 更新对话标题
            conversation.title = title
            withContext(Dispatchers.IO) { conversationRepository.save(conversation) }

            logger.info("Generated title for conversation $conversationId: $title")
            return title
        } catch (e: Exception) {
            // 对标题生成的错误进行处理，但不影响主要流程
            logger.error("Error generating title for conversation $conversationId: ${e.message}", e)
            return ""
        }
    }

    /** 对话摘要，包含对话的基本信息和最新的用户-助手消息对 */
    data class ConversationSummary(
        val conversationId: String,
        val title: String?,
        val modelType: String,
        val messageCount: Int,
        val createdAt: OffsetDateTime,
        val updatedAt: OffsetDateTime,
        val latestUserAssistantPair: UserAssistantPair?,
    )

    /** 用户-助手消息对 */
    data class UserAssistantPair(
        val userMessage: AIMessageEntity,
        val assistantMessage: AIMessageEntity,
    )

    /** 获取指定上下文ID列表的对话摘要 */
    fun getConversationSummariesByContextIds(
        moduleType: String,
        contextIds: List<IdType>,
    ): List<ConversationSummary> {
        // 1. 获取所有符合条件的对话
        val conversations =
            conversationRepository.findByModuleTypeAndContextIdIn(
                moduleType = moduleType,
                contextIds = contextIds,
            )

        if (conversations.isEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<ConversationSummary>()

        // 2. 获取每个对话的最新消息
        for (conversation in conversations) {
            // 获取每个会话的消息数量
            val messageCount = messageRepository.countByConversationId(conversation.id!!)

            // 获取最新的消息对（用户-助手）
            val messages =
                messageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.id!!)

            if (messages.size >= 2) {
                // 通常最新的消息是助手消息，其前一条是用户消息
                val assistantMessage = messages.find { it.role == "assistant" }
                val userMessage =
                    messages.find { it.role == "user" && (assistantMessage?.parentId == it.id) }

                if (assistantMessage != null && userMessage != null) {
                    // 构建对话摘要
                    val summary =
                        ConversationSummary(
                            conversationId = conversation.conversationId,
                            title = conversation.title,
                            modelType = conversation.modelType,
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

            // 如果无法找到用户-助手消息对，则只返回对话的基本信息
            val summary =
                ConversationSummary(
                    conversationId = conversation.conversationId,
                    title = conversation.title,
                    modelType = conversation.modelType,
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
