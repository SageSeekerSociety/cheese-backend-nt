package org.rucca.cheese.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.rucca.cheese.llm.processor.AbstractResponseProcessor
import org.rucca.cheese.llm.processor.ProcessedResponse
import org.rucca.cheese.llm.processor.StreamChunkResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/** 任务AI建议响应处理器 专门处理任务AI建议模块的响应格式，包括后续问题的提取等 */
@Component
class TaskAIAdviceResponseProcessor(private val objectMapper: ObjectMapper) :
    AbstractResponseProcessor() {
    private val logger = LoggerFactory.getLogger(TaskAIAdviceResponseProcessor::class.java)

    // 定义特殊的分隔符
    private val followupQuestionsDelimiter = "===FOLLOWUP_QUESTIONS==="

    // 记录上次检查的缓冲区长度
    private var lastCheckedLength = 0
    
    // 存储待处理的内容，包括可能需要确认是否为分隔符的内容
    private var pendingBuffer = StringBuilder()
    
    // 标记是否已经遇到了分隔符，如果为true，则后续所有内容都不应该发送给前端
    private var foundDelimiter = false

    override fun process(rawResponse: String): ProcessedResponse {
        // 处理非流式响应
        val parts = rawResponse.split(followupQuestionsDelimiter)
        val mainContent = parts[0].trim()

        val metadata = mutableMapOf<String, Any>()
        if (parts.size > 1) {
            try {
                val followupQuestionsJson = parts[1].trim()
                val followupQuestions = objectMapper.readValue<List<String>>(followupQuestionsJson)
                metadata["followupQuestions"] = followupQuestions
            } catch (e: Exception) {
                logger.error("解析后续问题失败: ${parts.getOrNull(1)}", e)
                metadata["followupQuestions"] = emptyList<String>()
            }
        } else {
            metadata["followupQuestions"] = emptyList<String>()
        }

        return ProcessedResponse(mainContent = mainContent, metadata = metadata)
    }

    override fun checkSpecialMarkers(bufferContent: String): StreamChunkResult? {
        // 检测缓冲区是否被重置或清空
        if (bufferContent.isEmpty() && lastCheckedLength > 0) {
            // 缓冲区已被重置，重置我们的处理位置和待处理缓冲区
            lastCheckedLength = 0
            pendingBuffer.clear()
            foundDelimiter = false
            return StreamChunkResult(content = "", shouldSkip = true)
        }

        // 确保lastCheckedLength不超过当前缓冲区长度
        if (lastCheckedLength > bufferContent.length) {
            lastCheckedLength = bufferContent.length
        }

        // 计算增量内容 - 只处理新增加的部分，并防止索引越界
        val newContentStart = lastCheckedLength.coerceAtMost(bufferContent.length)
        val incrementalContent =
            if (newContentStart < bufferContent.length) {
                bufferContent.substring(newContentStart)
            } else {
                ""
            }

        // 更新检查点位置
        lastCheckedLength = bufferContent.length

        // 如果没有增量内容，不需要进一步处理
        if (incrementalContent.isEmpty()) {
            return StreamChunkResult(content = "", shouldSkip = true)
        }
        
        // 如果已经找到了分隔符，所有后续内容都不应该发送
        if (foundDelimiter) {
            logger.debug("已发现分隔符，跳过后续内容: $incrementalContent")
            return StreamChunkResult(content = "", shouldSkip = true)
        }
        
        // 添加新增内容到待处理缓冲区
        pendingBuffer.append(incrementalContent)
        val pendingContent = pendingBuffer.toString()
        
        // 1. 检查待处理缓冲区中是否包含完整分隔符
        if (pendingContent.contains(followupQuestionsDelimiter)) {
            val delimiterIndex = pendingContent.indexOf(followupQuestionsDelimiter)
            // 只发送分隔符之前的安全内容
            val safeContent = pendingContent.substring(0, delimiterIndex)
            
            // 标记已找到分隔符，后续内容不应发送
            foundDelimiter = true
            
            // 保留分隔符后的内容在待处理缓冲区中
            pendingBuffer.delete(0, delimiterIndex + followupQuestionsDelimiter.length)
            
            logger.debug("检测到完整分隔符，发送分隔符之前的内容: $safeContent")
            logger.debug("保留分隔符后的内容: ${pendingBuffer.toString()}")
            
            return StreamChunkResult(content = safeContent, shouldSkip = false)
        }
        
        // 2. 检查待处理缓冲区末尾是否可能是分隔符的一部分
        var maxSafeLength = pendingContent.length
        
        // 从最长的可能分隔符部分开始检查
        for (i in followupQuestionsDelimiter.length - 1 downTo 1) {
            if (pendingContent.length < i) {
                continue // 内容长度不足以匹配这个长度
            }
            
            val tailPart = pendingContent.takeLast(i)
            if (followupQuestionsDelimiter.startsWith(tailPart)) {
                // 如果尾部与分隔符开头匹配，将这部分标记为不安全
                maxSafeLength = pendingContent.length - i
                logger.debug("检测到可能的分隔符部分: $tailPart，设置安全长度为: $maxSafeLength")
                break
            }
        }
        
        // 如果整个待处理内容可能都是分隔符的一部分，不发送任何内容
        if (maxSafeLength <= 0) {
            logger.debug("所有待处理内容可能都是分隔符的一部分，不发送任何内容")
            return StreamChunkResult(content = "", shouldSkip = true)
        }
        
        // 只发送安全部分，并从待处理缓冲区中移除
        val safeContent = pendingContent.substring(0, maxSafeLength)
        pendingBuffer.delete(0, maxSafeLength)
        logger.debug("发送安全内容: $safeContent，保留待处理内容: ${pendingBuffer.toString()}")
        return StreamChunkResult(content = safeContent, shouldSkip = false)
    }

    override fun extractMetadata(content: String): Map<String, Any> {
        val parts = content.split(followupQuestionsDelimiter)

        return if (parts.size > 1) {
            try {
                val followupQuestionsJson = parts[1].trim()
                val followupQuestions = objectMapper.readValue<List<String>>(followupQuestionsJson)
                mapOf("followupQuestions" to followupQuestions)
            } catch (e: Exception) {
                logger.error("解析后续问题失败: ${parts.getOrNull(1)}", e)
                mapOf("followupQuestions" to emptyList<String>())
            }
        } else {
            mapOf("followupQuestions" to emptyList<String>())
        }
    }

    override fun finalizeResponse(accumulatedResponse: String): ProcessedResponse {
        val parts = accumulatedResponse.split(followupQuestionsDelimiter)
        val mainContent = parts[0].trim()
        val metadata = extractMetadata(accumulatedResponse)

        // 重置状态
        lastCheckedLength = 0
        pendingBuffer.clear()
        foundDelimiter = false

        return ProcessedResponse(mainContent = mainContent, metadata = metadata)
    }

    override fun supports(moduleType: String): Boolean {
        return moduleType == "task_ai_advice"
    }
}
