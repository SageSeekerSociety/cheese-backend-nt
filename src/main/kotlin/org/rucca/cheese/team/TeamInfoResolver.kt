package org.rucca.cheese.team

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.notification.resolver.EntityInfoResolver
import org.rucca.cheese.notification.resolver.ResolvableEntityInfo
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

data class TeamEntityInfo(
    override val id: String,
    override val name: String,
    override val type: String = "team",
    override val url: String?,
    override val avatarUrl: String?,
) : ResolvableEntityInfo

@Component
class TeamInfoResolver(
    private val teamService: TeamService,
    private val applicationConfig: ApplicationConfig,
) : EntityInfoResolver {

    override fun supportedEntityType(): String = "team"

    override suspend fun resolve(entityIds: Set<String>): Map<String, ResolvableEntityInfo?> {
        val longIds = entityIds.mapNotNull { it.toLongOrNull() }.toList()
        if (longIds.isEmpty()) return emptyMap()

        return withContext(Dispatchers.IO) {
            val teamInfos = teamService.getTeamSummaryDTOs(longIds)

            teamInfos
                .mapValues { (_, summaryInfo) ->
                    summaryInfo.let {
                        val avatarUrl =
                            UriComponentsBuilder.fromUriString(applicationConfig.legacyUrl)
                                .path("/avatars")
                                .pathSegment("{id}")
                                .build(it.avatarId)
                                .toString()

                        TeamEntityInfo(
                            id = it.id.toString(),
                            name = it.name,
                            url = "/teams/${it.id}",
                            avatarUrl = avatarUrl,
                        )
                    }
                }
                .mapKeys { it.key.toString() }
        }
    }
}
