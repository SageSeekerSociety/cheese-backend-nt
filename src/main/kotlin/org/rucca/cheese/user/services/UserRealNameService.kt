package org.rucca.cheese.user.services

import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.pagination.model.SimpleCursor
import org.rucca.cheese.common.pagination.model.toPageDTO
import org.rucca.cheese.common.pagination.spec.simpleCursorSpec
import org.rucca.cheese.common.pagination.util.desc
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.getProperty
import org.rucca.cheese.model.*
import org.rucca.cheese.task.TaskInfoProvider
import org.rucca.cheese.user.models.*
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Service
class UserRealNameService(
    private val userRealNameIdentityRepository: UserRealNameIdentityRepository,
    private val userRealNameAccessLogRepository: UserRealNameAccessLogRepository,
    private val encryptionService: EncryptionService,
    private val userService: UserService,
    private val taskInfoProvider: TaskInfoProvider,
) {
    /** Get user real name identity information */
    @Transactional
    fun getUserIdentity(userId: IdType): UserIdentityDTO {
        // Check if user exists
        if (!userService.existsUser(userId)) {
            throw NotFoundError("user", userId)
        }

        val identity =
            userRealNameIdentityRepository.findByUserId(userId)
                ?: throw NotFoundError("user real name identity", userId)

        // If the data is encrypted, decrypt it
        return if (identity.realNameInfo.encrypted) {
            val decryptedRealName =
                encryptionService.decryptData(
                    identity.realNameInfo.realName,
                    identity.encryptionKeyId,
                )
            val decryptedStudentId =
                encryptionService.decryptData(
                    identity.realNameInfo.studentId,
                    identity.encryptionKeyId,
                )
            val decryptedGrade =
                encryptionService.decryptData(identity.realNameInfo.grade, identity.encryptionKeyId)
            val decryptedMajor =
                encryptionService.decryptData(identity.realNameInfo.major, identity.encryptionKeyId)
            val decryptedClassName =
                encryptionService.decryptData(
                    identity.realNameInfo.className,
                    identity.encryptionKeyId,
                )

            UserIdentityDTO(
                realName = decryptedRealName,
                studentId = decryptedStudentId,
                grade = decryptedGrade,
                major = decryptedMajor,
                className = decryptedClassName,
            )
        } else {
            UserIdentityDTO(
                realName = identity.realNameInfo.realName,
                studentId = identity.realNameInfo.studentId,
                grade = identity.realNameInfo.grade,
                major = identity.realNameInfo.major,
                className = identity.realNameInfo.className,
            )
        }
    }

    /** Get fuzzy user real name identity information */
    @Transactional
    fun getFuzzyUserIdentity(userId: IdType): UserIdentityDTO {
        // Check if user exists
        if (!userService.existsUser(userId)) {
            throw NotFoundError("user", userId)
        }

        val identity =
            userRealNameIdentityRepository.findByUserId(userId)
                ?: throw NotFoundError("user real name identity", userId)

        // Get the data, potentially decrypted
        val originalIdentity =
            if (identity.realNameInfo.encrypted) {
                val decryptedRealName =
                    encryptionService.decryptData(
                        identity.realNameInfo.realName,
                        identity.encryptionKeyId,
                    )
                val decryptedStudentId =
                    encryptionService.decryptData(
                        identity.realNameInfo.studentId,
                        identity.encryptionKeyId,
                    )
                val decryptedGrade =
                    encryptionService.decryptData(
                        identity.realNameInfo.grade,
                        identity.encryptionKeyId,
                    )
                val decryptedMajor =
                    encryptionService.decryptData(
                        identity.realNameInfo.major,
                        identity.encryptionKeyId,
                    )
                val decryptedClassName =
                    encryptionService.decryptData(
                        identity.realNameInfo.className,
                        identity.encryptionKeyId,
                    )

                UserIdentityDTO(
                    realName = decryptedRealName,
                    studentId = decryptedStudentId,
                    grade = decryptedGrade,
                    major = decryptedMajor,
                    className = decryptedClassName,
                )
            } else {
                UserIdentityDTO(
                    realName = identity.realNameInfo.realName,
                    studentId = identity.realNameInfo.studentId,
                    grade = identity.realNameInfo.grade,
                    major = identity.realNameInfo.major,
                    className = identity.realNameInfo.className,
                )
            }

        // Apply fuzzy logic to mask sensitive data
        return UserIdentityDTO(
            realName = maskName(originalIdentity.realName),
            studentId = maskStudentId(originalIdentity.studentId),
            grade = originalIdentity.grade, // Keep grade as is, not sensitive
            major = originalIdentity.major, // Keep major as is, not sensitive
            className = originalIdentity.className, // Keep className as is, not sensitive
        )
    }

    // Helper method to mask a name, keeping only the first character
    private fun maskName(name: String): String {
        if (name.isEmpty()) return name
        return "${name.first()}${"*".repeat(name.length - 1)}"
    }

    // Helper method to mask student ID, keeping only the first and last digits
    private fun maskStudentId(studentId: String): String {
        if (studentId.length <= 2) return studentId
        val prefix = studentId.substring(0, 1)
        val suffix = studentId.substring(studentId.length - 1)
        return "$prefix${"*".repeat(studentId.length - 2)}$suffix"
    }

    /** Create or update user real name identity information */
    @Transactional
    fun createOrUpdateUserIdentity(
        userId: IdType,
        realName: String,
        studentId: String,
        grade: String,
        major: String,
        className: String,
        shouldEncrypt: Boolean = true,
    ): UserIdentityDTO {
        // Check if user exists
        if (!userService.existsUser(userId)) {
            throw NotFoundError("user", userId)
        }

        // Get existing identity if it exists
        val existingIdentity = userRealNameIdentityRepository.findByUserId(userId)

        // Get or create encryption key - this should no longer cause optimistic locking issues
        val key = encryptionService.getOrCreateKey(KeyPurpose.USER_REAL_NAME, userId)

        // Create user real name info
        val realNameInfo =
            if (shouldEncrypt) {
                // Encrypt the data
                UserRealNameInfo(
                    realName = encryptionService.encryptData(realName, key.id),
                    studentId = encryptionService.encryptData(studentId, key.id),
                    grade = encryptionService.encryptData(grade, key.id),
                    major = encryptionService.encryptData(major, key.id),
                    className = encryptionService.encryptData(className, key.id),
                    encrypted = true,
                )
            } else {
                // Store without encryption
                UserRealNameInfo(
                    realName = realName,
                    studentId = studentId,
                    grade = grade,
                    major = major,
                    className = className,
                    encrypted = false,
                )
            }

        // Create or update the identity
        val identity =
            if (existingIdentity != null) {
                // Update existing identity with new information
                existingIdentity.updateInfo(realNameInfo, key.id)
                existingIdentity
            } else {
                // Create new identity
                UserRealNameIdentity(
                    userId = userId,
                    realNameInfo = realNameInfo,
                    encryptionKeyId = key.id,
                )
            }

        // Save the identity
        userRealNameIdentityRepository.save(identity)

        // Return the DTO with decrypted data
        return UserIdentityDTO(
            realName = realName,
            studentId = studentId,
            grade = grade,
            major = major,
            className = className,
        )
    }

    /** Log access to user real name information */
    @Transactional
    fun logAccess(
        accessorId: IdType,
        targetId: IdType,
        accessReason: String,
        accessType: AccessType,
        moduleType: AccessModuleType? = null,
        moduleEntityId: IdType? = null,
    ) {
        val request =
            (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        val ipAddress = request?.remoteAddr ?: "unknown"

        val log =
            UserRealNameAccessLog(
                accessorId = accessorId,
                targetId = targetId,
                moduleType = moduleType,
                moduleEntityId = moduleEntityId,
                accessReason = accessReason,
                ipAddress = ipAddress,
                accessType = accessType,
            )

        userRealNameAccessLogRepository.save(log)
    }

    /** Get access logs for a user's real name information */
    @Transactional(readOnly = true)
    fun getAccessLogs(
        targetUserId: IdType,
        pageSize: Int,
        pageStart: IdType?,
    ): Pair<List<UserIdentityAccessLogDTO>, PageDTO> {
        val spec =
            Specification.where { root, _, cb ->
                cb.equal(root.getProperty(UserRealNameAccessLog::targetId), targetUserId)
            }
        val cursorSpec =
            userRealNameAccessLogRepository
                .simpleCursorSpec(UserRealNameAccessLog::id)
                .sortBy(UserRealNameAccessLog::id.desc())
                .specification(spec)
                .build()
        val (result, page) =
            userRealNameAccessLogRepository.findAllWithCursor(
                cursorSpec,
                cursor = pageStart?.let { SimpleCursor.of(it) },
                pageSize = pageSize,
            )

        // convert to DTOs
        val accessors = result.map { it.accessorId }.distinct()
        val userDTOs = userService.getUserDtos(accessors)
        val resultDTOs =
            result.map { log ->
                val accessorUser =
                    userDTOs[log.accessorId] ?: throw NotFoundError("user", log.accessorId)
                convertToDto(log, accessorUser)
            }

        return Pair(resultDTOs, page.toPageDTO())
    }

    private fun AccessModuleType.toDTO(): UserIdentityAccessModuleTypeDTO {
        return UserIdentityAccessModuleTypeDTO.forValue(this.name)
    }

    private fun getEntityName(moduleType: AccessModuleType?, moduleEntityId: IdType?): String? {
        if (moduleType == null || moduleEntityId == null) {
            return null
        }
        return when (moduleType) {
            AccessModuleType.TASK -> taskInfoProvider.getTaskNameById(moduleEntityId)
        }
    }

    /** Convert UserRealNameAccessLog to UserIdentityAccessLogDTO */
    fun convertToDto(log: UserRealNameAccessLog, accessorUser: UserDTO): UserIdentityAccessLogDTO {
        // Get the current time in milliseconds
        val accessTime =
            log.createdAt.toInstant(ZoneOffset.UTC)?.toEpochMilli()
                ?: OffsetDateTime.now().toInstant().toEpochMilli()

        // Create module type DTO
        val moduleTypeDto = log.moduleType?.toDTO()

        // Create access type DTO
        val accessTypeDto = UserIdentityAccessTypeDTO.forValue(log.accessType.name)

        return UserIdentityAccessLogDTO(
            accessor = accessorUser,
            accessModuleType = moduleTypeDto,
            accessEntityId = log.moduleEntityId,
            accessEntityName = getEntityName(log.moduleType, log.moduleEntityId),
            accessTime = accessTime,
            accessType = accessTypeDto,
            ipAddress = log.ipAddress,
        )
    }

    fun hasUserIdentity(userId: IdType): Boolean {
        return userRealNameIdentityRepository.existsByUserId(userId)
    }
}
