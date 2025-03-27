package org.rucca.cheese.user.models

import jakarta.persistence.*
import java.time.OffsetDateTime
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository

enum class KeyPurpose {
    USER_REAL_NAME,
    TASK_REAL_NAME,
}

@Entity
@Table(name = "encryption_keys")
class EncryptionKey(
    @Id val id: String,
    @Column(nullable = false) val keyValue: String,
    @Column(nullable = false) val purpose: KeyPurpose,
    @Column(nullable = true) val relatedEntityId: IdType? = null,
    @Column(nullable = false) val createdAt: OffsetDateTime = OffsetDateTime.now(),
)

interface EncryptionKeyRepository : JpaRepository<EncryptionKey, String> {
    fun findByPurposeAndRelatedEntityId(
        purpose: KeyPurpose,
        relatedEntityId: IdType,
    ): EncryptionKey?
}
