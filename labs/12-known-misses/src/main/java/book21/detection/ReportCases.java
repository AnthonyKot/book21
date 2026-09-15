package book21.detection;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

// Reporting module added after the rule was written. Authored local fixtures.
@Service
public class ReportCases {
    private final JdbcTemplate jdbc;

    public ReportCases(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> byOwner(HttpServletRequest request) {
        String owner = request.getHeader("X-Owner");
        // case: report-by-owner
        return jdbc.queryForList("SELECT title FROM documents WHERE owner = '" + owner + "' ORDER BY id", String.class);
    }

    public List<String> rows(HttpServletRequest request) {
        String title = request.getParameter("title");
        // case: report-rows
        return jdbc.query("SELECT title FROM documents WHERE title = '" + title + "' ORDER BY id", (rs, n) -> rs.getString(1));
    }

    public List<String> sorted(HttpServletRequest request) {
        String column = switch (String.valueOf(request.getParameter("sort"))) {
            case "title" -> "title";
            default -> "id";
        };
        // case: report-sorted
        return jdbc.queryForList("SELECT title FROM documents ORDER BY " + column, String.class);
    }

    public List<String> page(HttpServletRequest request) {
        int page = Integer.parseInt(request.getParameter("page"));
        // case: report-page
        return jdbc.queryForList("SELECT title FROM documents ORDER BY id LIMIT 2 OFFSET " + (page * 2), String.class);
    }

    public List<String> ownerList(HttpServletRequest request) {
        // case: report-owner-list
        return jdbc.queryForList("SELECT title FROM documents WHERE owner = ? ORDER BY id", String.class, request.getHeader("X-Owner"));
    }
}
