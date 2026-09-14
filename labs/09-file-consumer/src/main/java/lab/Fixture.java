package lab;
import com.sun.net.httpserver.HttpServer;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class Fixture {
    static final String NOTE="SERVER-ONLY-SYNTHETIC-NOTE";
    final AtomicInteger httpHits=new AtomicInteger(), resolutions=new AtomicInteger();
    private final Path note;
    private final HttpServer server;
    public Fixture(@Value("${lab.fixture-port}") int port) throws IOException {
        note=Files.createTempFile("book21-note-", ".txt");Files.writeString(note,NOTE);
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",port),0);
        server.createContext("/", e -> {
            httpHits.incrementAndGet();
            String body=e.getRequestURI().getPath().equals("/invoice.dtd")
                ? "<!ENTITY note '"+NOTE+"'>" : NOTE;
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
            e.sendResponseHeaders(200,bytes.length);
            try(var out=e.getResponseBody()){out.write(bytes);}finally{e.close();}
        });server.start();
    }
    public String fileUri(){return note.toUri().toString();}
    public String httpUri(){return "http://127.0.0.1:"+server.getAddress().getPort()+"/note";}
    public String dtdUri(){return "http://127.0.0.1:"+server.getAddress().getPort()+"/invoice.dtd";}
    boolean permits(String uri){return fileUri().equals(uri)||httpUri().equals(uri)||dtdUri().equals(uri);}
    @PreDestroy void stop() throws IOException {server.stop(0);Files.deleteIfExists(note);}
}
