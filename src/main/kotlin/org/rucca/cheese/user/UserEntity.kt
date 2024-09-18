/*
 * DO NOT MODIFY THIS FILE
 *
 * Since cheese-backend is migrating from NestJS to Spring Boot,
 * some modules are still implemented in https://github.com/SageSeekerSociety/cheese-backend
 *
 * However, some tables are shared between the two implementations.
 * This file is one of them.
 *
 * The original project has an independent database schema, so if you modify this file,
 * the original project may not work properly.
 *
 */

/*
 * For the same reason, we recommend you take these tables as read-only,
 * that means, do not do any write operations (INSERT, UPDATE, DELETE) to these tables.
 *
 * We expect these tables to be maintained by the original project,
 * until we decide to fully migrate to Spring Boot.
 *
 */

package org.rucca.cheese.user

import jakarta.persistence.*
import java.time.OffsetDateTime
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@Table(
        name = "\"user\"",
        schema = "public",
        indexes =
                [
                        Index(name = "IDX_78a916df40e02a9deb1c4b75ed", columnList = "username", unique = true),
                        Index(name = "IDX_e12875dfb3b1d92d7d7c5377e2", columnList = "email", unique = true)])
@SQLDelete(sql = "UPDATE ${'$'}{hbm_dialect.table_name} SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
open class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_gen")
    @SequenceGenerator(name = "user_id_gen", sequenceName = "user_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    open var id: Int? = null

    @Column(name = "username", nullable = false, length = Integer.MAX_VALUE) open var username: String? = null

    @Column(name = "hashed_password", nullable = false, length = Integer.MAX_VALUE)
    open var hashedPassword: String? = null

    @Column(name = "email", nullable = false, length = Integer.MAX_VALUE) open var email: String? = null

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false, insertable = false)
    open var createdAt: OffsetDateTime? = null

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false, insertable = false)
    open var updatedAt: OffsetDateTime? = null

    @Column(name = "deleted_at") open var deletedAt: OffsetDateTime? = null
}

interface UserRepository : JpaRepository<User, Int>
