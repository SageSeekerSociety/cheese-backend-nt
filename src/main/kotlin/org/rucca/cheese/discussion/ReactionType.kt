package org.rucca.cheese.discussion

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@DynamicUpdate
@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    name = "reaction_type",
    indexes = [Index(name = "idx_reaction_type_code", columnList = "code", unique = true)],
)
class ReactionType(
    @Column(nullable = false, length = 32) var code: String? = null,
    @Column(nullable = false, length = 64) var name: String? = null,
    @Column(length = 255) var description: String? = null,
    @Column(nullable = false) var displayOrder: Int = 0,
    @Column(nullable = false) var isActive: Boolean = true,
) : BaseEntity()

@Repository
interface ReactionTypeRepository : JpaRepository<ReactionType, IdType> {
    fun findByCode(code: String): ReactionType?

    fun findAllByIsActiveTrueOrderByDisplayOrderAsc(): List<ReactionType>

    fun existsByCode(code: String): Boolean
}
