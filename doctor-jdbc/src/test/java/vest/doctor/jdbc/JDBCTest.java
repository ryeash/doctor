package vest.doctor.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.doctor.stream.StreamExt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class JDBCTest extends Assert {

    private static final ExecutorService BACKGROUND = Executors.newFixedThreadPool(4);

    JDBC jdbc;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:hsqldb:mem:doctor-mem-db");
        ds.setUsername("doctor");
        ds.setPassword("");
        ds.setMaximumPoolSize(3);

        jdbc = new JDBC(ds);
        assertNotNull(jdbc.dataSource());

        jdbc.execute("CREATE TABLE USERS (" +
                "ID INT NOT NULL, " +
                "NAME VARCHAR(256), " +
                "PASSWORD VARCHAR(256), " +
                "PRIMARY KEY(ID))");


        jdbc.execute("CREATE TABLE USER_DATA (" +
                "ID INT NOT NULL, " +
                "USER_ID INT NOT NULL, " +
                "DATA VARCHAR(256), " +
                "PRIMARY KEY(ID))");

        jdbc.transaction(api -> {
            PreparedQuery insertUser = api.preparedQuery("insert into users values (?, ?, ?)");

            for (int i = 0; i < 5; i++) {
                insertUser.clearParameters();
                insertUser.bindAll(Arrays.asList(i + 1, UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                        .addBatch();
            }
            insertUser.executeBatch();

            PreparedQuery insertData = api.preparedQuery("insert into user_data values (?, ?, ?)");
            for (int i = 0; i < 10; i++) {
                long update = insertData
                        .bind(1, i + 1)
                        .bind(2, ThreadLocalRandom.current().nextInt(1, 6))
                        .bind(3, randomString(ThreadLocalRandom.current().nextInt(10, 30)), Types.VARCHAR)
                        .update();
                assertEquals(update, 1);
            }

            PreparedQuery name = api.prepareNamedParameterQuery("insert into user_data (id, user_id, data) values (:id, :user_id, 'test')");
            for (int i = 11; i < 20; i++) {
                long update = name
                        .bind("id", i + 1)
                        .bind("user_id", ThreadLocalRandom.current().nextInt(1, 6))
                        .update();
                assertEquals(update, 1);
            }
        });
    }

    public static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = (char) ThreadLocalRandom.current().nextInt('A', 'z');
            sb.append(c);
        }
        return sb.toString();
    }

    @AfterClass(alwaysRun = true)
    public void closeout() {
        if (jdbc != null) {
            jdbc.close();
        }
    }

    @Test
    public void row() {
        jdbc.select("select * from users")
                .forEach(row -> {
                    int id = row.getNumber("id").intValue();
                    assertTrue(id >= 0);
                    assertTrue(row.hasColumn("name"));
                    assertEquals(row.toMap().get("NAME"), row.getString("name"));
                    assertFalse(row.getOpt("notThere", String.class).isPresent());
                    assertTrue(row.getOpt("id", Number.class).isPresent());
                    assertTrue(row.getOpt("id", Integer.class).orElse(0) >= 1);
                });
    }

    @Test
    public void simpleSelect() {
        jdbc.select("select * from users")
                .forEach(row -> assertNotNull(row.get("id")));
    }

    @Test
    public void parallelism() throws ExecutionException, InterruptedException {
        BACKGROUND.submit(() ->
                IntStream.range(0, 100)
                        .parallel()
                        .forEach(i ->
                                jdbc.select("select * from users")
                                        .forEach(row -> assertNotNull(row.get("name")))
                        )).get();
    }

    @Test
    public void join() {
        jdbc.select("select * from users " +
                "join user_data on users.id = user_data.user_id")
                .forEach(row -> {
                    assertNotNull(row.get("name"));
                    assertNotNull(row.get("data"));
                });
    }

    @Test
    public void unmanaged() throws SQLException {
        try (Connection c = jdbc.connection().unwrap();
             Statement statement = c.createStatement()) {
            statement.execute("select * from users");
            ResultSet resultSet = statement.getResultSet();
            assertTrue(resultSet.next());
            assertNotNull(resultSet.getString(1));
        }
    }

    @Test
    public void transactionConsumerAndFunction() {
        jdbc.transaction(conn -> {
            for (int i = 0; i < 3; i++) {
                long c = conn.preparedQuery("select * from users where id > ?")
                        .bind(1, 1)
                        .execute()
                        .count();
                assertTrue(c > 1);
            }
            for (int i = 0; i < 3; i++) {
                long c = conn.query("select * from users where id > 1")
                        .execute()
                        .count();
                assertTrue(c > 1);
            }
        });
    }

    @Test
    public void reusable() {
        JDBCConnection connection = jdbc.connection();
        assertFalse(connection.isReusable());
        JDBCConnection reusable = connection.reusable();
        assertTrue(reusable.isReusable());
        reusable.close();
    }

    @Test(invocationCount = 10) // <-- making sure failures don't leak connections
    public void transactionFailures() {
        assertThrows(() -> jdbc.transaction(c -> {
            c.query("update users set name = 'error' where id = 1");
            return c;
        }));
        jdbc.select("select * from users where id = 1")
                .map(Row::toMap)
                .feedForwardAndReturn(this::ensureOnlyOne)
                .forEach(user -> assertNotEquals(user.get("name"), "error"));
    }

    public <T> StreamExt<T> ensureOnlyOne(StreamExt<T> stream) {
        AtomicLong count = new AtomicLong(0);
        return stream.peek(t -> count.incrementAndGet())
                .onClose(() -> assertEquals(count.get(), 1L, "expected only one datapoint"));
    }

    @Test
    public void transactionBatch() {
        TransactionBatch batch = jdbc.transactionBatch()
                .setMaxActions(-1)
                .add(c -> c.preparedQuery("insert into users values (?, ?, ?)")
                        .bindAll(100000, "batch", "1")
                        .execute())
                .add(c -> c.preparedQuery("insert into user_data values (?, ?, ?)")
                        .bindAll(10001, 100000, "toast")
                        .execute());
        assertEquals(batch.size(), 2);
        batch.execute()
                .clear();

        Map<String, Object> datum = jdbc.select("select * from user_data where id = 10001")
                .map(Row::toMap)
                .findFirst()
                .orElseThrow(() -> new JDBCException("not found"));
        assertEquals(datum.get("data"), "toast");
    }

    @Test
    public void execute() {
        jdbc.execute("insert into users values (999999, 'executetest', 'Password')");
        String name = jdbc.query("select * from users where id = 999999")
                .execute()
                .map(r -> r.getString("name"))
                .findFirst()
                .orElseThrow();
        assertEquals(name, "executetest");
    }

    @Test
    public void quickPrepare() {
        jdbc.preparedQuery("select * from users where name like ?")
                .bindAll("%a%")
                .execute()
                .forEach(row -> assertTrue(row.getString("name").contains("a")));
    }

    @Test
    public void returnFromTransaction() {
        String name = UUID.randomUUID().toString();
        String returned = jdbc.transaction(con -> {
            jdbc.execute("insert into users values (9999999, '" + name + "', 'Password')");
            return name;
        });
        assertEquals(name, returned);
    }

    @Test
    public void update() {
        long changed = jdbc.update("update user_data set data = data || 'altered'");
        assertTrue(changed > 0);
    }

    @Test
    public void count() {
        long count = jdbc.connection()
                .count("select count(1) c from user_data", "c");
        assertTrue(count > 0);
    }
}