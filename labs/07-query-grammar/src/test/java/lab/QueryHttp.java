package lab;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import static org.junit.jupiter.api.Assertions.*;
abstract class QueryHttp {
    @Value("${local.server.port}") int port;
    private final HttpClient client = HttpClient.newHttpClient();
    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    HttpResponse<String> get(String path, boolean auth) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));
        if(auth) builder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString("alice:local-only".getBytes(StandardCharsets.UTF_8)));
        var response=client.send(builder.GET().build(),HttpResponse.BodyHandlers.ofString());
        System.out.println("GET "+path+" -> "+response.statusCode()+" "+response.body());
        return response;
    }
    HttpResponse<String> search(String title) throws Exception { return get("/api/search?title="+enc(title),true); }
    void ids(HttpResponse<String> response, String... expected) {
        assertEquals(200,response.statusCode(),response.body());
        var actual=new ArrayList<String>();
        var m=Pattern.compile("\"id\":\"([^\"]+)\"").matcher(response.body());
        while(m.find()) actual.add(m.group(1));
        assertEquals(Arrays.asList(expected),actual,response.body());
    }
}
