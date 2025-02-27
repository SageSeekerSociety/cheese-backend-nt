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
    
    // 存储可疑分隔符的缓冲区
    private var suspiciousBuffer = ""

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
            // 缓冲区已被重置，重置我们的处理位置和可疑缓冲区
            lastCheckedLength = 0
            suspiciousBuffer = ""
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

        // 检查缓冲区是否包含完整的分隔符
        if (bufferContent.contains(followupQuestionsDelimiter)) {
            // 找到分隔符在完整缓冲区中的位置
            val delimiterIndex = bufferContent.indexOf(followupQuestionsDelimiter)

            // 如果分隔符出现在新的增量内容中，我们需要只返回分隔符之前的内容
            if (delimiterIndex >= newContentStart) {
                // 先检查是否有之前缓存的可疑内容
                if (suspiciousBuffer.isNotEmpty()) {
                    // 如果缓存的内容是分隔符的一部分，不应该发送它
                    if (followupQuestionsDelimiter.startsWith(suspiciousBuffer)) {
                        suspiciousBuffer = "" // 清空可疑缓冲区
                        val contentBeforeDelimiter = 
                            if (delimiterIndex > suspiciousBuffer.length + newContentStart) {
                                bufferContent.substring(newContentStart + suspiciousBuffer.length, delimiterIndex)
                            } else {
                                ""
                            }
                        logger.debug("检测到完整分隔符，丢弃缓存的可疑内容并返回分隔符之前的内容")
                        return StreamChunkResult(content = contentBeforeDelimiter, shouldSkip = false)
                    } else {
                        // 可疑内容不是分隔符的一部分，可以发送
                        val contentToSend = suspiciousBuffer + 
                            bufferContent.substring(newContentStart, delimiterIndex)
                        suspiciousBuffer = "" // 清空可疑缓冲区
                        logger.debug("检测到完整分隔符，可疑内容不是分隔符的一部分，发送并继续")
                        return StreamChunkResult(content = contentToSend, shouldSkip = false)
                    }
                } else {
                    // 没有可疑内容，正常返回分隔符之前的内容
                    val contentBeforeDelimiter = bufferContent.substring(newContentStart, delimiterIndex)
                    logger.debug("检测到分隔符，返回分隔符之前的内容: $contentBeforeDelimiter")
                    return StreamChunkResult(content = contentBeforeDelimiter, shouldSkip = false)
                }
            } else {
                // 分隔符在之前已经处理过的内容中，应该跳过增量内容
                logger.debug("分隔符已在之前处理过的内容中，跳过增量内容")
                return StreamChunkResult(content = "", shouldSkip = true)
            }
        }

        // 检查缓冲区末尾是否可能是分隔符的开始部分
        // 我们需要检查最多分隔符长度-1个字符
        val maxCheckLength = followupQuestionsDelimiter.length - 1
        
        // 组合之前缓存的可疑内容和新增内容
        val combinedContent = suspiciousBuffer + incrementalContent
        
        // 检查组合内容中是否有分隔符
        if (combinedContent.contains(followupQuestionsDelimiter)) {
            // 存在完整分隔符，只返回分隔符之前的内容
            val delimiterIndex = combinedContent.indexOf(followupQuestionsDelimiter)
            val contentBeforeDelimiter = combinedContent.substring(0, delimiterIndex)
            suspiciousBuffer = "" // 清空可疑缓冲区
            logger.debug("组合内容中检测到分隔符，返回分隔符之前的内容")
            return StreamChunkResult(content = contentBeforeDelimiter, shouldSkip = false)
        }
        
        // 检查是否有可能的分隔符开头
        for (i in 1..maxCheckLength.coerceAtMost(bufferContent.length)) {
            val tailPart = bufferContent.takeLast(i)
            if (followupQuestionsDelimiter.startsWith(tailPart)) {
                // 发现可能的分隔符开始部分
                // 保存除了这部分之外的所有内容
                val safeTailIndex = (bufferContent.length - i).coerceAtLeast(0)
                
                // 如果之前有可疑内容，检查是否和新的可疑内容连续形成分隔符的一部分
                if (suspiciousBuffer.isNotEmpty()) {
                    val potentialDelimiterPart = suspiciousBuffer + tailPart
                    if (followupQuestionsDelimiter.startsWith(potentialDelimiterPart)) {
                        // 组合后仍是分隔符的一部分，继续缓存
                        suspiciousBuffer = potentialDelimiterPart
                        
                        // 返回从新内容开始到安全索引的内容（排除可疑部分）
                        if (safeTailIndex >= newContentStart) {
                            val safeContent = bufferContent.substring(newContentStart, safeTailIndex)
                            logger.debug("检测到连续的分隔符部分，缓存: $potentialDelimiterPart，返回安全内容")
                            return StreamChunkResult(content = safeContent, shouldSkip = false)
                        } else {
                            // 没有安全内容可以返回
                            return StreamChunkResult(content = "", shouldSkip = true)
                        }
                    } else {
                        // 组合后不是分隔符的连续部分，但新尾部仍可能是分隔符的开始
                        // 发送之前的可疑内容（确认不是分隔符的一部分）和安全内容
                        val contentToSend = suspiciousBuffer
                        suspiciousBuffer = tailPart // 更新可疑缓冲区为新的尾部
                        
                        if (safeTailIndex >= newContentStart) {
                            val safeContent = contentToSend + bufferContent.substring(newContentStart, safeTailIndex)
                            logger.debug("之前的可疑内容不是分隔符的一部分，发送它并缓存新的可疑部分: $tailPart")
                            return StreamChunkResult(content = safeContent, shouldSkip = false)
                        } else {
                            // 如果没有新的安全内容，只发送之前的可疑内容
                            logger.debug("没有新的安全内容，只发送之前的可疑内容: $contentToSend")
                            return StreamChunkResult(content = contentToSend, shouldSkip = false)
                        }
                    }
                } else {
                    // 没有之前的可疑内容，将当前尾部缓存为可疑内容
                    suspiciousBuffer = tailPart
                    
                    if (safeTailIndex >= newContentStart) {
                        val safeContent = bufferContent.substring(newContentStart, safeTailIndex)
                        logger.debug("检测到可能的分隔符部分: $tailPart，缓存它并返回安全内容")
                        return StreamChunkResult(content = safeContent, shouldSkip = false)
                    } else {
                        // 没有安全内容可以返回
                        return StreamChunkResult(content = "", shouldSkip = true)
                    }
                }
            }
        }
        
        // 检查当前的可疑缓冲区是否仍可能是分隔符的一部分
        if (suspiciousBuffer.isNotEmpty()) {
            // 检查可疑缓冲区加上新内容是否形成分隔符的一部分
            val potentialDelimiterPart = suspiciousBuffer + incrementalContent
            if (followupQuestionsDelimiter.startsWith(potentialDelimiterPart)) {
                // 仍是分隔符的一部分，继续缓存
                suspiciousBuffer = potentialDelimiterPart
                logger.debug("可疑缓冲区加新内容仍是分隔符的一部分，继续缓存: $potentialDelimiterPart")
                return StreamChunkResult(content = "", shouldSkip = true)
            } else if (potentialDelimiterPart.length >= followupQuestionsDelimiter.length) {
                // 长度超过分隔符但不匹配，可以确定不是分隔符
                val contentToSend = suspiciousBuffer + incrementalContent
                suspiciousBuffer = "" // 清空可疑缓冲区
                logger.debug("确认不是分隔符，发送所有内容: $contentToSend")
                return StreamChunkResult(content = contentToSend, shouldSkip = false)
            } else {
                // 不确定是否是分隔符的一部分，继续缓存
                suspiciousBuffer = potentialDelimiterPart
                logger.debug("不确定是否是分隔符的一部分，继续缓存: $potentialDelimiterPart")
                return StreamChunkResult(content = "", shouldSkip = true)
            }
        }

        // 没有特殊标记也没有可疑内容，返回增量内容
        return StreamChunkResult(content = incrementalContent, shouldSkip = false)
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
        suspiciousBuffer = ""  // 清空可疑缓冲区

        return ProcessedResponse(mainContent = mainContent, metadata = metadata)
    }

    override fun supports(moduleType: String): Boolean {
        return moduleType == "task_ai_advice"
    }
}
