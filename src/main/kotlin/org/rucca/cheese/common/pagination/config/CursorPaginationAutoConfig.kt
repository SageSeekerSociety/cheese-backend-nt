package org.rucca.cheese.common.pagination.config

import jakarta.annotation.PostConstruct
import org.rucca.cheese.common.pagination.encoding.CursorConfig
import org.rucca.cheese.common.pagination.repository.CursorPagingRepositoryImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * Autoconfiguration for cursor-based pagination.
 *
 * This configuration class enables cursor-based pagination in Spring Data repositories and
 * configures cursor encoding options.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = ["org.rucca.cheese"],
    repositoryBaseClass = CursorPagingRepositoryImpl::class,
)
class CursorPaginationAutoConfig {

    @Value("\${pagination.cursor.encryption.enabled:false}")
    private var encryptionEnabled: Boolean = false

    @Value("\${pagination.cursor.encryption.key:}") private var encryptionKey: String = ""

    /** Initialize cursor pagination settings. */
    @PostConstruct
    fun init() {
        if (encryptionEnabled && encryptionKey.isNotEmpty()) {
            CursorConfig.enableEncryption(encryptionKey)
        } else {
            CursorConfig.disableEncryption()
        }
    }
}
