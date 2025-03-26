package org.rucca.cheese.discussion

import jakarta.annotation.PostConstruct
import org.rucca.cheese.auth.context.ContextKey
import org.rucca.cheese.auth.context.DomainContextKeys
import org.rucca.cheese.auth.context.PermissionContextProvider
import org.rucca.cheese.auth.context.buildResourceContext
import org.rucca.cheese.auth.core.*
import org.rucca.cheese.auth.domain.DomainRoleProvider
import org.rucca.cheese.auth.dsl.applyHierarchy
import org.rucca.cheese.auth.dsl.definePermissions
import org.rucca.cheese.auth.dsl.defineRoleHierarchy
import org.rucca.cheese.auth.hierarchy.GraphRoleHierarchy
import org.rucca.cheese.auth.registry.RegistrationService
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.DiscussableModelTypeDTO
import org.rucca.cheese.project.ProjectService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
object DiscussionDomain : Domain {
    override val name: String = "discussion"
}

enum class DiscussionAction(override val actionId: String) : Action {
    LIST("list"),
    CREATE("create"),
    VIEW("view"),
    UPDATE("update"),
    DELETE("delete"),
    REACT("react");

    override val domain: Domain = DiscussionDomain
}

enum class DiscussionResource(override val typeName: String) : ResourceType {
    DISCUSSION("discussion"),
    REACTION("reaction");

    override val domain: Domain = DiscussionDomain

    companion object {
        fun of(typeName: String) =
            entries.find { it.typeName == typeName } ?: error("Invalid resource type")
    }
}

enum class DiscussionRole(override val roleId: String) : Role {
    CREATOR("creator"),
    PARTICIPANT("participant");

    override val domain: Domain? = DiscussionDomain
}

@Component
class DiscussionRoleHierarchyConfig(private val roleHierarchy: GraphRoleHierarchy) {
    @PostConstruct
    fun configureRoleHierarchy() {
        val hierarchyConfig = defineRoleHierarchy {
            role(DiscussionRole.CREATOR) { inheritsFrom(DiscussionRole.PARTICIPANT) }

            role(DiscussionRole.PARTICIPANT)
        }

        roleHierarchy.applyHierarchy(hierarchyConfig)
    }
}

object DiscussionContextKeys {
    val MODEL_TYPE = ContextKey.of<DiscussableModelTypeDTO>("modelType")
    val MODEL_ID = ContextKey.of<IdType>("modelId")
    val PARENT_ID = ContextKey.of<IdType>("parentId")
    val RESOURCE_ACCESS_CHECKER =
        ContextKey.of<(DiscussableModelTypeDTO, IdType, IdType) -> Boolean>("resourceAccessChecker")
}

@Component
class DiscussionContextProvider(private val projectService: ProjectService) :
    PermissionContextProvider {
    override val domain: Domain = DiscussionDomain

    override fun getContext(resourceName: String, resourceId: IdType?) =
        buildResourceContext(domain, DiscussionResource.of(resourceName), resourceId) {
            DiscussionContextKeys.RESOURCE_ACCESS_CHECKER { modelType, modelId, userId ->
                when (modelType) {
                    DiscussableModelTypeDTO.PROJECT -> {
                        runCatching { projectService.getMemberRole(modelId, userId) }.isSuccess
                    }
                }
            }
        }
}

@Component
class DiscussionRoleProvider(private val discussionService: DiscussionService) :
    DomainRoleProvider {
    private val logger = LoggerFactory.getLogger(DiscussionRoleProvider::class.java)

    override val domain: Domain = DiscussionDomain

    override fun getRoles(userId: IdType, context: Map<String, Any>): Set<Role> {
        val discussionId = DomainContextKeys.RESOURCE_ID.get(context)

        val roles = mutableSetOf<Role>()

        when (DomainContextKeys.RESOURCE_TYPE.get(context)?.let { DiscussionResource.of(it) }) {
            DiscussionResource.DISCUSSION,
            DiscussionResource.REACTION -> {
                val (modelType, modelId) =
                    Pair(
                            DiscussionContextKeys.MODEL_TYPE.get(context),
                            DiscussionContextKeys.MODEL_ID.get(context),
                        )
                        .takeIf { (type, id) ->
                            discussionId == null || (type != null && id != null)
                        } ?: discussionService.getModelTypeAndIdFromDiscussionId(discussionId!!)

                logger.debug(
                    "Checking roles for user {} on discussion {} with modelType {} and modelId {}",
                    userId,
                    discussionId,
                    modelType,
                    modelId,
                )

                if (modelType != null && modelId != null) {
                    val resourceAccessChecker =
                        DiscussionContextKeys.RESOURCE_ACCESS_CHECKER.get(context)
                    if (
                        resourceAccessChecker != null &&
                            resourceAccessChecker(modelType, modelId, userId)
                    ) {
                        roles.add(DiscussionRole.PARTICIPANT)
                    }
                }

                if (
                    discussionId != null &&
                        discussionService.isDiscussionCreator(discussionId, userId)
                ) {
                    roles.add(DiscussionRole.CREATOR)
                }
            }

            else -> {}
        }

        logger.debug("Found role {} for user {} on discussion {}", roles, userId, discussionId)
        return roles.toSet()
    }
}

@Component
class DiscussionPermissionConfig(
    private val permissionService: PermissionConfigurationService,
    private val registrationService: RegistrationService,
) {
    private fun PermissionRule<DiscussionAction, DiscussionResource>.hasResourceAccess() {
        withCondition { userInfo, _, _, _, context ->
            val modelType = DiscussionContextKeys.MODEL_TYPE.get(context)
            val modelId = DiscussionContextKeys.MODEL_ID.get(context)

            if (modelType == null || modelId == null) {
                return@withCondition false
            }

            val resourceAccessChecker =
                DiscussionContextKeys.RESOURCE_ACCESS_CHECKER.get(context)
                    ?: return@withCondition false
            val userId = userInfo.userId
            resourceAccessChecker(modelType, modelId, userId)
        }
    }

    @PostConstruct
    fun configurePermissions() {
        registrationService.registerActions(*DiscussionAction.entries.toTypedArray())
        registrationService.registerResources(*DiscussionResource.entries.toTypedArray())

        val config = definePermissions {
            role(SystemRole.USER) {}

            role(SystemRole.ADMIN) {
                can(
                        DiscussionAction.LIST,
                        DiscussionAction.CREATE,
                        DiscussionAction.VIEW,
                        DiscussionAction.UPDATE,
                        DiscussionAction.DELETE,
                    )
                    .on(DiscussionResource.DISCUSSION)
                    .all()

                can(DiscussionAction.REACT).on(DiscussionResource.REACTION).all()
            }

            role(DiscussionRole.PARTICIPANT) {
                can(DiscussionAction.LIST, DiscussionAction.VIEW, DiscussionAction.CREATE)
                    .on(DiscussionResource.DISCUSSION)

                can(DiscussionAction.REACT).on(DiscussionResource.REACTION)
            }

            role(DiscussionRole.CREATOR) {
                can(DiscussionAction.UPDATE, DiscussionAction.DELETE)
                    .on(DiscussionResource.DISCUSSION)
            }
        }

        permissionService.applyConfiguration(config)
    }
}
