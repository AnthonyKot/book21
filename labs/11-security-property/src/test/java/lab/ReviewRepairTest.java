package lab;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Passes against the guided repair. Negative control: run with -Dtest.partition=false so the
 * application keeps the shared key while these assertions still expect the repair.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"lab.partition=${test.partition:true}"})
class ReviewRepairTest extends PreviewCases {
    @Override
    boolean expectPartitioned() {
        return true;
    }
}
