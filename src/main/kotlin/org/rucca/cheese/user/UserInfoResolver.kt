package org.rucca.cheese.user

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.notification.resolver.EntityInfoResolver
import org.rucca.cheese.notification.resolver.ResolvableEntityInfo
import org.rucca.cheese.user.services.UserService
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

data class UserEntityInfo(
    override val id: String,
    override val name: String,
    override val type: String = "user",
    override val url: String?,
    override val avatarUrl: String?,
) : ResolvableEntityInfo

@Component
class UserInfoResolver(
    private val applicationConfig: ApplicationConfig,
    private val userService: UserService,
) : EntityInfoResolver {
    override fun supportedEntityType(): String = "user"

    override suspend fun resolve(entityIds: Set<String>): Map<String, ResolvableEntityInfo?> {
        val longIds = entityIds.mapNotNull { it.toLongOrNull() }.toList()
        if (longIds.isEmpty()) {
            return emptyMap()
        }

        return withContext(Dispatchers.IO) {
            val userInfos = userService.getUserDtos(longIds)

            userInfos
                .mapValues { (_, basicInfo) ->
                    val avatarUrl =
                        UriComponentsBuilder.fromUriString(applicationConfig.legacyUrl)
                            .path("/avatars")
                            .pathSegment("{id}")
                            .build(basicInfo.avatarId)
                            .toString()

                    UserEntityInfo(
                        id = basicInfo.id.toString(),
                        name = basicInfo.nickname,
                        url = "/users/${basicInfo.id}",
                        avatarUrl = avatarUrl,
                    )
                }
                .mapKeys { it.key.toString() }
        }
    }
}
