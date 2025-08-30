package org.rucca.cheese.knowledge

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
import org.rucca.cheese.team.TeamService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
object KnowledgeDomain : Domain {
    override val name: String = "knowledge"
}

enum class KnowledgeAction(override val actionId: String) : Action {
    CREATE("create"),
    LIST("list"),
    VIEW("view"),
    UPDATE("update"),
    DELETE("delete");

    override val domain: Domain = KnowledgeDomain
}

enum class KnowledgeResource(override val typeName: String) : ResourceType {
    KNOWLEDGE("knowledge");

    override val domain: Domain = KnowledgeDomain

    companion object {
        fun of(typeName: String) =
            entries.find { it.typeName == typeName } ?: error("Invalid resource type")
    }
}

enum class KnowledgeRole(override val roleId: String) : Role {
    CREATOR("creator"),
    CONTRIBUTOR("contributor"),
    READER("reader");

    override val domain: Domain? = KnowledgeDomain
}

@Component
class KnowledgeRoleHierarchyConfig(private val roleHierarchy: GraphRoleHierarchy) {
    @PostConstruct
    fun configureRoleHierarchy() {
        val hierarchyConfig = defineRoleHierarchy {
            role(KnowledgeRole.CREATOR) { inheritsFrom(KnowledgeRole.CONTRIBUTOR) }
            role(KnowledgeRole.CONTRIBUTOR) { inheritsFrom(KnowledgeRole.READER) }
            role(KnowledgeRole.READER)
        }

        roleHierarchy.applyHierarchy(hierarchyConfig)
    }
}

object KnowledgeContextKeys {
    val TEAM_ID = ContextKey.of<IdType>("teamId")
    val PROJECT_ID = ContextKey.of<IdType>("projectId")
    val IS_TEAM_MEMBER_PROVIDER = ContextKey.of<(IdType, IdType) -> Boolean>("isTeamMemberProvider")
}

@Component
class KnowledgeContextProvider(private val teamService: TeamService) : PermissionContextProvider {
    override val domain: Domain = KnowledgeDomain

    override fun getContext(resourceName: String, resourceId: IdType?) =
        buildResourceContext(domain, KnowledgeResource.of(resourceName), resourceId) {
            KnowledgeContextKeys.IS_TEAM_MEMBER_PROVIDER { teamId, userId ->
                teamService.isTeamMember(teamId, userId)
            }
        }
}

@Component
class KnowledgeRoleProvider(private val knowledgeService: KnowledgeService) : DomainRoleProvider {
    private val logger = LoggerFactory.getLogger(KnowledgeRoleProvider::class.java)

    override val domain: Domain = KnowledgeDomain

    override fun getRoles(userId: IdType, context: Map<String, Any>): Set<Role> {
        val roles = mutableSetOf<Role>()
        val knowledgeId = DomainContextKeys.RESOURCE_ID.get(context)

        when (DomainContextKeys.RESOURCE_TYPE.get(context)?.let { KnowledgeResource.of(it) }) {
            KnowledgeResource.KNOWLEDGE -> {
                // 如果是在查询特定知识条目
                if (knowledgeId != null) {
                    // 如果知识条目不存在，直接抛出 NotFoundError
                    val knowledge = knowledgeService.getKnowledge(knowledgeId)

                    // 检查团队成员关系
                    val teamId = knowledge.team?.id?.toLong()
                    val isTeamMember = KnowledgeContextKeys.IS_TEAM_MEMBER_PROVIDER.get(context)

                    if (teamId != null && isTeamMember != null && isTeamMember(teamId, userId)) {
                        roles.add(KnowledgeRole.READER)

                        // 分配贡献者角色给团队成员
                        // 这里可以根据实际业务需求修改，例如只有特定角色的团队成员才能成为贡献者
                        roles.add(KnowledgeRole.CONTRIBUTOR)
                    }

                    // 检查是否是创建者
                    if (knowledge.createdBy?.id?.toLong() == userId) {
                        roles.add(KnowledgeRole.CREATOR)
                    }
                }
                // 如果是在查询知识列表
                else {
                    val teamId = KnowledgeContextKeys.TEAM_ID.get(context)
                    val isTeamMember = KnowledgeContextKeys.IS_TEAM_MEMBER_PROVIDER.get(context)

                    if (teamId != null && isTeamMember != null && isTeamMember(teamId, userId)) {
                        roles.add(KnowledgeRole.READER)

                        // 同样，所有团队成员都可以是知识库贡献者
                        roles.add(KnowledgeRole.CONTRIBUTOR)
                    }
                }
            }
            null -> {}
        }

        logger.debug("Roles for user {} on knowledge {}: {}", userId, knowledgeId, roles)
        return roles.toSet()
    }
}

@Component
class KnowledgePermissionConfig(
    private val permissionService: PermissionConfigurationService,
    private val registrationService: RegistrationService,
) : DomainPermissionService {
    private val logger = LoggerFactory.getLogger(KnowledgePermissionConfig::class.java)

    override val domain: Domain = KnowledgeDomain

    private fun PermissionRule<KnowledgeAction, KnowledgeResource>.isTeamMember() {
        withCondition { userInfo, _, _, _, context ->
            val teamId = KnowledgeContextKeys.TEAM_ID.get(context) ?: return@withCondition false
            val isTeamMember =
                KnowledgeContextKeys.IS_TEAM_MEMBER_PROVIDER.get(context)
                    ?: return@withCondition false
            val userId = userInfo.userId
            isTeamMember(teamId, userId)
        }
    }

    @PostConstruct
    override fun configurePermissions() {
        // 注册操作和资源
        registrationService.registerActions(*KnowledgeAction.entries.toTypedArray())
        registrationService.registerResources(*KnowledgeResource.entries.toTypedArray())

        // 定义权限
        val config = definePermissions {
            // 普通用户权限
            role(SystemRole.USER) {
                can(KnowledgeAction.CREATE).on(KnowledgeResource.KNOWLEDGE).where { isTeamMember() }
            }

            // 管理员权限
            role(SystemRole.ADMIN) {
                can(
                        KnowledgeAction.CREATE,
                        KnowledgeAction.LIST,
                        KnowledgeAction.VIEW,
                        KnowledgeAction.UPDATE,
                        KnowledgeAction.DELETE,
                    )
                    .on(KnowledgeResource.KNOWLEDGE)
                    .all()
            }

            // 读者角色权限
            role(KnowledgeRole.READER) {
                can(KnowledgeAction.LIST, KnowledgeAction.VIEW)
                    .on(KnowledgeResource.KNOWLEDGE)
                    .all()
            }

            // 贡献者角色权限
            role(KnowledgeRole.CONTRIBUTOR) {
                can(KnowledgeAction.CREATE).on(KnowledgeResource.KNOWLEDGE).all()
            }

            // 创建者角色权限
            role(KnowledgeRole.CREATOR) {
                can(KnowledgeAction.UPDATE, KnowledgeAction.DELETE)
                    .on(KnowledgeResource.KNOWLEDGE)
                    .all()
            }
        }

        // 应用配置
        permissionService.applyConfiguration(config)
    }
}
