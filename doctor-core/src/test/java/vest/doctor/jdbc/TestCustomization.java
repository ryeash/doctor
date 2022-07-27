package vest.doctor.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class TestCustomization implements JDBCInterceptor {
    @Override
    public <T extends Statement> T intercept(T statement) throws SQLException {
        statement.setQueryTimeout(15);
        if (statement instanceof PreparedStatement p) {
            p.setPoolable(true);
        }
        return statement;
    }
}
