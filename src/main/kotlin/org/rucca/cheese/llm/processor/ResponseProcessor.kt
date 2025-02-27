package org.rucca.cheese.llm.processor

/** LLM响应处理器接口 用于处理不同模块对LLM响应的特定解析需求 */
interface ResponseProcessor {
    /**
     * 处理原始LLM响应
     *
     * @param rawResponse LLM返回的原始响应文本
     * @return 处理后的响应对象
     */
    fun process(rawResponse: String): ProcessedResponse

    /**
     * 处理流式响应片段
     *
     * @param contentChunk 响应内容片段
     * @param currentBuffer 当前累积的响应缓冲
     * @return 处理结果，包含要发送的内容和事件类型
     */
    fun processStreamChunk(contentChunk: String, currentBuffer: StringBuilder): StreamChunkResult

    /**
     * 获取最终的完整响应
     *
     * @param accumulatedResponse 累积的完整响应
     * @return 处理后的完整响应对象
     */
    fun finalizeResponse(accumulatedResponse: String): ProcessedResponse

    /**
     * 判断该处理器是否适用于指定的模块类型
     *
     * @param moduleType 模块类型
     * @return 是否适用
     */
    fun supports(moduleType: String): Boolean
}

/**
 * 处理后的响应
 *
 * @param mainContent 主要内容
 * @param metadata 元数据，包含从响应中提取的额外信息
 */
data class ProcessedResponse(val mainContent: String, val metadata: Map<String, Any> = mapOf())

/**
 * 流式块处理结果
 *
 * @param content 要发送的内容
 * @param eventType 事件类型
 * @param shouldSkip 是否应该跳过发送这个块
 */
data class StreamChunkResult(
    val content: String = "",
    val eventType: String = "PARTIAL",
    val shouldSkip: Boolean = false,
)

/** 默认响应处理器 最简单的实现，不做任何特殊处理，直接返回原始内容 */
class DefaultResponseProcessor : ResponseProcessor {
    // 跟踪上次处理的位置
    private var lastProcessedLength = 0

    override fun process(rawResponse: String): ProcessedResponse {
        return ProcessedResponse(mainContent = rawResponse)
    }

    override fun processStreamChunk(
        contentChunk: String,
        currentBuffer: StringBuilder,
    ): StreamChunkResult {
        // 检测缓冲区是否被重置或清空
        if (currentBuffer.length == 0 && lastProcessedLength > 0) {
            // 缓冲区已被重置，重置我们的处理位置
            lastProcessedLength = 0
        }

        // 先记录当前缓冲区长度
        val originalLength = currentBuffer.length

        // 确保lastProcessedLength不超过当前缓冲区长度
        if (lastProcessedLength > originalLength) {
            lastProcessedLength = originalLength
        }

        // 添加新内容到缓冲区
        currentBuffer.append(contentChunk)

        // 只返回新增的部分，并防止索引越界
        val newContent =
            if (lastProcessedLength < originalLength) {
                // 如果lastProcessedLength小于原始长度，说明有内容没有处理
                // 确保索引不会越界
                val safeStartIndex = lastProcessedLength.coerceAtMost(originalLength)
                val safeEndIndex = originalLength.coerceAtMost(currentBuffer.length)

                if (safeStartIndex < safeEndIndex) {
                    // 先获取未处理的旧内容
                    val oldContent = currentBuffer.substring(safeStartIndex, safeEndIndex)
                    // 再加上新内容
                    oldContent + contentChunk
                } else {
                    // 如果没有未处理的旧内容，只返回新内容
                    contentChunk
                }
            } else {
                // 否则只返回这次新增的内容
                contentChunk
            }

        // 更新处理位置
        lastProcessedLength = currentBuffer.length

        return StreamChunkResult(content = newContent)
    }

    override fun finalizeResponse(accumulatedResponse: String): ProcessedResponse {
        // 重置状态
        lastProcessedLength = 0
        return ProcessedResponse(mainContent = accumulatedResponse)
    }

    override fun supports(moduleType: String): Boolean {
        return true // 作为默认处理器，支持所有模块类型
    }
}

/** 关注点分离的流处理抽象类 可以被特定模块继承并实现自己的处理逻辑 */
abstract class AbstractResponseProcessor : ResponseProcessor {
    // 上次处理的缓冲区长度，用于追踪增量内容
    private var lastProcessedLength = 0

    override fun processStreamChunk(
        contentChunk: String,
        currentBuffer: StringBuilder,
    ): StreamChunkResult {
        // 检测缓冲区是否被重置或清空
        if (currentBuffer.length == 0 && lastProcessedLength > 0) {
            // 缓冲区已被重置，重置我们的处理位置
            lastProcessedLength = 0
        }

        // 先保存当前缓冲区长度，用于确定只返回新增内容
        val originalLength = currentBuffer.length

        // 确保lastProcessedLength不超过当前缓冲区长度
        // 这种情况可能发生在缓冲区被部分清空或重置时
        if (lastProcessedLength > originalLength) {
            lastProcessedLength = originalLength
        }

        // 增加新内容到缓冲区
        currentBuffer.append(contentChunk)
        val bufferContent = currentBuffer.toString()

        // 检查是否有特殊标记需要处理
        val specialMarkerResult = checkSpecialMarkers(bufferContent)
        if (specialMarkerResult != null) {
            // 如果特殊标记处理返回了结果，使用它
            return specialMarkerResult
        }

        // 默认处理：只返回新增的内容
        // 确保我们只发送自上次处理以来的新内容，并防止索引越界
        val newContent =
            if (lastProcessedLength < currentBuffer.length) {
                // 再次确保lastProcessedLength不超过当前缓冲区长度
                val safeStartIndex = lastProcessedLength.coerceAtMost(currentBuffer.length)
                currentBuffer.substring(safeStartIndex)
            } else {
                // 如果lastProcessedLength大于或等于当前缓冲区长度，返回空字符串
                ""
            }

        // 更新处理位置
        lastProcessedLength = currentBuffer.length

        return StreamChunkResult(content = newContent)
    }

    /** 检查特殊标记 子类可以覆盖此方法来实现自己的标记检测逻辑 */
    protected open fun checkSpecialMarkers(bufferContent: String): StreamChunkResult? {
        return null
    }

    /** 从缓冲内容中提取元数据 子类必须实现此方法来解析自己的特殊格式 */
    protected abstract fun extractMetadata(content: String): Map<String, Any>

    override fun finalizeResponse(accumulatedResponse: String): ProcessedResponse {
        // 重置状态
        lastProcessedLength = 0

        val metadata = extractMetadata(accumulatedResponse)
        // 如果有主内容提取逻辑，子类应该覆盖此方法
        return ProcessedResponse(mainContent = accumulatedResponse, metadata = metadata)
    }
}
