package lab;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

/** Local synthetic issuer ONLY. Keys are generated in memory and change on restart. */
@Component
class TokenFixtures {
    static final String ISSUER = "https://issuer.example.test";
    static final String AUDIENCE = "document-api";
    static final Set<String> KINDS = Set.of("valid", "billing", "no-audience", "expired",
            "future", "wrong-issuer", "wrong-key", "no-expiry", "unknown-user", "reset", "no-purpose",
            "no-subject", "blank-subject");
    private final KeyPair issuerKey = newKey();
    private final KeyPair otherKey = newKey();

    private static KeyPair newKey() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    RSAPublicKey publicKey() { return (RSAPublicKey) issuerKey.getPublic(); }

    String issue(String kind) {
        if (!KINDS.contains(kind)) throw new IllegalArgumentException("Unknown fixture");
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .issuer(kind.equals("wrong-issuer") ? "https://other.example.test" : ISSUER)
                .issueTime(Date.from(kind.equals("expired") ? now.minusSeconds(1200) : now.minusSeconds(10)))
                .notBeforeTime(Date.from(kind.equals("future") ? now.plusSeconds(600)
                        : kind.equals("expired") ? now.minusSeconds(1200) : now.minusSeconds(10)));
        if (!kind.equals("no-subject"))
            claims.subject(kind.equals("unknown-user") ? "mallory" : kind.equals("blank-subject") ? " " : "alice");
        if (!kind.equals("no-audience"))
            claims.audience(List.of(kind.equals("billing") ? "billing-api" : AUDIENCE));
        if (!kind.equals("no-expiry"))
            claims.expirationTime(Date.from(kind.equals("expired") ? now.minusSeconds(600)
                    : kind.equals("future") ? now.plusSeconds(900) : now.plusSeconds(300)));
        if (!kind.equals("no-purpose"))
            claims.claim("token_use", kind.equals("reset") ? "password-reset" : "api-access");
        // The reset fixture deliberately carries the document audience (see essay 5).
        try {
            var token = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
            KeyPair key = kind.equals("wrong-key") ? otherKey : issuerKey;
            token.sign(new RSASSASigner((RSAPrivateKey) key.getPrivate()));
            return token.serialize();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
