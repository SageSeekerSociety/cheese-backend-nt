package org.rucca.cheese.common

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.time.LocalDateTime
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UpdateTimestamp

typealias IdType = Long

/*
 * A base entity that provides common fields for all entities,
 * and enables soft deletion.
 *
 * Provided fields:
 *   - id:        The primary key of the entity.
 *   - createdAt: The timestamp when the entity was created.
 *   - updatedAt: The timestamp when the entity was last updated.
 *   - deletedAt: The timestamp when the entity was deleted.
 */
@SQLDelete(sql = "UPDATE ${'$'}{hbm_dialect.table_name} SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@MappedSuperclass
abstract class BaseEntity(
        // Default value for id, createdAt and updatedAt DO NOT have any effect.
        // They are only set to avoid compilation errors when deriving an entity from BaseEntity.
        @Id @GeneratedValue(strategy = GenerationType.SEQUENCE) val id: IdType = 0,
        @CreationTimestamp val createdAt: LocalDateTime = LocalDateTime.MIN,
        @UpdateTimestamp val updatedAt: LocalDateTime = LocalDateTime.MIN,
        val deletedAt: LocalDateTime? = null,
)