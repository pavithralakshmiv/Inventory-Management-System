package com.inventory.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final LogService logService;
    private final ObjectMapper objectMapper;

    public SecurityConfig(LogService logService, ObjectMapper objectMapper) {
        this.logService = logService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/register.html", "/logout.html", "/css/**", "/js/**",
                                "/images/**", "/error", "/favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/logout").permitAll()
                        .requestMatchers("/users.html", "/logs.html", "/reports.html").hasRole("ADMIN")
                        .requestMatchers("/api/users/**", "/api/logs/**", "/api/reports/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/settings").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/purchases/*/cancel").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/index.html")
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            logService.log(authentication.getName(), "LOGIN", "User logged in");
                            String role = authentication.getAuthorities().stream().findFirst()
                                    .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("");
                            writeJson(response, HttpServletResponse.SC_OK, Map.of("message", "Login successful", "role", role));
                        })
                        .failureHandler((request, response, exception) -> writeJson(response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                Map.of("message", "Invalid email or password, or the account is inactive.")))
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .addLogoutHandler((request, response, authentication) -> {
                            if (authentication != null) {
                                logService.log(authentication.getName(), "LOGOUT", "User logged out");
                            }
                        })
                        .logoutSuccessHandler((request, response, authentication) ->
                                writeJson(response, HttpServletResponse.SC_OK, Map.of("message", "Logged out")))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (isApi(request)) {
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        Map.of("message", "Please log in to continue."));
                            } else {
                                response.sendRedirect("/index.html");
                            }
                        })
                        .accessDeniedHandler((request, response, deniedException) -> {
                            if (isApi(request)) {
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                                        Map.of("message", "You do not have permission to perform this action."));
                            } else {
                                response.sendRedirect("/dashboard.html?denied=1");
                            }
                        }))
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.migrateSession()));
        return http.build();
    }

    private boolean isApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }

    private void writeJson(HttpServletResponse response, int status, Map<String, String> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
