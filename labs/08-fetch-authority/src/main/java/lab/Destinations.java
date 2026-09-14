package lab;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class Destinations {
    private final HttpServer partner, internal;
    final AtomicInteger partnerHits=new AtomicInteger(), internalHits=new AtomicInteger();
    final java.util.concurrent.atomic.AtomicBoolean sawAuthorization = new java.util.concurrent.atomic.AtomicBoolean();
    public Destinations(@Value("${lab.partner-port}") int partnerPort,
                        @Value("${lab.internal-port}") int internalPort) throws IOException {
        partner=HttpServer.create(new InetSocketAddress("127.0.0.1",partnerPort),0);
        internal=HttpServer.create(new InetSocketAddress("127.0.0.1",internalPort),0);
        internal.createContext("/", e -> {
            internalHits.incrementAndGet();
            if (e.getRequestHeaders().containsKey("Authorization")) sawAuthorization.set(true);
            reply(e,200,"INTERNAL-SYNTHETIC-REPORT");
        });
        partner.createContext("/", e -> {
            partnerHits.incrementAndGet();
            if (e.getRequestHeaders().containsKey("Authorization")) sawAuthorization.set(true);
            switch(e.getRequestURI().getPath()) {
                case "/document" -> reply(e,200,"PARTNER-INVOICE-C-1001");
                case "/to-internal" -> redirect(e,internalUrl());
                case "/to-document" -> redirect(e,"/document");
                case "/loop" -> redirect(e,"/loop");
                case "/chain" -> redirect(e,"/to-document");
                default -> reply(e,503,"fixture unavailable");
            }
        });
        internal.start(); partner.start();
    }
    public String partnerUrl() { return "http://127.0.0.1:"+partner.getAddress().getPort(); }
    public String internalUrl() { return "http://127.0.0.1:"+internal.getAddress().getPort()+"/report"; }
    private static void redirect(HttpExchange e,String location) throws IOException {
        e.getResponseHeaders().set("Location",location);e.sendResponseHeaders(302,-1);e.close();
    }
    private static void reply(HttpExchange e,int status,String text) throws IOException {
        byte[] body=text.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type","text/plain; charset=utf-8");
        e.sendResponseHeaders(status,body.length);
        try(var out=e.getResponseBody()){out.write(body);}finally{e.close();}
    }
    @PreDestroy void stop() { partner.stop(0);internal.stop(0); }
}
