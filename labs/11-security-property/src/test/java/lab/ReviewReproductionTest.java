package lab;

import org.springframework.boot.test.context.SpringBootTest;

/** Passes while the cache is keyed by document only: these assertions describe the flaw. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"lab.partition=${test.partition:false}"})
class ReviewReproductionTest extends PreviewCases {
    @Override
    boolean expectPartitioned() {
        return false;
    }
}
