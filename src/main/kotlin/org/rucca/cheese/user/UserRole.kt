package org.rucca.cheese.user

import jakarta.persistence.*
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository

enum class UserRole {
    SUPER_ADMIN,
    ADMIN,
    MODERATOR,
    USER,
}

@Entity
@Table(
    name = "user_role",
    indexes = [Index(columnList = "user_id")],
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "role"])],
)
class UserRoleEntity(
    @Column(name = "user_id", nullable = false) val userId: IdType,
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    val role: UserRole = UserRole.USER,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: IdType? = null
}

interface UserRoleRepository : JpaRepository<UserRoleEntity, IdType> {
    fun findAllByUserId(userId: Long): Set<UserRoleEntity>

    fun existsByUserIdAndRole(userId: IdType, role: UserRole): Boolean
}
