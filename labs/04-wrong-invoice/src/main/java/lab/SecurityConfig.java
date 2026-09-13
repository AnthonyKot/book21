package lab;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            // Lab simplification: an API called with curl, no browser session. CSRF is essay 6.
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    UserDetailsService users() {
        // Synthetic lab users with plain-text passwords. Never do this outside a local fixture.
        return new InMemoryUserDetailsManager(
            User.withUsername("alice").password("{noop}alice-pass").roles("USER").build(),
            User.withUsername("bob").password("{noop}bob-pass").roles("USER").build());
    }
}
