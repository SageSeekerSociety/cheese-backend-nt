package org.rucca.cheese.team

import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable

fun Team.toElasticSearchDocument(): TeamElasticSearch {
    val team = this
    return TeamElasticSearch().apply {
        id = team.id
        name = team.name
    }
}

@Configurable
class TeamElasticSearchSyncListener {
    // It's not easy to inject dependency into JPA EntityListener, because it's not managed by
    // Spring.
    // Fortunately, I'm not the first person who has this problem.
    // Someone has already found a solution to this problem, and that's it:
    // See:
    // https://stackoverflow.com/questions/12155632/injecting-a-spring-dependency-into-a-jpa-entitylistener
    @Autowired
    lateinit var teamElasticSearchRepositoryProvider: ObjectProvider<TeamElasticSearchRepository>

    @PrePersist
    fun prePersist(team: Team) {
        val teamElasticSearchRepository = teamElasticSearchRepositoryProvider.getObject()
        teamElasticSearchRepository.save(team.toElasticSearchDocument())
    }

    @PreUpdate
    fun preUpdate(team: Team) {
        val teamElasticSearchRepository = teamElasticSearchRepositoryProvider.getObject()
        if (team.deletedAt == null) {
            teamElasticSearchRepository.save(team.toElasticSearchDocument())
        } else {
            teamElasticSearchRepository.delete(team.toElasticSearchDocument())
        }
    }
}
