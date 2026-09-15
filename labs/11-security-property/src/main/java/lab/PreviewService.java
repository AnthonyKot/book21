package lab;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class PreviewService {
    private final DocumentStore store;
    private final ConcurrentHashMap<Key,DocumentStore.Document> cache=new ConcurrentHashMap<>();
    @Value("${lab.partition}") boolean partition;
    public PreviewService(DocumentStore store){this.store=store;}
    public DocumentStore.Document preview(String id,String tenant){
        Key key=new Key(partition?tenant:"",id);
        return cache.computeIfAbsent(key,ignored->store.read(id,tenant));
    }
    public void clear(){cache.clear();}
    public int entries(){return cache.size();}
    private record Key(String tenant,String id){}
}
