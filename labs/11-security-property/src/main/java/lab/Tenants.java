package lab;

import static org.springframework.http.HttpStatus.FORBIDDEN;

import java.security.Principal;
import org.springframework.web.server.ResponseStatusException;

/** Fixed mapping from authenticated principal to tenant. Never derived from request input. */
final class Tenants {
    private Tenants() {}

    static String of(Principal principal) {
        return switch (principal.getName()) {
            case "alice" -> "cedar";
            case "bob" -> "birch";
            default -> throw new ResponseStatusException(FORBIDDEN);
        };
    }
}
