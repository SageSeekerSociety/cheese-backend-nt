package org.rucca.cheese.llm

import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AIConversationRepository : JpaRepository<AIConversationEntity, IdType> {
    /** 按会话ID查找对话 */
    fun findByConversationId(conversationId: String): AIConversationEntity?

    fun findByOwnerIdAndConversationId(
        ownerId: IdType,
        conversationId: String,
    ): AIConversationEntity?

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

    fun findByModuleTypeAndContextIdInAndOwnerId(
        moduleType: String,
        contextIds: List<IdType>,
        ownerId: IdType,
    ): List<AIConversationEntity>

    fun existsByConversationIdAndOwnerId(conversationId: String, ownerId: IdType): Boolean

    @Modifying
    @Query("UPDATE AIConversationEntity SET title = :title WHERE id = :id AND ownerId = :ownerId")
    fun updateTitleByIdAndOwnerId(id: IdType, ownerId: IdType, title: String): Int
}
