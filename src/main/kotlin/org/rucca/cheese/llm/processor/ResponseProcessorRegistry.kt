package org.rucca.cheese.llm.processor

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/** 响应处理器注册表 管理系统中所有的响应处理器，并提供根据模块类型获取处理器的方法 */
@Component
class ResponseProcessorRegistry(processors: List<ResponseProcessor>) {
    private val logger = LoggerFactory.getLogger(ResponseProcessorRegistry::class.java)
    private val defaultProcessor = DefaultResponseProcessor()
    private final val processorMap: Map<String, ResponseProcessor>

    init {
        // 根据处理器支持的模块类型进行分组
        val tempMap = mutableMapOf<String, ResponseProcessor>()
        processors.forEach { processor ->
            // 使用反射获取处理器支持的模块类型
            val processorName = processor.javaClass.simpleName
            try {
                // 使用supports方法找出所有支持的模块
                for (moduleType in getAllModuleTypes()) {
                    if (processor.supports(moduleType)) {
                        logger.info("注册响应处理器: $processorName 用于模块: $moduleType")
                        tempMap[moduleType] = processor
                    }
                }
            } catch (e: Exception) {
                logger.error("注册处理器 $processorName 时出错", e)
            }
        }
        processorMap = tempMap.toMap()
        logger.info("响应处理器注册完成，共 ${processorMap.size} 个模块")
    }

    /** 获取支持指定模块类型的处理器 如果没有找到匹配的处理器，返回默认处理器 */
    fun getProcessor(moduleType: String): ResponseProcessor {
        return processorMap[moduleType] ?: defaultProcessor
    }

    /** 获取系统中所有已知的模块类型 这个方法可以从配置、枚举或其他方式获取所有已知的模块类型 */
    private fun getAllModuleTypes(): List<String> {
        // 这里可以从配置或其他地方动态获取
        // 暂时硬编码一些已知的模块类型
        return listOf(
            "task_ai_advice",
        )
    }
}
