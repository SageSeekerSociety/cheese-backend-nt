package org.rucca.cheese.user

import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TaskParticipantSummaryDTO
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

    fun getTaskParticipantSummaryDto(userId: IdType): TaskParticipantSummaryDTO {
        val dto = getUserDto(userId)
        return TaskParticipantSummaryDTO(
            id = dto.id,
            intro = dto.intro,
            name = dto.nickname,
            avatarId = dto.avatarId,
        )
    }

    fun getUserAvatarId(userId: IdType): IdType {
        val profile =
            userProfileRepository.findByUserId(userId.toInt()).orElseThrow {
                NotFoundError("user", userId)
            }
        return profile.avatar!!.id!!.toLong()
    }

    fun ensureUserExists(userId: IdType) {
        userRepository.findById(userId.toInt()).orElseThrow { NotFoundError("user", userId) }
    }
}
