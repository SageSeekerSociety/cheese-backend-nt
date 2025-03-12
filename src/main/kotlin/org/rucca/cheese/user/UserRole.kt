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
    indexes = [Index(columnList = "userId")],
    uniqueConstraints = [UniqueConstraint(columnNames = ["userId", "role"])],
)
class UserRoleEntity(
    @Column(name = "userId", nullable = false) val userId: IdType,
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
}
