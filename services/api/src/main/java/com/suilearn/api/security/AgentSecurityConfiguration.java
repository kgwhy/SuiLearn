package com.suilearn.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableConfigurationProperties(AgentAuthProperties.class)
public class AgentSecurityConfiguration {
    @Bean
    LearnerTokenRegistry learnerTokenRegistry(AgentAuthProperties properties, ObjectMapper objectMapper) {
        return LearnerTokenRegistry.fromJson(properties.getTokens(), objectMapper);
    }

    @Bean
    BearerLearnerTokenFilter bearerLearnerTokenFilter(LearnerTokenRegistry registry) {
        return new BearerLearnerTokenFilter(registry);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AgentAuthProperties properties,
                                            BearerLearnerTokenFilter bearerFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(login -> login.disable())
            .logout(logout -> logout.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (properties.isEnabled()) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v2/agent/**").authenticated()
                    .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                    (request, response, exception) -> response.sendError(401, "Agent authentication required")))
                .addFilterBefore(bearerFilter, UsernamePasswordAuthenticationFilter.class);
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }
        return http.build();
    }
}
