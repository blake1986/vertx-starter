package io.vertx.starter.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.serviceproxy.ProxyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Created by trunglnm on 8/29/17.
 */
public class WikiDataBaseVerticle extends AbstractVerticle {

	public static final String CONFIG_WIKIDB_JDBC_URL = "wikidb.jdbc.url";
	public static final String CONFIG_WIKIDB_JDBC_DRIVER_CLASS = "wikidb.jdbc.driver_class";
	public static final String CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE = "wikidb.jdbc.max_pool_size";
	public static final String CONFIG_WIKIDB_SQL_QUERIES_RESOURCE_FILE = "wikidb.sqlqueries.resource.file";

	public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";

	private static final Logger LOGGER = LoggerFactory.getLogger(WikiDataBaseVerticle.class);

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		HashMap<SqlQuery, String> sqlQueries = loadSqlQueries();

		JDBCClient dbClient = JDBCClient.createShared(vertx, new JsonObject()
				.put("url", config().getString(CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:file:db/wiki"))
				.put("driver_class", config().getString(CONFIG_WIKIDB_JDBC_DRIVER_CLASS, "org.hsqldb.jdbcDriver"))
				.put("max_pool_size", config().getInteger(CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 30)));

		WikiDatabaseService.create(dbClient, sqlQueries, resultHandler -> {

			if (resultHandler.succeeded()) {
				ProxyHelper.registerService(WikiDatabaseService.class, vertx, resultHandler.result(), CONFIG_WIKIDB_QUEUE);
				startFuture.complete();
			} else {
				startFuture.fail(resultHandler.cause());
			}
		});
	}

	/**
	 * Note: this uses blocking APIs, but data is small ...
	 */
	private HashMap<SqlQuery, String> loadSqlQueries() throws IOException {

		String queriesFile = config().getString(CONFIG_WIKIDB_SQL_QUERIES_RESOURCE_FILE);
		InputStream queriesInputStream;
		if (queriesFile != null) {
			queriesInputStream = new FileInputStream(queriesFile);
		} else {
			queriesInputStream = getClass().getResourceAsStream("/db-queries.properties");
		}

		Properties queriesProps = new Properties();
		queriesProps.load(queriesInputStream);
		queriesInputStream.close();

		HashMap<SqlQuery, String> sqlQueries = new HashMap<>();
		sqlQueries.put(SqlQuery.CREATE_PAGES_TABLE, queriesProps.getProperty("create-pages-table"));
		sqlQueries.put(SqlQuery.ALL_PAGES, queriesProps.getProperty("all-pages"));
		sqlQueries.put(SqlQuery.GET_PAGE, queriesProps.getProperty("get-page"));
		sqlQueries.put(SqlQuery.CREATE_PAGE, queriesProps.getProperty("create-page"));
		sqlQueries.put(SqlQuery.SAVE_PAGE, queriesProps.getProperty("save-page"));
		sqlQueries.put(SqlQuery.DELETE_PAGE, queriesProps.getProperty("delete-page"));
		sqlQueries.put(SqlQuery.ALL_PAGES_DATA, queriesProps.getProperty("all-pages-data"));
		sqlQueries.put(SqlQuery.GET_PAGE_BY_ID, queriesProps.getProperty("get-page-by-id"));

		return sqlQueries;
	}

}
