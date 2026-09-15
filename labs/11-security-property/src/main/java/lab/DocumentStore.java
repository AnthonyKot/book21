package lab;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;
@Component
public class DocumentStore {
    private final JdbcTemplate jdbc;
    final AtomicInteger bodyLoads=new AtomicInteger();
    public DocumentStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public Document read(String id,String tenant){
        bodyLoads.incrementAndGet();
        var rows=jdbc.query("SELECT summary,body FROM document WHERE id=? AND tenant=?",(rs,n)->new Document(rs.getString(1),rs.getString(2)),id,tenant);
        if(rows.isEmpty())throw new ResponseStatusException(NOT_FOUND);return rows.getFirst();
    }
    public record Document(String summary,String content){}
    public void requireDownload(String id,String tenant){
        Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM document WHERE id=? AND tenant=? AND archived=FALSE",Integer.class,id,tenant);
        if(n==null||n!=1)throw new ResponseStatusException(NOT_FOUND);
    }
    public void archive(String id,String tenant){if(jdbc.update("UPDATE document SET archived=TRUE WHERE id=? AND tenant=?",id,tenant)!=1)throw new ResponseStatusException(NOT_FOUND);}
    public void reset(){jdbc.update("UPDATE document SET archived=FALSE");bodyLoads.set(0);}
}
