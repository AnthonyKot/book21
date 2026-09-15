package lab;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Passes against the guided repair. Negative control: run with -Dtest.hardened=false so the
 * application is permissive while these assertions still expect the repair.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"lab.fixture-port=0", "lab.hardened=${test.hardened:true}"})
class ParserRepairTest extends PreviewCases {
    @Override
    boolean expectHardened() {
        return true;
    }
}
