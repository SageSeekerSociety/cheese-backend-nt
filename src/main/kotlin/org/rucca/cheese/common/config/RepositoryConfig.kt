package org.rucca.cheese.common.config

import org.rucca.cheese.common.repository.CursorPagingRepositoryImpl
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(
    basePackages = ["org.rucca.cheese"],
    repositoryBaseClass = CursorPagingRepositoryImpl::class,
)
class RepositoryConfig
