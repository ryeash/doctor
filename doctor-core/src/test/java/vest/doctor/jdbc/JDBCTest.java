package vest.doctor.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Test(invocationCount = 15, threadPoolSize = 5)
public class JDBCTest extends Assert {

    private static final AtomicInteger i = new AtomicInteger(-10000);

    JDBC jdbc;

    @BeforeClass(alwaysRun = true)
    public void initializeDB() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:hsqldb:mem:testdb;sql.enforce_strict_size=true;hsqldb.tx=mvcc");
        ds.setUsername("SA");
        ds.setPassword("");
        ds.setPoolName("test");
        ds.setMaximumPoolSize(3);
        ds.setMinimumIdle(0);
        jdbc = new JDBC(ds, List.of(
                new JDBCInterceptor.AutoCommit(false),
                new JDBCInterceptor.ReadOnly(false),
                new TestCustomization()));

        jdbc.transaction(c -> {
            c.update("CREATE TABLE USERS (" +
                    "ID INT NOT NULL, " +
                    "NAME VARCHAR(256), " +
                    "PASSWORD VARCHAR(256), " +
                    "PRIMARY KEY(ID))");
            c.update("CREATE TABLE PROPERTIES (" +
                    "USER_ID INT NOT NULL, " +
                    "NAME VARCHAR(256)," +
                    "DATA VARCHAR(2048), " +
                    "PRIMARY KEY(USER_ID, NAME))");
        });

        jdbc.transaction(c -> {
            JDBCStatement<PreparedStatement> insertUser = c.configure(conn -> {
                        try {
                            conn.setAutoCommit(false);
                            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                        } catch (Throwable t) {
                            throw new DatabaseException(t);
                        }
                    })
                    .prepare("insert into users values (?, ?, ?)");
            for (int i = 0; i < 50; i++) {
                insertUser.bindAll(List.of(i, i + "", "password")).addBatch();
            }
            insertUser.executeBatch();
        });

        try (Transaction tx = jdbc.transaction()) {
            JDBCConnection c = tx.connection();
            JDBCStatement<PreparedStatement> insertProperty = c.prepare("insert into properties values (?, ?, ?)");
            for (int i = 0; i < 50; i++) {
                insertProperty.bindAll(List.of(i, "defer", ThreadLocalRandom.current().nextBoolean() + ""))
                        .addBatch()
                        .bindAll(List.of(i, "hats", ThreadLocalRandom.current().nextInt(10, 100) + ""))
                        .addBatch();
            }
            insertProperty.executeBatch();
        }
    }

    @AfterClass(alwaysRun = true)
    public void teardown() {
        ((HikariDataSource) jdbc.dataSource()).close();
    }

    public void connectionConsumerAndFunction() {
        jdbc.connection(c -> {
            List<Map<String, Object>> rows = c.select("select * from users limit 10")
                    .map(RowMapper.rowToMap())
                    .toList();
            assertFalse(rows.isEmpty());
        });

        List<Map<String, Object>> rows = jdbc.connection(c -> {
            return c.statement("select * from users limit 10")
                    .configure(s -> {
                        try {
                            s.setMaxRows(10);
                            s.setFetchDirection(ResultSet.FETCH_FORWARD);
                            s.setFetchSize(1000);
                            s.setQueryTimeout(2);
                        } catch (Throwable t) {
                            throw new DatabaseException(t);
                        }
                    })
                    .select()
                    .map(RowMapper.rowToMap())
                    .toList();
        });
        assertFalse(rows.isEmpty());
    }

    public void insertMechanisms() throws SQLException {
        int count = i.decrementAndGet();
        try (Transaction tx = jdbc.transaction()) {
            tx.connection().insert("insert into users values (?, ?, ?)", List.of(count, "thing" + count, "pa$$"));
            tx.connection().insert("insert into properties values (?, ?, ?)", List.of(count, "bats", "numerous"));
            tx.connection().insert("insert into properties values (" + count + ", 'cats', 'none')");
            tx.commit();
        }
        try (JDBCConnection c = jdbc.connection()) {
            String bats = c.select("select * from properties where user_id = " + count + " and name = 'bats'")
                    .map(RowMapper.apply(row -> row.getString("data")))
                    .findFirst()
                    .orElse(null);
            assertEquals(bats, "numerous");
            String cats = c.select("select * from properties where user_id = " + count + " and name = 'cats'")
                    .map(RowMapper.apply(row -> row.getString("data")))
                    .findFirst()
                    .orElse(null);
            assertEquals(cats, "none");

        }
        jdbc. transaction(c -> {
            assertEquals(c.prepare("delete from properties where user_id = ?")
                    .bindAll(List.of(count))
                    .update(), 2);
            assertEquals(  c.prepare("delete from users where id = ?")
                    .bindAll(List.of(count))
                    .update(), 1);
        });
    }

    public void count() {
        long count = jdbc.connection(c -> {
            return c.count("select count(0) c from users", "c");
        });
        assertEquals(count, 50);
    }

    public void errorHandling() {
        assertThrows(() -> {
            try (Transaction tx = jdbc.transaction()) {
                tx.connection().insert("insert into users values (?, ?, ?)", List.of(-13, "thirteen", "pa$$"));
                tx.connection().insert("insert into users values (?, ?, ?)", List.of(-13, "thirteen1", "pa$$"));
                tx.commit();
            }
        });
    }
}
