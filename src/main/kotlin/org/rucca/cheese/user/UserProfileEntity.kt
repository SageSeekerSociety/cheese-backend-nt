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
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@Table(name = "avatar")
open class Avatar {
    @Id @ColumnDefault("nextval('avatar_id_seq')") @Column(name = "id", nullable = false) open var id: Int? = null
}

@Entity
@Table(
        name = "user_profile",
        schema = "public",
        indexes = [Index(name = "IDX_51cb79b5555effaf7d69ba1cff", columnList = "id", unique = true)])
open class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_profile_id_gen")
    @SequenceGenerator(name = "user_profile_id_gen", sequenceName = "user_profile_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    open var id: Int? = null

    @Column(name = "nickname", nullable = false, length = Integer.MAX_VALUE) open var nickname: String? = null

    @Column(name = "intro", nullable = false, length = Integer.MAX_VALUE) open var intro: String? = null

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false, insertable = false)
    open var createdAt: OffsetDateTime? = null

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false, insertable = false)
    open var updatedAt: OffsetDateTime? = null

    @Column(name = "deleted_at") open var deletedAt: OffsetDateTime? = null

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    open var userEntity: UserEntity? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "avatar_id", nullable = false)
    open var avatar: Avatar? = null
}

interface UserProfileRepository : JpaRepository<UserProfile, Int>