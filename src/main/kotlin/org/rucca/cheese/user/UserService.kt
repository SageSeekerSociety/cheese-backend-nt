/*
 *  Description: This file implements the TopicService class.
 *               It is responsible for providing user's DTO.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      nameisyui
 *
 */

package org.rucca.cheese.user

import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.UserDTO
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userRoleRepository: UserRoleRepository,
) {
    @Transactional
    @CacheEvict(cacheNames = ["userRoles"], key = "#userId")
    fun addRole(userId: IdType, role: UserRole) {
        ensureUserIdExists(userId)
        if (!userRoleRepository.existsByUserIdAndRole(userId, role)) {
            userRoleRepository.save(UserRoleEntity(userId, role))
        }
    }

    fun getUserDto(userId: IdType): UserDTO {
        val user =
            userRepository.findById(userId.toInt()).orElseThrow { NotFoundError("user", userId) }
        val profile =
            userProfileRepository.findByUserId(userId.toInt()).orElseThrow {
                RuntimeException("UserProfile not found for user $userId")
            }
        return UserDTO(
            avatarId = profile.avatar!!.id!!.toLong(),
            id = user.id!!.toLong(),
            intro = profile.intro!!,
            nickname = profile.nickname!!,
            username = user.username!!,
        )
    }

    // use batch query to improve performance
    fun getUserDtos(userIds: List<IdType>): Map<Long, UserDTO> {
        val users = userRepository.findAllById(userIds.map { it.toInt() })
        return convertUsersToDto(users)
    }

    /**
     * Batch converts User entities to DTOs with optimized query performance.
     *
     * Instead of N+1 queries (one for each user plus one for each profile), this method performs a
     * single batch query to fetch all profiles at once, reducing database roundtrips significantly
     * for large sets.
     */
    fun convertUsersToDto(users: List<User>): Map<Long, UserDTO> {
        if (users.isEmpty()) {
            return emptyMap()
        }

        val userIds = users.mapNotNull { it.id }
        val profiles = userProfileRepository.findAllByUserIdIn(userIds)
        val profileMap = profiles.associateBy { it.user!!.id!! }
        return users
            .mapNotNull { user ->
                val userId = user.id ?: return@mapNotNull null
                val profile = profileMap[userId] ?: return@mapNotNull null

                val dto =
                    UserDTO(
                        avatarId = profile.avatar!!.id!!.toLong(),
                        id = userId.toLong(),
                        intro = profile.intro!!,
                        nickname = profile.nickname!!,
                        username = user.username!!,
                    )

                userId.toLong() to dto
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
