package lab;

import org.springframework.boot.test.context.SpringBootTest;

/** Passes while the preview parser is permissive: these assertions describe the flaw. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"lab.fixture-port=0", "lab.hardened=${test.hardened:false}"})
class ParserReproductionTest extends PreviewCases {
    @Override
    boolean expectHardened() {
        return false;
    }
}
