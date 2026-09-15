package book21.detection;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

// Authored local fixtures. These methods intentionally include injection flaws.
@Service
public class SearchCases {
    private final JdbcTemplate jdbc;
    private final boolean bind;
    public SearchCases(JdbcTemplate jdbc, @Value("${lab.bind:true}") boolean bind) {
        this.jdbc = jdbc;
        this.bind = bind;
    }
    public List<String> direct(HttpServletRequest request) {
        // case: direct
        return jdbc.queryForList("SELECT title FROM documents WHERE title = '" + request.getParameter("title") + "' ORDER BY id", String.class);
    }
    public List<String> alias(HttpServletRequest request) {
        String title = request.getParameter("title");
        String sql = "SELECT title FROM documents WHERE title = '" + title + "' ORDER BY id";
        // case: alias
        return jdbc.queryForList(sql, String.class);
    }
    public List<String> wrapped(HttpServletRequest request) {
        String title = request.getParameter("title");
        return execute("SELECT title FROM documents WHERE title = '" + title + "' ORDER BY id");
    }
    private List<String> execute(String sql) {
        // case: wrapped
        return jdbc.queryForList(sql, String.class);
    }
    public List<String> bound(HttpServletRequest request) {
        // case: bound
        return jdbc.queryForList("SELECT title FROM documents WHERE title = ? ORDER BY id", String.class, request.getParameter("title"));
    }
    public List<String> repaired(HttpServletRequest request) {
        if (!bind) return direct(request); // Negative control reopens the real vulnerable path.
        // case: repaired
        return jdbc.queryForList("SELECT title FROM documents WHERE title = ? ORDER BY id", String.class, request.getParameter("title"));
    }
    public List<String> constant(HttpServletRequest request) {
        // case: constant
        return jdbc.queryForList("SELECT title FROM documents WHERE title = 'Budget' ORDER BY id", String.class);
    }
}
