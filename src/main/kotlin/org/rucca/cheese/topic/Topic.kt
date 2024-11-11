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

package org.rucca.cheese.topic

import jakarta.persistence.*
import java.time.OffsetDateTime
import org.hibernate.annotations.ColumnDefault
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@Table(name = "topic")
open class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "topic_id_gen")
    @SequenceGenerator(name = "topic_id_gen", sequenceName = "topic_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    open var id: Int? = null

    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
    open var name: String? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    open var createdBy: User? = null

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    open var createdAt: OffsetDateTime? = null

    @Column(name = "deleted_at") open var deletedAt: OffsetDateTime? = null
}

interface TopicRepository : JpaRepository<Topic, Int>
