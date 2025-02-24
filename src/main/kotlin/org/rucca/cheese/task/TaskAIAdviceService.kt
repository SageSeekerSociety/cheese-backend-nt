package org.rucca.cheese.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.security.MessageDigest
import kotlinx.coroutines.*
import org.rucca.cheese.auth.CustomAuthLogics
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.RichTextHelper
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.llm.config.LLMProperties
import org.rucca.cheese.llm.error.LLMError.*
import org.rucca.cheese.llm.service.LLMService
import org.rucca.cheese.llm.service.UserAIQuotaService
import org.rucca.cheese.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskAIAdviceService(
    private val taskService: TaskService,
    private val taskAIAdviceRepository: TaskAIAdviceRepository,
    private val objectMapper: ObjectMapper,
    private val llmService: LLMService,
    private val properties: LLMProperties,
    private val userAIQuotaService: UserAIQuotaService,
) {
    private val logger = LoggerFactory.getLogger(CustomAuthLogics::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Autowired private lateinit var applicationContext: ApplicationContext

    @Service
    class TransactionalService(
        private val taskAIAdviceRepository: TaskAIAdviceRepository,
        private val objectMapper: ObjectMapper,
    ) {
        @Transactional
        fun updateAdviceStatus(taskId: IdType, modelHash: String, status: TaskAIAdviceStatus) {
            val advice = taskAIAdviceRepository.findByTaskIdAndModelHash(taskId, modelHash).get()
            advice.status = status
            taskAIAdviceRepository.save(advice)
        }

        @Transactional
        fun saveAdviceResponse(
            taskId: IdType,
            modelHash: String,
            llmResponse: String,
            dto: TaskAIAdviceDTO,
        ) {
            val advice = taskAIAdviceRepository.findByTaskIdAndModelHash(taskId, modelHash).get()
            advice.apply {
                this.rawResponse = llmResponse
                this.knowledgeFields = objectMapper.writeValueAsString(dto.knowledgeFields)
                this.learningPaths = objectMapper.writeValueAsString(dto.learningPaths)
                this.methodology = objectMapper.writeValueAsString(dto.methodology)
                this.teamTips = objectMapper.writeValueAsString(dto.teamTips)
                this.status = TaskAIAdviceStatus.COMPLETED
            }
            taskAIAdviceRepository.save(advice)
        }
    }

    fun requestTaskAIAdvice(taskId: IdType, userId: IdType): RequestTaskAiAdvice200ResponseDataDTO {
        // 在协程外获取所有需要的信息
        val task = taskService.getTaskDto(taskId)
        val modelHash = calculateModelHash(task)
        val prompt = buildPrompt(task)

        // 检查是否存在缓存
        val cachedAdvice = taskAIAdviceRepository.findByTaskIdAndModelHash(taskId, modelHash)
        if (cachedAdvice.isPresent) {
            // 如果之前的请求失败了，重新尝试
            if (cachedAdvice.get().status == TaskAIAdviceStatus.FAILED) {
                cachedAdvice.get().status = TaskAIAdviceStatus.PENDING
                taskAIAdviceRepository.save(cachedAdvice.get())

                // 启动异步处理
                coroutineScope.launch { processTaskAIAdvice(taskId, userId, modelHash, prompt) }
            }
            return RequestTaskAiAdvice200ResponseDataDTO(
                status =
                    RequestTaskAiAdvice200ResponseDataDTO.Status.valueOf(
                        cachedAdvice.get().status.toString()
                    ),
                quota = getQuotaInfo(userId),
            )
        }

        // 创建新的请求记录
        val advice =
            TaskAIAdvice().apply {
                this.taskId = taskId
                this.modelHash = modelHash
                this.status = TaskAIAdviceStatus.PENDING
            }

        taskAIAdviceRepository.save(advice)

        // 启动异步处理
        coroutineScope.launch { processTaskAIAdvice(taskId, userId, modelHash, prompt) }

        return RequestTaskAiAdvice200ResponseDataDTO(
            status = RequestTaskAiAdvice200ResponseDataDTO.Status.PENDING,
            quota = getQuotaInfo(userId),
        )
    }

    fun getTaskAIAdviceStatus(
        taskId: IdType,
        userId: IdType,
    ): RequestTaskAiAdvice200ResponseDataDTO {
        val task = taskService.getTaskDto(taskId)
        val modelHash = calculateModelHash(task)
        val advice =
            taskAIAdviceRepository.findByTaskIdAndModelHash(taskId, modelHash).orElseThrow {
                NotFoundError("task/ai-advice", taskId)
            }
        return RequestTaskAiAdvice200ResponseDataDTO(
            status = RequestTaskAiAdvice200ResponseDataDTO.Status.valueOf(advice.status.toString()),
            quota = getQuotaInfo(userId),
        )
    }

    fun getTaskAIAdvice(taskId: IdType): TaskAIAdviceDTO {
        val task = taskService.getTaskDto(taskId)
        val modelHash = calculateModelHash(task)
        val advice =
            taskAIAdviceRepository.findByTaskIdAndModelHash(taskId, modelHash).orElseThrow {
                NotFoundError("task/ai-advice", taskId)
            }

        if (advice.status != TaskAIAdviceStatus.COMPLETED) {
            throw AdviceNotReadyError(advice.status.toString())
        }

        return objectMapper.readValue(extractJsonFromResponse(advice.rawResponse!!))
    }

    private fun getQuotaInfo(userId: IdType): QuotaInfoDTO {
        val quota = userAIQuotaService.getUserQuota(userId)
        return QuotaInfoDTO(
            remaining = quota.remainingQuota,
            total = quota.dailyQuota,
            resetTime = userAIQuotaService.getUserResetTime(),
        )
    }

    suspend fun processTaskAIAdvice(
        taskId: IdType,
        userId: IdType,
        modelHash: String,
        prompt: String,
    ) {
        val transactionalService = applicationContext.getBean(TransactionalService::class.java)

        try {
            // 更新状态为处理中
            transactionalService.updateAdviceStatus(
                taskId,
                modelHash,
                TaskAIAdviceStatus.PROCESSING,
            )

            // 检查并扣减配额
            if (!userAIQuotaService.checkAndDeductQuota(userId)) {
                transactionalService.updateAdviceStatus(
                    taskId,
                    modelHash,
                    TaskAIAdviceStatus.FAILED,
                )
                throw QuotaExceededError()
            }

            // 调用LLM服务
            val systemPrompt = ""
            val llmResponse = llmService.getCompletion(prompt, systemPrompt, true).trim()

            // 提取JSON内容
            val jsonResponse = extractJsonFromResponse(llmResponse)
            logger.info("Task $taskId AI advice generated successfully")

            // 解析响应并保存结果
            val dto: TaskAIAdviceDTO = objectMapper.readValue(jsonResponse)

            // 使用事务服务保存响应
            transactionalService.saveAdviceResponse(taskId, modelHash, llmResponse, dto)
        } catch (e: Exception) {
            logger.error("Failed to generate AI advice for task $taskId", e)
            transactionalService.updateAdviceStatus(taskId, modelHash, TaskAIAdviceStatus.FAILED)
            throw ServiceError()
        }
    }

    private fun calculateModelHash(task: TaskDTO): String {
        val content = buildPrompt(task) + properties.model.name
        return MessageDigest.getInstance("SHA-256").digest(content.toByteArray()).fold("") { str, it
            ->
            str + "%02x".format(it)
        }
    }

    private fun extractJsonFromResponse(response: String): String {
        // 移除可能存在的markdown代码块标记
        val jsonPattern = """(?:```json\n)?(\{[\s\S]*\})(?:\n```)?""".toRegex()
        return jsonPattern.find(response)?.groupValues?.get(1)?.trim()
            ?: throw InvalidResponseError()
    }

    private fun buildPrompt(task: TaskDTO): String {
        // 将富文本转换为Markdown
        val description = RichTextHelper.toMarkdown(task.description)

        return """
请根据以下科研课题信息，为学生提供详细而具体的建议。生成的内容必须仅基于已有事实、常识和可靠的知识，不允许凭空编造或虚构内容。如果对于某些信息无法确定，请标记为"未知"或留空。请严格输出有效的 JSON 数据，不包含任何额外说明或文字，仅返回 JSON 格式内容。

【输入信息】
- 课题标题: ${task.name}
- 详细介绍: 
${description}
- 参与方式: ${if (task.submitterType == TaskSubmitterTypeDTO.TEAM) "团队" else "个人"}

【输出要求】
请根据以上信息生成如下结构的建议。请确保生成的每一项建议都经过谨慎推理，仅基于可靠、已有的信息，不进行无依据的扩展和猜测。

1. **knowledge_fields**（知识领域解读）
   - 类型：数组，每个元素为对象，描述课题涉及的主要知识领域及其背景。  
   - 每个对象包含：
     - "name": 字符串，表示知识领域名称（例如："人工智能"、"统计学"）。
     - "description": 字符串，详细解释该知识领域在本课题中的作用及核心概念，内容必须基于可靠常识描述。

2. **learning_paths**（推荐学习路径）
   - 类型：数组，每个元素为对象，描述分阶段的学习建议。  
   - 每个对象包含：
     - "stage": 字符串，描述学习阶段（例如："基础入门"、"进阶"、"高级"）。
     - "description": 字符串，详细说明该阶段应掌握的知识点或技能，建议内容必须基于实际可验证的知识。
     - "resources": 数组，推荐的学习资源，每个资源为对象，字段包括：
       - "name": 字符串，资源名称（如书籍、在线课程、论文）。
       - "type": 字符串，资源类型（可选项：video/article/course/github/paper/website）。
       - "url": 字符串，可选，提供真实存在的资源链接；如无法确认则留空。

3. **methodology**（研究方法规划）
   - 类型：数组，每个元素为对象，描述完成课题的各个研究步骤。  
   - 每个对象包含：
     - "step": 字符串，描述具体步骤或阶段（例如："实验设计"、"数据采集"、"模型构建"）。
     - "description": 字符串，详细说明该步骤的执行方法、关键点及注意事项，必须基于公认的科研方法。
     - "estimated_time": 字符串，可选，建议完成该步骤的时间预估，如不确定则标记"未知"。

4. **team_tips**（团队协作建议）
   - 类型：数组，每个元素为对象（如果课题为个人参与，可返回空数组）。  
   - 每个对象包含：
     - "role": 字符串，建议的团队角色（例如："数据分析师"、"开发工程师"）。
     - "description": 字符串，详细描述该角色在课题中的主要任务与责任，要求内容可靠、常见。
     - "collaboration_tips": 字符串，提供团队协作时的建议或注意事项，必须基于实际团队协作经验，不进行虚构。
        """
            .trimIndent()
    }
}
