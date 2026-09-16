package lab;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
@Repository
public class InvoiceRepository {
    private final JdbcTemplate jdbc;
    private final boolean bindTitle;
    private static final String COLUMNS = "SELECT id, tenant_id, title, amount FROM invoice";
    private static final RowMapper<Invoice> ROW = (rs, n) -> new Invoice(
        rs.getString("id"), rs.getString("tenant_id"), rs.getString("title"), rs.getInt("amount"));
    public record Invoice(String id, String tenant, String title, int amount) {}
    public InvoiceRepository(JdbcTemplate jdbc, @Value("${lab.bind-title}") boolean bindTitle) {
        this.jdbc = jdbc; this.bindTitle = bindTitle;
    }
    public List<Invoice> search(String tenant, String title) {
        if (bindTitle) {
            return jdbc.query(COLUMNS + " WHERE tenant_id = ? AND title = ? ORDER BY id", ROW, tenant, title);
        }
        // Deliberate SQL injection, even though this overload uses a PreparedStatement.
        String sql = COLUMNS + " WHERE tenant_id = ? AND title = '" + title + "' ORDER BY id";
        return jdbc.query(sql, ROW, tenant);
    }
    public List<Invoice> scoped(String tenant, String id) {
        return jdbc.query(COLUMNS + " WHERE tenant_id = ? AND id = ?", ROW, tenant, id);
    }
    public List<Invoice> unscoped(String id) {
        // Deliberate counterexample: bound input, missing resource authorization.
        return jdbc.query(COLUMNS + " WHERE id = ?", ROW, id);
    }
    public List<Invoice> sorted(String tenant, String sort) {
        // Practice endpoint. Its contract is stated in practice/07-query-worksheet.md.
        return jdbc.query(COLUMNS + " WHERE tenant_id = ? ORDER BY " + sort + ", id", ROW, tenant);
    }
}
