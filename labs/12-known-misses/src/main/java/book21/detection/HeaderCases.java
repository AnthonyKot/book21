package book21.detection;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

// Independent rule exercise: the request value now arrives in a header.
@Service
public class HeaderCases {
    private final JdbcTemplate jdbc;
    public HeaderCases(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<String> unsafe(HttpServletRequest request) {
        String title = request.getHeader("X-Title");
        // case: header-unsafe
        return jdbc.queryForList("SELECT title FROM documents WHERE title = '" + title + "' ORDER BY id", String.class);
    }
    public List<String> bound(HttpServletRequest request) {
        // case: header-bound
        return jdbc.queryForList("SELECT title FROM documents WHERE title = ? ORDER BY id", String.class, request.getHeader("X-Title"));
    }
    public List<String> wrapped(HttpServletRequest request) {
        return execute("SELECT title FROM documents WHERE title = '" + request.getHeader("X-Title") + "' ORDER BY id");
    }
    private List<String> execute(String sql) {
        // case: header-wrapped
        return jdbc.queryForList(sql, String.class);
    }
}
