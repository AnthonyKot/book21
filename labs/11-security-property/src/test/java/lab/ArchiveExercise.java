package lab;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"lab.partition=true"})
class ArchiveExercise extends ReviewHttp {
    @Test void activeDownload()throws Exception{var r=get("alice","C-1001","download","");assertEquals(200,r.statusCode());assertEquals("CEDAR-FULL-INVOICE-CONTENT",r.body());}
    @Test void warmThenArchive()throws Exception{preview("alice","C-1001");store.archive("C-1001","cedar");noContent(get("alice","C-1001","download",""));assertEquals(1,store.bodyLoads.get());}
    @Test void archiveBeforeCacheFill()throws Exception{store.archive("C-1001","cedar");noContent(get("alice","C-1001","download",""));assertEquals(0,store.bodyLoads.get());}
    @Test void previewStillAllowedAfterArchive()throws Exception{store.archive("C-1001","cedar");assertEquals("CEDAR-PRIVATE-SUMMARY",preview("alice","C-1001").body());}
    @Test void foreignWarmDownloadDenied()throws Exception{preview("alice","C-1001");noContent(get("bob","C-1001","download",""));}
    @Test void unaffectedDocumentStillDownloads()throws Exception{store.archive("C-1001","cedar");assertEquals("CEDAR-SECOND-CONTENT",get("alice","C-1002","download","").body());}
}
