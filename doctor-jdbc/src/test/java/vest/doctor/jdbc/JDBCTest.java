package vest.doctor.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Test(invocationCount = 5, threadPoolSize = 5)
public class JDBCTest extends Assert {

    JDBC jdbc;

    @BeforeClass(alwaysRun = true)
    public void initializeDB() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:hsqldb:mem:testdb");
        ds.setUsername("SA");
        ds.setPassword("");
        ds.setPoolName("test");
        ds.setMaximumPoolSize(3);
        ds.setMinimumIdle(0);
        jdbc = new JDBC(ds, List.of(
                new JDBCInterceptor.AutoCommit(false),
                new JDBCInterceptor.ReadOnly(false),
                new TestCustomization()));

        jdbc.inTransaction(c -> {
            assertTrue(c.isReusable());
            c.executeAndSink("CREATE TABLE USERS (" +
                    "ID INT NOT NULL, " +
                    "NAME VARCHAR(256), " +
                    "PASSWORD VARCHAR(256), " +
                    "PRIMARY KEY(ID))");
            c.executeAndSink("CREATE TABLE PROPERTIES (" +
                    "USER_ID INT NOT NULL, " +
                    "NAME VARCHAR(256)," +
                    "DATA VARCHAR(2048), " +
                    "PRIMARY KEY(USER_ID, NAME))");
        });

        jdbc.inTransaction(c -> {
            c.setAutoCommit(false);
            JDBCStatement<PreparedStatement> insertUser = c.prepare("insert into users values (?, ?, ?)");
            for (int i = 0; i < 50; i++) {
                insertUser.bindAll(List.of(i, i + "", "password")).addBatch();
            }
            insertUser.executeBatch();
        });

        try (Transaction tx = jdbc.transaction()) {
            tx.execute(c -> {
                JDBCStatement<PreparedStatement> insertProperty = c.prepare("insert into properties values (?, ?, ?)");
                for (int i = 0; i < 50; i++) {
                    insertProperty.bindAll(List.of(i, "defer", ThreadLocalRandom.current().nextBoolean() + ""))
                            .addBatch()
                            .bindAll(List.of(i, "hats", ThreadLocalRandom.current().nextInt(10, 100) + ""))
                            .addBatch();
                }
                insertProperty.executeBatch();
            });
        }
    }

    @AfterClass(alwaysRun = true)
    public void teardown() {
        ((HikariDataSource) jdbc.dataSource()).close();
    }

    public void connectionConsumerAndFunction() {
        jdbc.withConnection(c -> {
            List<Map<String, Object>> rows = c.select("select * from users limit 10")
                    .map(Row::toMap)
                    .toList();
            assertFalse(rows.isEmpty());
        });

        List<Map<String, Object>> rows = jdbc.withConnection(c -> {
            return c.select("select * from users limit 10")
                    .map(Row::toMap)
                    .toList();
        });
        assertFalse(rows.isEmpty());
    }

    @Test(invocationCount = 1)
    public void insertMechanisms() {
        try (Transaction tx = jdbc.transaction()) {
            tx.execute(c -> {
                c.insert("insert into properties values (?, ?, ?)", List.of(0, "bats", "numerous"));
            });
            tx.execute(c -> {
                c.insert("insert into properties values (0, 'cats', 'none')");
            });
        }
        String bats = jdbc.connection().select("select * from properties where user_id = 0 and name = 'bats'")
                .map(row -> row.getString("data"))
                .findFirst()
                .orElse(null);
        assertEquals(bats, "numerous");
        String cats = jdbc.connection().select("select * from properties where user_id = 0 and name = 'cats'")
                .map(row -> row.getString("data"))
                .findFirst()
                .orElse(null);
        assertEquals(cats, "none");
    }

    public void count() {
        long count = jdbc.connection().count("select count(0) c from users", "c");
        assertEquals(count, 50);
    }

    public void transactionCallback() {
        Transaction transaction = jdbc.transaction();
        CompletableFuture<Void> passFail = transaction.execute(c -> {
            int id = ThreadLocalRandom.current().nextInt(1000, 100000);
            c.prepare("insert into users values (?, ?, ?)")
                    .bindAll(List.of(id, "name" + id, "gusr"))
                    .execute();
        });
        CompletableFuture<List<String>> listFuture = transaction.execute(c -> {
            return c.select("select name from users where id > 999")
                    .map(row -> row.getString("name"))
                    .collect(Collectors.toList());
        });
        transaction.close();
        assertTrue(passFail.isDone());
        passFail.join();
        List<String> names = listFuture.join();
        assertFalse(names.isEmpty());
        for (String name : names) {
            assertTrue(name.startsWith("name"));
        }
        assertTrue(transaction.isClosed());
        assertThrows(IllegalStateException.class, transaction::rollback);
    }

    public void transactionErrorCallback() {
        Transaction transaction = jdbc.transaction();
        CompletableFuture<Void> passFail = transaction.execute(c -> {
            int id = 1;
            c.prepare("insert into users values (?, ?, ?)")
                    .bindAll(List.of(id, "name" + id, "gusr"))
                    .execute();
        });
        assertThrows(DatabaseException.class, transaction::close);
        assertTrue(passFail.isCompletedExceptionally());
    }

    public void rowOperations() {
        jdbc.connection()
                .select("select * from users where id > 0 limit 10")
                .forEach(row -> {
                    assertNotNull(row.resultSet());
                    assertTrue(row.hasColumn(1));
                    assertFalse(row.hasColumn(0));
                    assertTrue(row.hasColumn("name"));
                    assertFalse(row.hasColumn("not_a_column"));
                    assertNotNull(row.get("name"));
                    assertNotNull(row.getString("name"));
                    assertTrue(row.getNumber("id").longValue() > 0);

                    assertTrue(row.getOpt("name").isPresent());
                    assertFalse(row.getOpt("nope").isPresent());
                    assertTrue(row.getOpt("name", String.class).isPresent());
                    assertFalse(row.getOpt("nope", Character.class).isPresent());

                    assertTrue(row.getOpt(1).isPresent());
                    assertFalse(row.getOpt(0).isPresent());
                    assertFalse(row.getOpt(10).isPresent());
                    assertTrue(row.getOpt(1, String.class).isPresent());
                    assertFalse(row.getOpt(0, Character.class).isPresent());
                    assertFalse(row.getOpt(10, Character.class).isPresent());
                });

        Set<Boolean> data = jdbc.connection()
                .select("select * from users u join properties p on u.id = p.user_id where u.id = ? and p.name = ?",
                        List.of(1, "defer"))
                .map(row -> row.getBoolean("data"))
                .collect(Collectors.toSet());
        assertFalse(data.isEmpty());
    }

    public void invalidTransactionReturnType() {
        try (Transaction tx = jdbc.transaction()) {
            tx.execute(c -> {
                return c.select("select name from users limit 1")
                        .map(row -> row.getString("name"))
                        .collect(Collectors.toList());
            }).whenComplete((name, error) -> {
                assertNull(name);
                assertNotNull(error);
            });
            tx.execute(c -> c)
                    .whenComplete((connect, error) -> {
                        assertNull(connect);
                        assertNotNull(error);
                        assertTrue(error instanceof IllegalArgumentException);
                    });
            tx.execute(c -> {
                return c.select("select name from users limit 1")
                        .map(row -> row.getString("name"))
                        .collect(Collectors.toList());
            }).whenComplete((name, error) -> {
                assertNull(name);
                assertNotNull(error);
            });
        } catch (DatabaseException e) {
            // ignore pass
        }
    }
}
