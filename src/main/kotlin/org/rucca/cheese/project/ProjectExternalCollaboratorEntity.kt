package org.rucca.cheese.project

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "project_id"), Index(columnList = "user_id")])
class ProjectExternalCollaborator(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var project: Project? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var user: User? = null,
) : BaseEntity()

interface ProjectExternalCollaboratorRepository :
    JpaRepository<ProjectExternalCollaborator, IdType> {}
