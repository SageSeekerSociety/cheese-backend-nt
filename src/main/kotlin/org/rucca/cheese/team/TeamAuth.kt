package org.rucca.cheese.team

// No longer need TeamMembershipService here if checks move to service
import jakarta.annotation.PostConstruct
import org.rucca.cheese.auth.context.ContextKey
import org.rucca.cheese.auth.context.DomainContextKeys
import org.rucca.cheese.auth.context.PermissionContextProvider
import org.rucca.cheese.auth.context.buildResourceContext
import org.rucca.cheese.auth.core.*
import org.rucca.cheese.auth.domain.DomainPermissionService
import org.rucca.cheese.auth.domain.DomainRoleProvider
import org.rucca.cheese.auth.dsl.applyHierarchy
import org.rucca.cheese.auth.dsl.definePermissions
import org.rucca.cheese.auth.dsl.defineRoleHierarchy
import org.rucca.cheese.auth.hierarchy.GraphRoleHierarchy
import org.rucca.cheese.auth.registry.RegistrationService
import org.rucca.cheese.common.persistent.IdType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
object TeamDomain : Domain {
    override val name: String = "team"
}

// --- Refined Actions ---
enum class TeamAction(override val actionId: String) : Action {
    // Basic CRUD on Team resource
    CREATE("create"),
    VIEW("view"),
    UPDATE("update"),
    DELETE("delete"),
    ENUMERATE("enumerate"), // Could potentially merge VIEW and ENUMERATE if desired

    // Membership Management on Membership resource
    REMOVE_MEMBER("remove_member"),
    UPDATE_MEMBER_ROLE("update_member_role"),

    // Request Management on Request resource (Admin actions)
    APPROVE_REQUEST("approve_request"),
    REJECT_REQUEST("reject_request"),

    // Invitation Management on Invitation resource (Admin actions)
    CREATE_INVITATION("create_invitation"),
    CANCEL_INVITATION("cancel_invitation"),

    // Ownership Management on Team resource
    TRANSFER_OWNERSHIP("transfer_ownership");

    override val domain: Domain = TeamDomain
}

// --- Resource Types (Unchanged) ---
enum class TeamResource(override val typeName: String) : ResourceType {
    TEAM("team"),
    MEMBERSHIP("membership"),
    REQUEST("request"),
    INVITATION("invitation");

    override val domain: Domain = TeamDomain

