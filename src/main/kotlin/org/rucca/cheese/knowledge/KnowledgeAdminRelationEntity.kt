package org.rucca.cheese.knowledge

import jakarta.persistence.*
import java.util.Optional
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.JpaRepository

enum class KnowledgeAdminRole {
    OWNER,
    ADMIN,
}

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "knowledge_id"), Index(columnList = "user_id")])
class KnowledgeAdminRelation(
    @JoinColumn(nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val knowledge: Knowledge? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val user: User? = null,
    @Column(nullable = false) var role: KnowledgeAdminRole? = null,
) : BaseEntity()

interface KnowledgeAdminRelationRepository : JpaRepository<KnowledgeAdminRelation, IdType> {
    fun findAllByKnowledgeId(knowledgeId: IdType): List<KnowledgeAdminRelation>

    fun findByKnowledgeIdAndRole(
        knowledgeId: IdType,
        role: KnowledgeAdminRole,
    ): Optional<KnowledgeAdminRelation>

    fun findByKnowledgeIdAndUserId(
        knowledgeId: IdType,
        userId: IdType,
    ): Optional<KnowledgeAdminRelation>

    fun existsByKnowledgeIdAndUserId(knowledgeId: IdType, userId: IdType): Boolean

    fun existsByKnowledgeIdAndUserIdAndRole(
        knowledgeId: IdType,
        userId: IdType,
        role: KnowledgeAdminRole,
    ): Boolean
}
