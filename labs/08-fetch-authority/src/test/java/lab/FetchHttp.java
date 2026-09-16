package lab;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import static org.junit.jupiter.api.Assertions.*;
abstract class FetchHttp {
    @Value("${local.server.port}") int port;
    @Autowired Destinations destinations;
    @BeforeEach void resetCounters() {destinations.partnerHits.set(0);destinations.mirrorHits.set(0);destinations.internalHits.set(0);destinations.sawAuthorization.set(false);}
    HttpResponse<String> call(String endpoint,String url,boolean auth) throws Exception {
        var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+endpoint+"?url="+URLEncoder.encode(url,StandardCharsets.UTF_8)));
        if(auth) req.header("Authorization","Basic "+Base64.getEncoder().encodeToString("alice:local-only".getBytes(StandardCharsets.UTF_8)));
        var r=HttpClient.newHttpClient().send(req.GET().build(),HttpResponse.BodyHandlers.ofString());
        System.out.println(endpoint+" target="+url+" status="+r.statusCode()+" internalHits="+destinations.internalHits.get()+" body="+r.body());
        return r;
    }
    HttpResponse<String> fetch(String url) throws Exception {return call("/api/fetch",url,true);}
    void deniedWithoutContact(String url) throws Exception {
        assertEquals(403,fetch(url).statusCode());assertEquals(0,destinations.partnerHits.get());assertEquals(0,destinations.internalHits.get());
    }
}
