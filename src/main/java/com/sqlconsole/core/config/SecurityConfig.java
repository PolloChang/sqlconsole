package com.sqlconsole.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            (requests) ->
                requests
                    .requestMatchers("/admin/**", "/api/users/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/console", "/api/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .formLogin((form) -> form.defaultSuccessUrl("/console", true))
        .logout((logout) -> logout.permitAll())
        .csrf(csrf -> csrf.disable()); // 關閉 CSRF 以方便 jQuery POST

    return http.build();
  }
}
