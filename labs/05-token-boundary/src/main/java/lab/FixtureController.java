package lab;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Unauthenticated token dispenser is a teaching tool, never a production issuer. */
@RestController
class FixtureController {
    private final TokenFixtures fixtures;
    FixtureController(TokenFixtures fixtures) { this.fixtures = fixtures; }

    @GetMapping(value = "/lab/tokens/{kind}", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> token(@PathVariable String kind) {
        return TokenFixtures.KINDS.contains(kind)
                ? ResponseEntity.ok(fixtures.issue(kind)) : ResponseEntity.notFound().build();
    }
}
