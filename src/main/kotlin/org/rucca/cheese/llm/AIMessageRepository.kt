package org.rucca.cheese.llm

import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AIMessageRepository : JpaRepository<AIMessageEntity, IdType> {
    /** 按对话ID查找消息，根据创建时间升序排序 */
    fun findByConversationIdOrderByCreatedAtAsc(conversationId: IdType): List<AIMessageEntity>

    /** 按对话ID查找消息，根据创建时间降序排序 */
    fun findByConversationIdOrderByCreatedAtDesc(conversationId: IdType): List<AIMessageEntity>

    /** 按角色和对话ID查找消息 */
    fun findByRoleAndConversationIdOrderByCreatedAtAsc(
        role: String,
        conversationId: IdType,
    ): List<AIMessageEntity>

    /** 按父消息ID查找消息 */
    fun findByParentIdOrderByCreatedAtAsc(parentId: IdType): List<AIMessageEntity>

    /** 查找指定对话ID的最新用户消息和助手回复对 */
    @Query(
        """
        SELECT m FROM AIMessageEntity m
        WHERE m.conversation.id = :conversationId
        AND m.id IN (
            SELECT MAX(m2.id) FROM AIMessageEntity m2
            WHERE m2.conversation.id = :conversationId
            GROUP BY m2.role
        )
        ORDER BY m.createdAt DESC
    """
    )
    fun findLatestMessagePairByConversationId(conversationId: IdType): List<AIMessageEntity>

    /** 统计指定对话的消息数量 */
    fun countByConversationId(conversationId: IdType): Long
}
