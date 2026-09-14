package lab;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
@Component
public class UploadStore {
    static final int MAX_BYTES=8192;
    private final Path dir;
    public UploadStore() throws IOException {dir=Files.createTempDirectory("book21-uploads-");}
    public String save(InputStream input) throws IOException {
        byte[] bytes=input.readNBytes(MAX_BYTES+1);
        if(bytes.length>MAX_BYTES)throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
        String id=UUID.randomUUID().toString();Files.write(dir.resolve(id),bytes,StandardOpenOption.CREATE_NEW);
        return id;
    }
    public byte[] read(String id) throws IOException {
        if(!id.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"))throw new ResponseStatusException(NOT_FOUND);
        Path file=dir.resolve(id);
        if(!Files.exists(file))throw new ResponseStatusException(NOT_FOUND);
        return Files.readAllBytes(file);
    }
    @PreDestroy void stop() throws IOException {
        try(var files=Files.list(dir)){for(Path file:files.toList())Files.deleteIfExists(file);}
        Files.deleteIfExists(dir);
    }
}
