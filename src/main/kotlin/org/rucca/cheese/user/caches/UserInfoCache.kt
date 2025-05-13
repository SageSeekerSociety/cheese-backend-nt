package org.rucca.cheese.user.caches

import org.rucca.cheese.common.config.CacheConfig
import org.rucca.cheese.model.UserDTO
import org.rucca.cheese.user.UserProfileRepository
import org.rucca.cheese.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserInfoCache(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Fetches and caches a single UserDTO. This is the method that should be called internally or
     * externally to get cached user info. It returns null if the user or essential profile data is
     * not found.
     *
     * @param userId The ID of the user.
     * @return The UserDTO or null if not found/incomplete.
     */
    @Cacheable(
        cacheNames = [CacheConfig.USER_DTO_CACHE],
        key = "#userId",
        unless = "#result == null",
    )
    @Transactional(readOnly = true)
    fun findUserDto(userId: Long): UserDTO? {
        log.debug("Cache miss or fetch for user ID: {}", userId)
        try {
            val user = userRepository.findById(userId.toInt()).orElse(null) ?: return null
            val profile =
                userProfileRepository.findByUserId(userId.toInt()).orElse(null) ?: return null

            if (
                profile.avatar?.id == null ||
                    profile.intro == null ||
                    profile.nickname == null ||
                    user.username == null
            ) {
                log.warn("Incomplete data for user ID {}, cannot create DTO.", userId)
                return null
            }

            return UserDTO(
                avatarId = profile.avatar!!.id!!.toLong(),
                id = user.id!!.toLong(),
                intro = profile.intro!!,
                nickname = profile.nickname!!,
                username = user.username!!,
            )
        } catch (e: Exception) {
            log.error("Error fetching UserDTO components for user ID {}: {}", userId, e.message, e)
            return null
        }
    }

    /**
     * Evicts the UserDTO cache entry for a specific user. Call this method (e.g., from an event
     * listener) when user profile data changes.
     *
     * @param userId The ID of the user whose cache entry should be evicted.
     */
    @CacheEvict(cacheNames = [CacheConfig.USER_DTO_CACHE], key = "#userId")
    fun evictUserDto(userId: Long) {
        log.info("Evicting cache entry for user ID: {}", userId)
    }
}
