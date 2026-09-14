package lab;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class FetchService {
    private final Destinations destinations;
    private final HttpClient client;
    public FetchService(Destinations destinations,@Value("${lab.follow-redirects}") boolean follow) {
        this.destinations=destinations;
        client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
            .followRedirects(follow?HttpClient.Redirect.ALWAYS:HttpClient.Redirect.NEVER).build();
    }
    URI allowed(String text) {
        final URI uri;
        try { uri=URI.create(text); } catch(IllegalArgumentException e) { throw denied(); }
        URI partner=URI.create(destinations.partnerUrl());
        if (!"http".equals(uri.getScheme()) || !"127.0.0.1".equals(uri.getHost())
                || uri.getPort()!=partner.getPort() || uri.getRawUserInfo()!=null
                || uri.getRawFragment()!=null) throw denied();
        return uri;
    }
    static ResponseStatusException denied() { return new ResponseStatusException(HttpStatus.FORBIDDEN,"destination denied"); }
    HttpResponse<String> send(URI uri) {
        try {
            // New request, no incoming cookies, Authorization or caller-selected headers.
            return client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(2)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
        } catch(IOException|IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY); }
    }
    String requireSuccess(HttpResponse<String> result) {
        if(result.statusCode()!=200) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"upstream not 200");
        return result.body();
    }
    public String fetch(String url) { return requireSuccess(send(allowed(url))); }
    public String followOne(String url) {
        // Unfinished practice: implement one validated redirect, keeping automatic following off.
        return fetch(url);
    }
}
