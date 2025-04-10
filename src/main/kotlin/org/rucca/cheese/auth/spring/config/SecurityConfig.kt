package org.rucca.cheese.auth.spring.config

import org.rucca.cheese.auth.spring.CustomAuth0JwtDecoder
import org.rucca.cheese.auth.spring.CustomJwtAuthenticationConverter
import org.rucca.cheese.auth.spring.handlers.CustomAccessDeniedHandler
import org.rucca.cheese.auth.spring.handlers.CustomAuthenticationEntryPoint
import org.rucca.cheese.common.config.ActuatorCredentialsProperties
import org.rucca.cheese.common.config.ApplicationConfig
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
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(ActuatorCredentialsProperties::class)
class SecurityConfig(
    private val applicationConfig: ApplicationConfig,
    private val actuatorCredentialsProperties: ActuatorCredentialsProperties,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(SecurityConfig::class.java)

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        configuration.allowedOrigins = listOf(applicationConfig.corsOrigin)
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders =
            listOf("Authorization", "Cache-Control", "Content-Type", "X-Requested-With", "Accept")
        configuration.exposedHeaders = listOf("Authorization", "Content-Disposition")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)

        return source
    }

    @Bean
    fun actuatorUserDetailsService(passwordEncoder: PasswordEncoder): InMemoryUserDetailsManager {
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

        http
            .securityMatcher(actuatorMatcher)
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers(healthMatcher)
                    .permitAll()
                    .anyRequest()
                    .hasRole(actuatorCredentialsProperties.role)
            }
            .httpBasic(Customizer.withDefaults())
            .csrf { it.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

        return http.build()
    }

    @Bean
    @Order(10)
    fun apiSecurityFilterChain(
        http: HttpSecurity,
        jwtDecoder: CustomAuth0JwtDecoder,
        customJwtAuthenticationConverter: CustomJwtAuthenticationConverter,
    ): SecurityFilterChain {
        logger.debug("Configuring API security filter chain")

        val apiMatcher = AntPathRequestMatcher("/**")

        http
            .securityMatcher(apiMatcher)
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers(AntPathRequestMatcher("/actuator/**"))
                    .permitAll()
                    .requestMatchers(
                        AntPathRequestMatcher("/users/auth/**"),
                        AntPathRequestMatcher("/public/**"),
                    )
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    // Use our custom decoder (which uses auth0 verifier)
                    jwt.decoder(jwtDecoder)
                    // Use our custom converter to create UserPrincipalAuthenticationToken
                    jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter)
                }
            }
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint(customAuthenticationEntryPoint)
                    .accessDeniedHandler(customAccessDeniedHandler)
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .csrf { it.disable() }

        return http.build()
    }
}