    companion object {
        fun of(typeName: String) =
            entries.find { it.typeName.equals(typeName, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid team resource type: $typeName")
    }
}

// --- Domain Roles (Unchanged) ---
enum class TeamRole(override val roleId: String) : Role {
    OWNER("owner"),
    ADMIN("admin"),
    MEMBER("member");

    override val domain: Domain? = TeamDomain
}

fun TeamMemberRole.toAuthRole(): TeamRole =
    when (this) {
        TeamMemberRole.OWNER -> TeamRole.OWNER
        TeamMemberRole.ADMIN -> TeamRole.ADMIN
        TeamMemberRole.MEMBER -> TeamRole.MEMBER
    }

// --- Role Hierarchy (Unchanged) ---
@Component
class TeamRoleHierarchyConfig(private val roleHierarchy: GraphRoleHierarchy) {
    @PostConstruct
    fun configureRoleHierarchy() {
        val hierarchyConfig = defineRoleHierarchy {
            role(TeamRole.OWNER) { inheritsFrom(TeamRole.ADMIN) }
            role(TeamRole.ADMIN) { inheritsFrom(TeamRole.MEMBER) }
            role(TeamRole.MEMBER)
        }
        roleHierarchy.applyHierarchy(hierarchyConfig)
    }
}

// --- Context Keys (Unchanged, potentially simplify if not used in complex conditions) ---
object TeamContextKeys {
    val TEAM_ID = ContextKey.of<IdType>("teamId")
    val TARGET_USER_ID = ContextKey.of<IdType>("targetUserId")
    val REQUEST_ID =
        ContextKey.of<IdType>("requestId") // Keep if needed for specific request context
    val INVITATION_ID =
        ContextKey.of<IdType>("invitationId") // Keep if needed for specific invite context
}

@Component
class TeamContextProvider : PermissionContextProvider {
    override val domain: Domain = TeamDomain

    override fun getContext(resourceName: String, resourceId: IdType?): Map<String, Any> {
        val resourceType = TeamResource.of(resourceName)
        return buildResourceContext(domain, resourceType, resourceId) {
            when (TeamResource.of(resourceName)) {
                TeamResource.TEAM -> resourceId?.let { TeamContextKeys.TEAM_ID(it) }
                TeamResource.REQUEST -> resourceId?.let { TeamContextKeys.REQUEST_ID(it) }
                TeamResource.INVITATION -> resourceId?.let { TeamContextKeys.INVITATION_ID(it) }
                TeamResource.MEMBERSHIP -> {
                    /* TEAM_ID expected from @AuthContext */
                }
            }
        }
    }
}

@Component
class TeamRoleProvider(private val teamService: TeamService) : DomainRoleProvider {
    private val logger = LoggerFactory.getLogger(TeamRoleProvider::class.java)

    override val domain: Domain = TeamDomain

    override fun getRoles(userId: IdType, context: Map<String, Any>): Set<Role> {
        val teamId =
            DomainContextKeys.RESOURCE_ID.get(context)?.takeIf {
                DomainContextKeys.RESOURCE_TYPE.get(context) == TeamResource.TEAM.typeName
            } ?: TeamContextKeys.TEAM_ID.get(context) ?: return emptySet()
        val roles = mutableSetOf<Role>()
        try {
            teamService.findTeamUserRelation(teamId, userId)?.role?.let {
                roles.add(it.toAuthRole())
            }
        } catch (e: Exception) {
            logger.error(
                "Error fetching team role for user {} in team {}: {}",
                userId,
                teamId,
                e.message,
            )
        }
        logger.debug("Determined roles for user {} in team {}: {}", userId, teamId, roles)
        return roles
    }
}

@Component
class TeamPermissionConfig(
    private val permissionService: PermissionConfigurationService,
    private val registrationService: RegistrationService,
) : DomainPermissionService {
    private val logger = LoggerFactory.getLogger(TeamPermissionConfig::class.java)
    override val domain: Domain = TeamDomain

    private fun PermissionRule<TeamAction, TeamResource>.isSelf() {
        withCondition("isSelf") { userInfo, _, _, _, context ->
            val targetUserId = TeamContextKeys.TARGET_USER_ID.get(context)
            targetUserId != null && userInfo.userId == targetUserId
        }
    }

    private fun PermissionRule<TeamAction, TeamResource>.isNotSelf() {
        withCondition("isNotSelf") { userInfo, _, _, _, context ->
            val targetUserId = TeamContextKeys.TARGET_USER_ID.get(context)
            val result = targetUserId == null || userInfo.userId != targetUserId
            result
        }
    }

    @PostConstruct
    override fun configurePermissions() {
        // 1. Register Actions & Resources
        registrationService.registerActions(*TeamAction.entries.toTypedArray())
        registrationService.registerResources(*TeamResource.entries.toTypedArray())

        // 2. Define Permissions using refined actions
        val config = definePermissions {

            // --- SystemRole.USER Permissions (Any logged-in user) ---
            role(SystemRole.USER) {
                // Can attempt to create a team and view teams publicly
                can(TeamAction.CREATE, TeamAction.VIEW, TeamAction.ENUMERATE)
                    .on(TeamResource.TEAM)
                    .all() // Service layer might have other limits (e.g., max teams per user)
                // Can list/search teams (public/all teams endpoint)
                can(TeamAction.ENUMERATE).on(TeamResource.TEAM).all()
                // Note: Creating requests, accepting/declining invites are handled by @Auth login
                // check + specific controller logic
            }

            // --- TeamRole.MEMBER Permissions ---
            role(TeamRole.MEMBER) {
                // Can view the team, its members, requests, and invitations (if allowed by business
                // logic)
                can(TeamAction.VIEW)
                    .on(
                        TeamResource.TEAM,
                        TeamResource.MEMBERSHIP,
                        TeamResource.REQUEST,
                        TeamResource.INVITATION,
                    )
                    .all()
                // Can leave the team (remove self)
                can(TeamAction.REMOVE_MEMBER).on(TeamResource.MEMBERSHIP).where { isSelf() }
            }

            // --- TeamRole.ADMIN Permissions ---
            role(TeamRole.ADMIN) { // Inherits MEMBER permissions
                // Can update basic team info
                can(TeamAction.UPDATE).on(TeamResource.TEAM).all()
                // Can remove other members (Service layer checks if target is MEMBER)
                can(TeamAction.REMOVE_MEMBER).on(TeamResource.MEMBERSHIP).where {
                    isNotSelf()
                } // Must not be self
                // Can update member roles (Service layer checks target role validity)
                can(TeamAction.UPDATE_MEMBER_ROLE).on(TeamResource.MEMBERSHIP).all()

                // Manage join requests
                can(TeamAction.APPROVE_REQUEST, TeamAction.REJECT_REQUEST)
                    .on(TeamResource.REQUEST)
                    .all()

                // Manage invitations
                can(TeamAction.CREATE_INVITATION, TeamAction.CANCEL_INVITATION)
                    .on(TeamResource.INVITATION)
                    .all()
            }

            // --- TeamRole.OWNER Permissions ---
            role(TeamRole.OWNER) { // Inherits ADMIN permissions
                // Can delete the team
                can(TeamAction.DELETE).on(TeamResource.TEAM).all()
                // Can transfer ownership
                can(TeamAction.TRANSFER_OWNERSHIP).on(TeamResource.TEAM).all()
                // Owner implicitly has all permissions from ADMIN, including removing ADMINS
                // (Service validates target != owner)
            }
        }

        // 3. Apply Configuration
        permissionService.applyConfiguration(config)
    }
}
