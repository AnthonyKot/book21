package lab;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
    @Bean UserDetailsService users() {
        // Synthetic local credentials, never a production password store.
        return new InMemoryUserDetailsManager(User.withUsername("alice")
                .password("{noop}local-only").roles("CEDAR").build());
    }
    @Bean SecurityFilterChain security(HttpSecurity http,
            @Value("${lab.csrf}") boolean csrf) throws Exception {
        http.authorizeHttpRequests(a -> a.anyRequest().authenticated())
            .formLogin(f -> f.defaultSuccessUrl("/", true));
        if (csrf) http.csrf(Customizer.withDefaults());
        else http.csrf(c -> c.disable());
        // No cross-origin response access is granted. No CSP in this teaching fixture.
        return http.build();
    }
}
