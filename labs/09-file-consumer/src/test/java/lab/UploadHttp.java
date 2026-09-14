package lab;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
abstract class UploadHttp {
    @Value("${local.server.port}") int port;
    @Autowired Fixture fixture;
    @Autowired InvoiceParser parser;
    @Autowired UploadStore store;
    @Autowired SummaryWorker worker;
    HttpClient client;String token;
    @BeforeEach void start()throws Exception {
        client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        var r=client.send(request("/csrf",true).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,r.statusCode());var m=Pattern.compile("\"token\":\"([^\"]+)\"").matcher(r.body());assertTrue(m.find());token=m.group(1);
        fixture.httpHits.set(0);fixture.resolutions.set(0);
    }
    @AfterEach void stop(){client.close();}
    HttpRequest.Builder request(String path,boolean auth){
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));
        if(auth)b.header("Authorization","Basic "+Base64.getEncoder().encodeToString("alice:local-only".getBytes(StandardCharsets.UTF_8)));
        return b;
    }
    HttpResponse<String> post(String path,byte[] body,String type,boolean csrf,boolean auth)throws Exception {
        var b=request(path,auth).header("Content-Type",type);
        if(csrf)b.header("X-CSRF-TOKEN",token);
        var r=client.send(b.POST(HttpRequest.BodyPublishers.ofByteArray(body)).build(),HttpResponse.BodyHandlers.ofString());
        System.out.println(path+" status="+r.statusCode()+" httpHits="+fixture.httpHits.get()+" resolutions="+fixture.resolutions.get()+" body="+r.body());return r;
    }
    String upload(byte[] bytes)throws Exception {
        var r=post("/api/uploads",bytes,"application/xml",true,true);assertEquals(201,r.statusCode());return r.body();
    }
    String upload(String xml)throws Exception{return upload(xml.getBytes(StandardCharsets.UTF_8));}
    HttpResponse<String> preview(String id)throws Exception{return post("/api/uploads/"+id+"/preview",new byte[0],"application/xml",true,true);}
    static String entity(String uri){return "<!DOCTYPE invoice [<!ENTITY note SYSTEM '"+uri+"'>]><invoice><title>&note;</title></invoice>";}
    void rejected(String xml)throws Exception {assertEquals(422,preview(upload(xml)).statusCode());assertEquals(0,fixture.httpHits.get());}
}
