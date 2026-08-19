package com.sample_generator.sample.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                HttpSecurity http) throws Exception {

                http
                        .csrf(csrf -> csrf.disable())

                        .authorizeHttpRequests(auth -> auth

                                .requestMatchers("/api/assets/download/**")
                                .permitAll()

                                .requestMatchers(
                                        "/login",
                                        "/css/**",
                                        "/js/**",
                                        "/images/**"
                                )
                                .permitAll()

                                .requestMatchers(
                                        "/admin/**",
                                        "/api/reports/all",
                                        "/api/admin/**"
                                )
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        "/dashboard",
                                        "/api/reports/**",
                                        "/api/assets/**",
                                        "/api/user/me",
                                        "/api/sample-reports/**"
                                )
                                .authenticated()

                                .anyRequest()
                                .authenticated()
                        )

                        .formLogin(form -> form

                                .loginPage("/login")

                                .loginProcessingUrl("/login")

                                .defaultSuccessUrl(
                                        "/dashboard",
                                        true
                                )

                                .failureUrl(
                                        "/login?error=true"
                                )

                                .permitAll()
                        )

                        .logout(logout -> logout

                                .logoutUrl("/logout")

                                .logoutSuccessUrl(
                                        "/login?logout=true"
                                )

                                .invalidateHttpSession(true)

                                .deleteCookies("JSESSIONID")

                                .permitAll()
                        );

                return http.build();
        }
}