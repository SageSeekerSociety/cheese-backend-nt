package org.rucca.cheese.common.config

import org.springframework.boot.context.properties.EnableConfigurationProperties // Import this
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(ActuatorCredentialsProperties::class)
class ActuatorSecurityConfig(
    private val actuatorCredentialsProperties: ActuatorCredentialsProperties
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    // Define an in-memory user details service using configured credentials
    fun actuatorUserDetailsService(passwordEncoder: PasswordEncoder): InMemoryUserDetailsManager {
        // Properties are validated by @Validated on the properties class
        val user =
            User.withUsername(actuatorCredentialsProperties.username)
                .password(passwordEncoder.encode(actuatorCredentialsProperties.password))
                .roles(actuatorCredentialsProperties.role)
                .build()
        return InMemoryUserDetailsManager(user)
    }

    @Bean
    @Order(1)
    fun actuatorSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val actuatorMatcher = AntPathRequestMatcher("/actuator/**")
        val healthMatcher = AntPathRequestMatcher("/actuator/health")

        // Start configuration using http object directly
        http
            // Apply ONLY to actuator paths
            .securityMatcher(actuatorMatcher)
            // Configure authorization rules using lambda with HttpSecurity.authorizeHttpRequests
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers(healthMatcher)
                    .permitAll() // Allow anonymous access to health summary
                    .anyRequest()
                    .hasRole(actuatorCredentialsProperties.role) // Require role for others
            }
            // Configure HTTP Basic authentication (using Customizer or defaults)
            .httpBasic(Customizer.withDefaults()) // Use defaults for basic auth popup
            // Disable CSRF using lambda or shortcut
            .csrf { it.disable() }
            // Configure session management using lambda
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Set to stateless
            }

        // Build the filter chain
        return http.build()
    }

    @Bean
    @Order(10) // Lower priority for API
    fun apiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        // Match paths NOT starting with /actuator (effectively, everything else including '/')
        // Or be more specific if needed, e.g., AntPathRequestMatcher("/**") combined with
        // exclusions below
        val apiMatcher = AntPathRequestMatcher("/**") // Match everything initially

        http
            .securityMatcher(apiMatcher) // Apply to everything NOT caught by the actuator chain
            // IMPORTANT: Exclude actuator paths explicitly if using a broad matcher like "/**"
            // Although the @Order should handle it, explicit exclusion adds clarity.
            // This might not be strictly necessary with @Order but improves readability.
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers(AntPathRequestMatcher("/actuator/**"))
                    .permitAll()
                    .anyRequest()
                    .permitAll()
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .csrf { it.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

        return http.build()
    }
}
