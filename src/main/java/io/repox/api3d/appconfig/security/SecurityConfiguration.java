package io.repox.api3d.appconfig.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // enables @PreAuthorize, @PostAuthorize, @PreFilter and @PostFilter
@Slf4j
public class SecurityConfiguration {

    private final SecurityConfigurationProperties props;
    private final String[] AUTH_PATHS = {
        "/rest/**", "/token/**", "/harvest/**", "/xrculture/**", "/xrculturehub/**", "/tools/**", "/samples/**"
    };

    public SecurityConfiguration(SecurityConfigurationProperties optionsPlaceholder) {
        this.props = optionsPlaceholder;
    }

    @Bean
    UserDetailsService mockUserDetailsService() {
        return new InMemoryUserDetailsManager(); // just an empty in mem user details service bean since we do not use
        // it anywhere
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChainForUnprotected(HttpSecurity http) throws Exception {
        return http.securityMatcher(AUTH_PATHS)
                .cors(Customizer.withDefaults())
                .sessionManagement(httpSecuritySessionManagementConfigurer ->
                        httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(RequestCacheConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> {
                    authorizationManagerRequestMatcherRegistry
                            .requestMatchers(AUTH_PATHS)
                            .permitAll();
                })
                .build();
    }

    @Bean
    @Order(4)
    public SecurityFilterChain filterChainForRemainingEndpoints(HttpSecurity http) throws Exception {
        return http.sessionManagement(httpSecuritySessionManagementConfigurer ->
                        httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .csrf(Customizer.withDefaults())
                .requestCache(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .build();
    }
}
