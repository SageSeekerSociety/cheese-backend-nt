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
            // 缓冲区已被重置，重置我们的处理位置
            lastCheckedLength = 0
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

        // 检查缓冲区是否包含分隔符
        if (bufferContent.contains(followupQuestionsDelimiter)) {
            // 找到分隔符在完整缓冲区中的位置
            val delimiterIndex = bufferContent.indexOf(followupQuestionsDelimiter)

            // 如果分隔符出现在新的增量内容中，我们需要只返回分隔符之前的内容
            if (delimiterIndex >= newContentStart) {
                val contentBeforeDelimiter =
                    bufferContent.substring(newContentStart, delimiterIndex)
                logger.debug("检测到分隔符，返回分隔符之前的内容: $contentBeforeDelimiter")
                return StreamChunkResult(content = contentBeforeDelimiter, shouldSkip = false)
            } else {
                // 分隔符在之前已经处理过的内容中，应该跳过增量内容
                logger.debug("分隔符已在之前处理过的内容中，跳过增量内容")
                return StreamChunkResult(content = "", shouldSkip = true)
            }
        }

        // 检查是否正在处理分隔符的一部分（防止分隔符被切分）
        val maxCheckLength = followupQuestionsDelimiter.length - 1
        for (i in 1..maxCheckLength.coerceAtMost(bufferContent.length)) {
            val tailPart = bufferContent.takeLast(i)
            if (followupQuestionsDelimiter.startsWith(tailPart)) {
                // 正在处理分隔符的开始部分，暂时跳过这部分尾部
                val safeTailIndex = (bufferContent.length - i).coerceAtLeast(0)
                if (safeTailIndex >= newContentStart) {
                    val safeContent = bufferContent.substring(newContentStart, safeTailIndex)
                    logger.debug("检测到可能的分隔符部分: $tailPart，返回安全内容: $safeContent")
                    return StreamChunkResult(content = safeContent, shouldSkip = false)
                } else {
                    // 分隔符开始部分在前面已处理内容中，应跳过
                    return StreamChunkResult(content = "", shouldSkip = true)
                }
            }
        }

        // 没有特殊标记，让基类处理增量内容
        return null
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

        return ProcessedResponse(mainContent = mainContent, metadata = metadata)
    }

    override fun supports(moduleType: String): Boolean {
        return moduleType == "task_ai_advice"
    }
}
