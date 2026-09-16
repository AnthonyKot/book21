package lab;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {
    @Bean
    JwtDecoder jwtDecoder(TokenFixtures fixtures, @Value("${lab.audience-check:true}") boolean checkAudience) {
        var decoder = NimbusJwtDecoder.withPublicKey(fixtures.publicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256).build();
        List<OAuth2TokenValidator<Jwt>> checks = new ArrayList<>();
        checks.add(JwtValidators.createDefaultWithIssuer(TokenFixtures.ISSUER));
        // This API profile requires exp and a nonempty subject, not just validity if present.
        checks.add(new JwtClaimValidator<Instant>("exp", value -> value != null));
        checks.add(new JwtClaimValidator<String>("sub", value -> value != null && !value.isBlank()));
        if (checkAudience) {
            checks.add(new JwtClaimValidator<List<String>>("aud",
                    audiences -> audiences != null && audiences.contains(TokenFixtures.AUDIENCE)));
        }
        // This validator list is the API's acceptance profile. The practice task in
        // practice/05-token-worksheet.md states what it must satisfy.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(checks));
        return decoder;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // This fixture accepts header bearer tokens only, no cookie or Basic authentication.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.requestMatchers("/lab/tokens/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }
}
