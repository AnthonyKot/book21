package lab;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
abstract class CreditHttp {
    @Value("${local.server.port}") int port;
    @Autowired RaceGate gate;
    @Autowired JdbcTemplate jdbc;
    @Autowired CreditWorker worker;
    HttpClient client;String token;
    @BeforeEach void start()throws Exception {
        gate.reset();jdbc.update("UPDATE credit SET applied=FALSE");jdbc.update("UPDATE account SET balance_cents=0");
        client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        var r=client.send(base("/csrf",true).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,r.statusCode());var m=Pattern.compile("\"token\":\"([^\"]+)\"").matcher(r.body());assertTrue(m.find());token=m.group(1);
    }
    @AfterEach void stop(){gate.release("A");gate.release("B");client.close();}
    HttpRequest.Builder base(String path,boolean auth){
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(java.time.Duration.ofSeconds(20));
        if(auth)b.header("Authorization","Basic "+Base64.getEncoder().encodeToString("alice:local-only".getBytes(StandardCharsets.UTF_8)));return b;
    }
    HttpRequest request(String id,String route,String lane,String fault,boolean csrf,boolean auth){
        var b=base("/api/credits/"+id+"/"+route,auth).header("X-Lab-Lane",lane).header("X-Lab-Fault",fault);
        if(csrf)b.header("X-CSRF-TOKEN",token);return b.POST(HttpRequest.BodyPublishers.noBody()).build();
    }
    int apply(String id,String fault)throws Exception{return client.send(request(id,"apply","",fault,true,true),HttpResponse.BodyHandlers.ofString()).statusCode();}
    CompletableFuture<HttpResponse<String>> send(String id,String lane){return client.sendAsync(request(id,"apply",lane,"",true,true),HttpResponse.BodyHandlers.ofString());}
    void state(int balance,int count){assertEquals(balance,jdbc.queryForObject("SELECT balance_cents FROM account WHERE tenant='cedar'",Integer.class));assertEquals(count,jdbc.queryForObject("SELECT COUNT(*) FROM credit WHERE tenant='cedar' AND applied=TRUE",Integer.class));}
    void race(String first,boolean conditional,boolean distinct)throws Exception {
        var a=send("C-1001","A");var b=send(distinct?"C-1002":"C-1001","B");
        try {
            gate.awaitBoth();gate.release(first);
            var winner=(first.equals("A")?a:b).get(10,TimeUnit.SECONDS);assertEquals(200,winner.statusCode());
            gate.release(first.equals("A")?"B":"A");
            var loser=(first.equals("A")?b:a).get(10,TimeUnit.SECONDS);
            System.out.println("ORDER "+first+" first: "+winner.statusCode()+","+loser.statusCode());
            assertEquals(conditional&&!distinct?409:200,loser.statusCode());state(conditional&&!distinct?1000:2000,distinct?2:1);
        }finally{gate.release("A");gate.release("B");CompletableFuture.allOf(a,b).get(10,TimeUnit.SECONDS);}
    }
}
