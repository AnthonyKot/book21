import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
class RuleExamples {
    void direct(JdbcTemplate jdbc, HttpServletRequest req) {
        // ruleid: request-sql
        jdbc.queryForList("SELECT * FROM documents WHERE title='" + req.getParameter("title") + "'", String.class);
    }
    void alias(JdbcTemplate jdbc, HttpServletRequest req) {
        String title = req.getParameter("title");
        String sql = "SELECT * FROM documents WHERE title='" + title + "'";
        // ruleid: request-sql
        jdbc.queryForList(sql, String.class);
    }
    void bound(JdbcTemplate jdbc, HttpServletRequest req) {
        // ok: request-sql
        jdbc.queryForList("SELECT * FROM documents WHERE title=?", String.class, req.getParameter("title"));
    }
    void constant(JdbcTemplate jdbc) {
        // ok: request-sql
        jdbc.queryForList("SELECT * FROM documents WHERE title='Budget'", String.class);
    }
    void caller(JdbcTemplate jdbc, HttpServletRequest req) {
        execute(jdbc, "SELECT * FROM documents WHERE title='" + req.getParameter("title") + "'");
    }
    void execute(JdbcTemplate jdbc, String sql) {
        // todoruleid: request-sql
        jdbc.queryForList(sql, String.class);
    }
}
