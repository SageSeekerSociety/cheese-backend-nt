package org.rucca.cheese.team

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.Avatar
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@SQLRestriction("deleted_at IS NULL")
class Team(
        @Column(nullable = false) val name: String? = null,
        @Column(nullable = false) val description: String? = null,
        @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val avatar: Avatar? = null,
) : BaseEntity()

interface TeamRepository : JpaRepository<Team, IdType>
