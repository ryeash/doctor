/**
 * This package is a collection of helpers, decorators, and wrappers to make using
 * the standard {@link java.sql JDBC package} a bit easier.
 * <p><br>
 * The starting point is {@link vest.doctor.jdbc.JDBC}; example (using hikari):
 * <code><pre>
 * HikariDataSource dataSource = new HikariDataSource();
 * ... init hikari datasource pool ...
 * JDBC jdbc = new JDBC(dataSource, List.of(
 *  // built in interceptors to explicitly set auto-commit and read-only state on connections
 *  new JDBCInterceptor.AutoCommit(false),
 *  new JDBCInterceptor.ReadOnly(false)));
 *
 * // use a transaction to insert data
 * jdbc.inTransaction(c -> {
 *  c.setAutoCommit(false);
 *  JDBCStatement<PreparedStatement> insertUser = c.prepare("insert into users values (?, ?, ?)");
 *  for (int i = 0; i < 50; i++) {
 *      insertUser.bindAll(List.of(i, i + "", "password")).addBatch();
 *  }
 *  insertUser.executeBatch();
 * });
 *
 * // use a connection to query data
 * List<Map<String, Object>> rows = jdbc.withConnection(c -> {
 *  return c.select("select * from users limit 10")
 *      .map(Row::toMap)
 *      .toList();
 * });
 * </pre></code>
 */
package vest.doctor.jdbc;
