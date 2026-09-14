package lab;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.xml.sax.SAXException;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"lab.fixture-port=0","lab.hardened=${test.hardened:true}"})
class ParserRepairTest extends UploadHttp {
    private static final boolean HARDENED=true;
    private void entityCase(String xml,boolean network)throws Exception {
        String id=upload(xml);assertEquals(0,fixture.resolutions.get());assertEquals(0,fixture.httpHits.get());
        var r=preview(id);assertEquals(HARDENED?422:200,r.statusCode());
        assertEquals(HARDENED?0:1,fixture.resolutions.get());assertEquals(!HARDENED&&network?1:0,fixture.httpHits.get());
        if(!HARDENED)assertEquals(Fixture.NOTE,r.body());
    }
    @Test void ordinaryInvoice()throws Exception{assertEquals("Cedar & Sons",preview(upload("<invoice><title>Cedar &amp; Sons</title></invoice>")).body());}
    @Test void cdataText()throws Exception{assertEquals("A < B",preview(upload("<invoice><title><![CDATA[A < B]]></title></invoice>")).body());}
    @Test void fileEntity()throws Exception{entityCase(entity(fixture.fileUri()),false);}
    @Test void httpEntity()throws Exception{entityCase(entity(fixture.httpUri()),true);}
    @Test void externalDtd()throws Exception{entityCase("<!DOCTYPE invoice SYSTEM '"+fixture.dtdUri()+"'><invoice><title>&note;</title></invoice>",true);}
    @Test void internalDtd()throws Exception{
        var r=preview(upload("<!DOCTYPE invoice [<!ENTITY note 'Cedar'>]><invoice><title>&note;</title></invoice>"));
        assertEquals(HARDENED?422:200,r.statusCode());assertEquals(0,fixture.resolutions.get());
    }
    @Test void utf16Entity()throws Exception{
        String xml="<?xml version='1.0' encoding='UTF-16'?>"+entity(fixture.fileUri());
        String id=upload(xml.getBytes(StandardCharsets.UTF_16));var r=preview(id);
        assertEquals(HARDENED?422:200,r.statusCode());if(!HARDENED)assertEquals(Fixture.NOTE,r.body());
        assertEquals(HARDENED?0:1,fixture.resolutions.get());
    }
    @Test void malformed()throws Exception{rejected("<invoice><title>broken</invoice>");}
    @Test void duplicateTitle()throws Exception{rejected("<invoice><title>A</title><title>B</title></invoice>");}
    @Test void longTitle()throws Exception{rejected("<invoice><title>"+"x".repeat(121)+"</title></invoice>");}
    @Test void xincludeStaysInactive()throws Exception{rejected("<invoice xmlns:xi='http://www.w3.org/2001/XInclude'><title><xi:include href='"+fixture.httpUri()+"' parse='text'/></title></invoice>");assertEquals(0,fixture.resolutions.get());}
    @Test void wrongMediaType()throws Exception{assertEquals(415,post("/api/uploads","hello".getBytes(),"text/plain",true,true).statusCode());}
    @Test void oversizedUpload()throws Exception{assertEquals(413,post("/api/uploads",new byte[8193],"application/xml",true,true).statusCode());}
    @Test void exactUploadLimit()throws Exception{
        String xml="<invoice><title>Cedar</title></invoice>";xml+=" ".repeat(8192-xml.length());
        assertEquals("Cedar",preview(upload(xml)).body());
    }
    @Test void unknownId()throws Exception{assertEquals(404,preview("00000000-0000-0000-0000-000000000000").statusCode());}
    @Test void csrfRequired()throws Exception{assertEquals(401,post("/api/uploads",new byte[0],"application/xml",false,true).statusCode());}
    @Test void authenticationRequired()throws Exception{assertEquals(401,post("/api/uploads",new byte[0],"application/xml",true,false).statusCode());}
    @Test void parserDepthLimit()throws Exception{
        String allowed="<n>".repeat(32)+"</n>".repeat(32),denied="<n>".repeat(33)+"</n>".repeat(33);
        assertNotNull(parser.builder(HARDENED).parse(new ByteArrayInputStream(allowed.getBytes())));
        SAXException e=assertThrows(SAXException.class,()->parser.builder(HARDENED).parse(new ByteArrayInputStream(denied.getBytes())));
        assertTrue(e.getMessage().contains("32"),e.getMessage());
    }
}
