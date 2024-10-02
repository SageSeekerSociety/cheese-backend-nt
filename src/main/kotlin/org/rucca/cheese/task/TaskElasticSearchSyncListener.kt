package org.rucca.cheese.task

import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable

fun Task.toElasticSearchDocument(): TaskElasticSearch {
    val task = this
    return TaskElasticSearch().apply {
        id = task.id
        name = task.name
    }
}

@Configurable
class TaskElasticSearchSyncListener {
    // It's not easy to inject dependency into JPA EntityListener, because it's not managed by
    // Spring.
    // Fortunately, I'm not the first person who has this problem.
    // Someone has already found a solution to this problem, and that's it:
    // See:
    // https://stackoverflow.com/questions/12155632/injecting-a-spring-dependency-into-a-jpa-entitylistener
    @Autowired lateinit var taskElasticSearchRepositoryProvider: ObjectProvider<TaskElasticSearchRepository>

    @PrePersist
    fun prePersist(task: Task) {
        val taskElasticSearchRepository = taskElasticSearchRepositoryProvider.getObject()
        taskElasticSearchRepository.save(task.toElasticSearchDocument())
    }

    @PreUpdate
    fun preUpdate(task: Task) {
        val taskElasticSearchRepository = taskElasticSearchRepositoryProvider.getObject()
        if (task.deletedAt == null) {
            taskElasticSearchRepository.save(task.toElasticSearchDocument())
        } else {
            taskElasticSearchRepository.delete(task.toElasticSearchDocument())
        }
    }
}
