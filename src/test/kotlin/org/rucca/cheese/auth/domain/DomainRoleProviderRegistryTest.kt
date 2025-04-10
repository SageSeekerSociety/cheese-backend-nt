package org.rucca.cheese.auth.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.rucca.cheese.auth.core.Domain
import org.rucca.cheese.auth.core.Role
import org.springframework.context.ApplicationContext

@ExtendWith(MockitoExtension::class)
@DisplayName("Domain Role Provider Registry Tests")
class DomainRoleProviderRegistryTest {

    // Test domains and roles
    companion object {
        private val testDomain1 =
            object : Domain {
                override val name: String = "test1"
            }

        private val testDomain2 =
            object : Domain {
                override val name: String = "test2"
            }
    }

    private enum class TestRole(override val roleId: String) : Role {
        ADMIN("admin"),
        USER("user");

        override val domain: Domain? = null
    }

    // Mocks
    @Mock private lateinit var applicationContext: ApplicationContext

    @Mock private lateinit var provider1: DomainRoleProvider

    @Mock private lateinit var provider2: DomainRoleProvider

    // SUT
    private lateinit var registry: DomainRoleProviderRegistry

    @BeforeEach
    fun setUp() {
        // Setup domain providers
        registry = DomainRoleProviderRegistry(applicationContext)
    }

    @Test
    @DisplayName("Should initialize providers from application context")
    fun shouldInitializeProvidersFromApplicationContext() {
        `when`(provider1.domain).thenReturn(testDomain1)
        `when`(provider2.domain).thenReturn(testDomain2)

        // Arrange
        val providers = mapOf("provider1" to provider1, "provider2" to provider2)

        `when`(applicationContext.getBeansOfType(DomainRoleProvider::class.java))
            .thenReturn(providers)

        // Act
        registry.afterSingletonsInstantiated()

        // Assert
        assertThat(registry.getProvider(testDomain1.name)).isEqualTo(provider1)
        assertThat(registry.getProvider(testDomain2.name)).isEqualTo(provider2)
    }

    @Test
    @DisplayName("Should manually register provider")
    fun shouldManuallyRegisterProvider() {
        `when`(provider1.domain).thenReturn(testDomain1)

        // Act
        registry.registerProvider(provider1)

        // Assert
        assertThat(registry.getProvider(testDomain1.name)).isEqualTo(provider1)
    }

    @Test
    @DisplayName("Should prevent duplicate provider registration")
    fun shouldPreventDuplicateProviderRegistration() {
        `when`(provider1.domain).thenReturn(testDomain1)

        // Arrange
        registry.registerProvider(provider1)

        // Act & Assert
        assertThrows<IllegalArgumentException> { registry.registerProvider(provider1) }
    }

    @Test
    @DisplayName("Should get all providers")
    fun shouldGetAllProviders() {
        `when`(provider1.domain).thenReturn(testDomain1)
        `when`(provider2.domain).thenReturn(testDomain2)

        // Arrange
        registry.registerProvider(provider1)
        registry.registerProvider(provider2)

        // Act
        val allProviders = registry.getAllProviders()

        // Assert
        assertThat(allProviders).containsExactlyInAnyOrder(provider1, provider2)
    }

    @Test
    @DisplayName("Should return null for non-existent provider")
    fun shouldReturnNullForNonExistentProvider() {
        // Act
        val provider = registry.getProvider("nonexistent")

        // Assert
        assertThat(provider).isNull()
    }
}
