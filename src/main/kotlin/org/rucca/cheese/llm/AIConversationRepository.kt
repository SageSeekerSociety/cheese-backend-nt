package org.rucca.cheese.llm

import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AIConversationRepository : JpaRepository<AIConversationEntity, IdType> {
    /** 按会话ID查找对话 */
    fun findByConversationId(conversationId: String): AIConversationEntity?

    /** 按模块类型和上下文ID查找对话 */
    fun findByModuleTypeAndContextId(
        moduleType: String,
        contextId: IdType,
    ): List<AIConversationEntity>

    /** 按所有者ID查找对话，根据创建时间降序排序 */
    fun findByOwnerIdOrderByCreatedAtDesc(ownerId: IdType): List<AIConversationEntity>

    /** 按模块类型和所有者ID查找对话，根据创建时间降序排序 */
    fun findByModuleTypeAndOwnerIdOrderByCreatedAtDesc(
        moduleType: String,
        ownerId: IdType,
    ): List<AIConversationEntity>

    /** 按模块类型和上下文ID列表查找对话 */
    fun findByModuleTypeAndContextIdIn(
        moduleType: String,
        contextIds: List<IdType>,
    ): List<AIConversationEntity>
}
