package lab;
import java.io.IOException;
import org.springframework.stereotype.Component;
@Component
public class SummaryWorker {
    private final UploadStore store;
    private final InvoiceParser parser;
    public SummaryWorker(UploadStore store,InvoiceParser parser){this.store=store;this.parser=parser;}
    public String summarize(String id) throws IOException {
        // Independent exercise: this later consumer still enables external entities.
        // A stored file or a failed preview is not evidence that these bytes are trusted.
        return parser.title(store.read(id),false);
    }
}
