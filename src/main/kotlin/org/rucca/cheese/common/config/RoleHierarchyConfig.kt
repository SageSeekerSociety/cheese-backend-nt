package org.rucca.cheese.common.config

import jakarta.annotation.PostConstruct
import org.rucca.cheese.auth.core.SystemRole
import org.rucca.cheese.auth.dsl.applyHierarchy
import org.rucca.cheese.auth.dsl.defineRoleHierarchy
import org.rucca.cheese.auth.hierarchy.GraphRoleHierarchy
import org.springframework.stereotype.Component

@Component
class RoleHierarchyConfig(private val roleHierarchy: GraphRoleHierarchy) {
    @PostConstruct
    fun configureRoleHierarchy() {
        val hierarchyConfig = defineRoleHierarchy {
            role(SystemRole.SUPER_ADMIN) { inheritsFrom(SystemRole.ADMIN) }

            role(SystemRole.ADMIN) { inheritsFrom(SystemRole.USER) }

            role(SystemRole.MODERATOR) { inheritsFrom(SystemRole.USER) }

            role(SystemRole.USER) { inheritsFrom(SystemRole.GUEST) }

            role(SystemRole.GUEST)
        }

        roleHierarchy.applyHierarchy(hierarchyConfig)
    }
}
