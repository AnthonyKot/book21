package lab;
import java.security.Principal;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.FORBIDDEN;
final class Tenants {
    static String of(Principal principal){return switch(principal.getName()){
        case "alice" -> "cedar";case "bob" -> "birch";
        default -> throw new ResponseStatusException(FORBIDDEN);
    };}
}
