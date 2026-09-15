package lab;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
abstract class ReviewHttp {
    @Value("${local.server.port}") int port;
    @Autowired DocumentStore store;
    @Autowired PreviewService previews;
    HttpClient client;
    @BeforeEach void reset(){store.reset();previews.clear();client=HttpClient.newHttpClient();}
    @AfterEach void stop(){client.close();}
    HttpResponse<String> get(String user,String id,String route,String suffix)throws Exception{
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/documents/"+id+"/"+route+suffix));
        if(!user.isEmpty())b.header("Authorization","Basic "+Base64.getEncoder().encodeToString((user+":local-only").getBytes(StandardCharsets.UTF_8)));
        var r=client.send(b.GET().build(),HttpResponse.BodyHandlers.ofString());
        System.out.println(user+" "+id+"/"+route+" -> "+r.statusCode()+" body="+r.body()+" bodyLoads="+store.bodyLoads.get());return r;
    }
    HttpResponse<String> preview(String user,String id)throws Exception{return get(user,id,"preview","");}
    void noContent(HttpResponse<String> r){assertEquals(404,r.statusCode());assertFalse(r.body().contains("FULL-INVOICE"));assertFalse(r.body().contains("PRIVATE-SUMMARY"));}
}
