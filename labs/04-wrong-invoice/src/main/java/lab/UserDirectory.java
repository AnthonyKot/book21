package lab;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** The server's own record of which tenant a user belongs to. Never taken from the request. */
@Component
class UserDirectory {
    private final JdbcClient jdbc;

    UserDirectory(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    String tenantOf(String username) {
        return jdbc.sql("SELECT tenant_id FROM app_user WHERE username = ?")
                .param(username).query(String.class).single();
    }
}
