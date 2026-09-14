package lab;
import java.net.http.HttpResponse;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"lab.gates=true","lab.conditional=true"})
class WorkerExercise extends CreditHttp {
    @Test void ordinaryWorker()throws Exception{assertEquals(200,client.send(request("C-1001","worker","","",true,true),HttpResponse.BodyHandlers.ofString()).statusCode());state(1000,1);}
    @Test void duplicateWorker(){worker.apply("C-1001","","");assertEquals(409,assertThrows(ResponseStatusException.class,()->worker.apply("C-1001","","")).getStatusCode().value());state(1000,1);}
    @Test void rollbackClaim(){assertThrows(IllegalStateException.class,()->worker.apply("C-1001","","claim"));state(0,0);}
    @Test void rollbackBalance(){assertThrows(IllegalStateException.class,()->worker.apply("C-1001","","balance"));state(0,0);}
    @Test void retryAfterFailure(){assertThrows(IllegalStateException.class,()->worker.apply("C-1001","","claim"));assertDoesNotThrow(()->worker.apply("C-1001","",""));state(1000,1);}
    @Test void overlappingWorkers()throws Exception {
        try(var executor=Executors.newFixedThreadPool(2)){
            Future<Integer> a=executor.submit(()->run("A"));Future<Integer>b=executor.submit(()->run("B"));
            try {gate.awaitBoth();gate.release("B");assertEquals(200,b.get(10,TimeUnit.SECONDS));gate.release("A");assertEquals(409,a.get(10,TimeUnit.SECONDS));state(1000,1);}
            finally {gate.release("A");gate.release("B");}
        }
    }
    int run(String lane){try{worker.apply("C-1001",lane,"");return 200;}catch(ResponseStatusException e){return e.getStatusCode().value();}}
}
