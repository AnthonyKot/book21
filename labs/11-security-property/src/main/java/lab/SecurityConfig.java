package lab;
import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
    @Bean UserDetailsService users() {
        return new InMemoryUserDetailsManager(
            User.withUsername("alice").password("{noop}local-only").roles("CEDAR").build(),
            User.withUsername("bob").password("{noop}local-only").roles("BIRCH").build());
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        // CSRF defaults retained for all POSTs. Basic auth is a loopback fixture convenience.
        return http.authorizeHttpRequests(a -> a.anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults()).build();
    }
}
