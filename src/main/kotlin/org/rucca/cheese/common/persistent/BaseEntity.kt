package org.rucca.cheese.common.persistent

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.time.LocalDateTime
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp

typealias IdType = Long

typealias IdGetter = () -> IdType

/*
 * A base entity that provides common fields for all entities,
 * and enables soft deletion.
 *
 * Provided fields:
 *   - id:        The primary key of the entity.
 *   - createdAt: The timestamp when the entity was created.
 *   - updatedAt: The timestamp when the entity was last updated.
 *   - deletedAt: The timestamp when the entity was deleted.
 *
 * Add the following lines to your entities to implement soft deletion:
 *      @SQLDelete(sql = "UPDATE ${'$'}{hbm_dialect.table_name} SET deleted_at = current_timestamp WHERE id = ?")
 *      @SQLRestriction("deleted_at IS NULL")
 * Unfortunately, adding these lines to BaseEntity does not work.
 */
@MappedSuperclass
abstract class BaseEntity(
        // Default value for id, createdAt and updatedAt DO NOT have any effect.
        // They are only set to avoid compilation errors when deriving an entity from BaseEntity.
        @Id @GeneratedValue(strategy = GenerationType.SEQUENCE) var id: IdType = 0,
        @CreationTimestamp val createdAt: LocalDateTime = LocalDateTime.MIN,
        @UpdateTimestamp val updatedAt: LocalDateTime = LocalDateTime.MIN,
        val deletedAt: LocalDateTime? = null,
)
