package org.rucca.cheese.user.models

import jakarta.persistence.*
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository

@Embeddable
class UserRealNameInfo(
    @Column(name = "real_name", nullable = false) val realName: String,
    @Column(name = "student_id", nullable = false) val studentId: String,
    @Column(name = "grade", nullable = false) val grade: String,
    @Column(name = "major", nullable = false) val major: String,
    @Column(name = "class_name", nullable = false) val className: String,
    @Column(name = "encrypted", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    val encrypted: Boolean = false,
)

@Entity
@Table(name = "user_real_name_identities")
class UserRealNameIdentity(
    @Column(nullable = false) val userId: IdType,
    @Embedded var realNameInfo: UserRealNameInfo,
    @Column(nullable = false) var encryptionKeyId: String,
) : BaseEntity() {
    fun updateInfo(newInfo: UserRealNameInfo, newKeyId: String) {
        this.realNameInfo = newInfo
        this.encryptionKeyId = newKeyId
    }
}

interface UserRealNameIdentityRepository : JpaRepository<UserRealNameIdentity, IdType> {
    fun findByUserId(userId: IdType): UserRealNameIdentity?
}
