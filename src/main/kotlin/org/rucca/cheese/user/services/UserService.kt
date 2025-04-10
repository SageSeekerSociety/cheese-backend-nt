/*
 *  Description: This file implements the TopicService class.
 *               It is responsible for providing user's DTO.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      nameisyui
 *
 */

package org.rucca.cheese.user.services

import java.util.Locale
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.UserDTO
import org.rucca.cheese.user.*
import org.rucca.cheese.user.caches.UserInfoCache
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userRoleRepository: UserRoleRepository,
    private val userInfoCache: UserInfoCache,
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

    fun getUserLocale(userId: IdType): Locale {
        // TODO: Implement user locale settings
        return Locale.CHINESE
    }

    fun getUserEmail(userId: IdType): String? {
        val user =
            userRepository.findById(userId.toInt()).orElseThrow { NotFoundError("user", userId) }
        return user.email
    }

    fun getUserReference(userId: IdType): User {
        return userRepository.getReferenceById(userId.toInt())
    }

    @Transactional
    @CacheEvict(cacheNames = ["userRoles"], key = "#userId")
    fun addRole(userId: IdType, role: UserRole) {
        ensureUserIdExists(userId)
        if (!userRoleRepository.existsByUserIdAndRole(userId, role)) {
            userRoleRepository.save(UserRoleEntity(userId, role))
        }
    }

    /**
     * Gets the UserDTO for a given user ID, utilizing the cache.
     *
     * @param userId The ID of the user.
     * @return The UserDTO.
     * @throws NotFoundError if the user is not found or essential data is missing.
     */
    fun getUserDto(userId: IdType): UserDTO {
        return userInfoCache.findUserDto(userId)
            ?: throw NotFoundError("user or essential profile data", userId)
    }

    /**
     * Gets UserDTOs for a list of user IDs using the cache for individual lookups.
     *
     * @param userIds List of user IDs.
     * @return A Map of userId to UserDTO. Users not found will be omitted.
     */
    fun getUserDtos(userIds: List<IdType>): Map<Long, UserDTO> {
        if (userIds.isEmpty()) {
            return emptyMap()
        }
        log.debug("Batch requesting UserDTOs for IDs: {}", userIds)
        return userIds
            .mapNotNull { userId ->
                try {
                    val dto = getUserDto(userId)
                    userId to dto
                } catch (e: NotFoundError) {
                    log.warn(
                        "User not found or incomplete data for ID {} during batch fetch.",
                        userId,
                    )
                    null
                } catch (e: Exception) {
                    log.error(
                        "Unexpected error fetching DTO for user ID {} in batch: {}",
                        userId,
                        e.message,
                        e,
                    )
                    null
                }
            }
            .toMap()
    }

    fun getUserAvatarId(userId: IdType): IdType {
        val profile =
            userProfileRepository.findByUserId(userId.toInt()).orElseThrow {
                NotFoundError("user", userId)
            }
        return profile.avatar!!.id!!.toLong()
    }

    fun existsUser(userId: IdType): Boolean {
        return userRepository.existsById(userId.toInt())
    }

    fun ensureUserIdExists(userId: IdType) {
        if (!existsUser(userId)) throw NotFoundError("user", userId)
    }

    fun ensureUsersExist(userIds: List<IdType>) {
        val expectedCount = userIds.distinct().size
        val count = userRepository.countByIdIn(userIds.distinct().map { it.toInt() })
        if (count != expectedCount) throw NotFoundError("Some users not found")
    }

    fun getUserRoles(userId: IdType): Set<UserRole> {
        return userRoleRepository.findAllByUserId(userId).map { it.role }.toSet()
    }
}
