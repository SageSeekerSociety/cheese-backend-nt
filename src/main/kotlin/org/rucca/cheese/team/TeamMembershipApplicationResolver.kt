package org.rucca.cheese.team

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rucca.cheese.notification.resolver.EntityInfoResolver
import org.rucca.cheese.notification.resolver.ResolvableEntityInfo
import org.rucca.cheese.team.repositories.TeamMembershipApplicationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

data class TeamMembershipApplicationInfo(
    override val id: String,
    override val name: String = "",
    override val type: String = "team_membership_application",
    override val url: String? = null,
    override val avatarUrl: String? = null,
    override val status: String, // Enum name as String ("PENDING", "ACCEPTED", etc.)
) : ResolvableEntityInfo

@Component
class TeamMembershipApplicationResolver(
    private val applicationRepository: TeamMembershipApplicationRepository
) : EntityInfoResolver {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun supportedEntityType(): String = "team_membership_application"

    override suspend fun resolve(ids: Set<String>): Map<String, ResolvableEntityInfo?> {
        if (ids.isEmpty()) return emptyMap()

        val longIds = ids.mapNotNull { it.toLongOrNull() }.toSet()
        if (longIds.isEmpty()) {
            log.warn(
                "No valid Long IDs found in the set for team_membership_application resolution."
            )
            return ids.associateWith { null } // Return null for all requested string IDs
        }

        log.debug("Resolving team application statuses for IDs: {}", longIds)

        // Use withContext for blocking repository call
        val applications =
            withContext(Dispatchers.IO) {
                // Fetch entities - Consider fetching only ID and status if possible for performance
                applicationRepository.findAllById(longIds)
            }

        val resultMap =
            applications.associate { app ->
                // Key is the original String ID
                app.id!!.toString() to
                    TeamMembershipApplicationInfo(
                        id = app.id!!.toString(),
                        status =
                            app.status
                                .name, // Get the enum name as String ("PENDING", "ACCEPTED", etc.)
                    )
            }

        // Add null entries for IDs that were requested but not found
        return ids.associateWith { resultMap[it] }
    }
}
