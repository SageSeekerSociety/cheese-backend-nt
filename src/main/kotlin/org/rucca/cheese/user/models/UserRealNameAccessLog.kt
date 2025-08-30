package org.rucca.cheese.user.models

import jakarta.persistence.*
import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

enum class AccessType {
    VIEW,
    EXPORT,
}

enum class AccessModuleType {
    TASK
}

@Entity
@Table(name = "user_real_name_access_logs")
class UserRealNameAccessLog(
    @Column(nullable = false) val accessorId: IdType,
    @Column(nullable = false) val targetId: IdType,
    @Enumerated(EnumType.STRING) @Column(nullable = true) val moduleType: AccessModuleType? = null,
    @Column(nullable = true) val moduleEntityId: IdType? = null,
    @Column(nullable = false) val accessReason: String,
    @Column(nullable = false) val ipAddress: String,
    @Enumerated(EnumType.STRING) @Column(nullable = false) val accessType: AccessType,
) : BaseEntity()

interface UserRealNameAccessLogRepository : CursorPagingRepository<UserRealNameAccessLog, IdType> {
    fun findAllByTargetIdOrderByCreatedAtDesc(
        targetId: IdType,
        pageable: Pageable,
    ): Page<UserRealNameAccessLog>
}
