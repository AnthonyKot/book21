package lab;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"lab.fixture-port=0","lab.hardened=true"})
class SummaryExercise extends UploadHttp {
    private void deniedByWorker(String xml)throws Exception {
        String id=upload(xml);assertEquals(422,preview(id).statusCode());assertEquals(0,fixture.httpHits.get());
        // Call the actual later consumer directly; no queue timing is being simulated.
        ResponseStatusException e=assertThrows(ResponseStatusException.class,()->worker.summarize(id));
        assertEquals(422,e.getStatusCode().value());assertEquals(0,fixture.httpHits.get());assertEquals(0,fixture.resolutions.get());
    }
    @Test void ordinarySummary()throws Exception{
        String id=upload("<invoice><title>Quarterly invoice</title></invoice>");
        assertEquals("Quarterly invoice",worker.summarize(id));
        assertEquals(200,post("/api/uploads/"+id+"/summary",new byte[0],"application/xml",true,true).statusCode());
    }
    @Test void fileReferenceDenied()throws Exception{deniedByWorker(entity(fixture.fileUri()));}
    @Test void httpReferenceDenied()throws Exception{deniedByWorker(entity(fixture.httpUri()));}
    @Test void harmlessDtdAlsoDenied()throws Exception{deniedByWorker("<!DOCTYPE invoice [<!ENTITY note 'Cedar'>]><invoice><title>&note;</title></invoice>");}
    @Test void malformedStillDenied()throws Exception{deniedByWorker("<invoice>");}
}
