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
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
) {
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

    /** 批量转换用户为DTO，优化查询性能 此方法使用批量查询一次性获取所有用户资料 */
    fun convertUsersToDto(users: List<User>): Map<Long, UserDTO> {
        if (users.isEmpty()) {
            return emptyMap()
        }

        // 提取所有用户ID
        val userIds = users.mapNotNull { it.id }

        // 一次性查询所有用户资料
        val profiles = userProfileRepository.findAllByUserIdIn(userIds)

        // 构建userId到profile的映射，方便快速查找
        val profileMap = profiles.associateBy { it.user!!.id!! }

        // 为每个用户构建DTO
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
}
