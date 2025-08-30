package org.rucca.cheese.project.repositories

import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.project.models.ProjectMemberRole
import org.rucca.cheese.project.models.ProjectMembership

interface ProjectMembershipRepository : CursorPagingRepository<ProjectMembership, IdType> {
    interface ProjectMembershipRoleOnly {
        val role: ProjectMemberRole
    }

    fun findRoleByProjectIdAndUserId(projectId: IdType, userId: IdType): ProjectMembershipRoleOnly?

    fun findAllByProjectId(projectId: IdType): List<ProjectMembership>
}
