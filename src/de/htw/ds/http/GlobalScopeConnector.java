package de.htw.ds.http;

import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.Serializables;
import de.sb.javase.sql.JdbcConnectionMonitor;


/**
 * <p>Connector class reading and writing persistent global scope entries
 * from/into a database. Note that this class is parameterized using a
 * property file named "database.properties"</p>
 */
@TypeMetadata(copyright="2012-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class GlobalScopeConnector implements AutoCloseable {
	private static final String DELETE_ENTRY = "DELETE FROM GlobalScope WHERE alias=?";
	private static final String INSERT_ENTRY = "INSERT INTO GlobalScope VALUES (0, ?, ?)";
	private static final String SELECT_ENTRY = "SELECT * FROM GlobalScope WHERE alias=?";
	private static final String SELECT_ENTRIES = "SELECT * FROM GlobalScope";
	private static final DataSource DATA_SOURCE;

	static {
		try {
			final Properties properties = new Properties();
			try (InputStream byteSource = GlobalScopeConnector.class.getResourceAsStream("http-mysql.properties")) {
				properties.load(byteSource);
			}

			final Class<?> dataSourceClass = Class.forName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource", true, Thread.currentThread().getContextClassLoader());
			final DataSource dataSource = (DataSource) dataSourceClass.newInstance();
			dataSourceClass.getMethod("setURL", String.class).invoke(dataSource, properties.get("connectionUrl"));
			dataSourceClass.getMethod("setCharacterEncoding", String.class).invoke(dataSource, properties.get("characterEncoding"));
			dataSourceClass.getMethod("setUser", String.class).invoke(dataSource, properties.get("alias"));
			dataSourceClass.getMethod("setPassword", String.class).invoke(dataSource, properties.get("password"));
			DATA_SOURCE = dataSource;
		} catch (final Exception exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}

	private final Connection connection;


	/**
	 * Creates a new global scope connector based on an exclusive JDBC connection.
	 * @throws NullPointerException if the given JDBC connection is <tt>null</tt>
	 * @throws SQLException if there is an underlying JDBC problem
	 */
	public GlobalScopeConnector () throws SQLException {
		super();

		this.connection = DATA_SOURCE.getConnection();
		this.connection.setAutoCommit(false);

		final Runnable connectionMonitor = new JdbcConnectionMonitor(this.connection, "select null", 60000);
		final Thread thread = new Thread(connectionMonitor, "jdbc-connection-monitor");
		thread.setDaemon(true);
		thread.start();
	}


	/**
	 * Closes the underlying JDBC connection.
	 */
	public void close() throws SQLException {
		this.connection.close();
	}


	/**
	 * Returns the persistent global scope entries read from a database.
	 * @return the global scope entries as a map
	 * @throws ClassNotFoundException if a value was persisted with a class that is not
	 *    available or compatible on this VM
	 * @throws SQLException if there is a problem reading a persistent global scope entry
	 */
	public Map<String,Serializable> readEntries() throws ClassNotFoundException, SQLException {
		final Map<String,Serializable> result = new HashMap<>();
		synchronized (this.connection) {
			try (PreparedStatement statement = this.connection.prepareStatement(SELECT_ENTRIES)) {
				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						final String key = resultSet.getString("alias");
						final byte[] valueData = resultSet.getBytes("value");
						final Serializable value = Serializables.deserializeObjects(valueData, 0, valueData.length)[0];
						result.put(key, value);
					}
				}
				this.connection.commit();
			} catch (final Exception exception) {
				try { this.connection.rollback(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
				throw exception;
			}
		}
		return result;
	}


	/**
	 * Returns the persistent global scope value for the given key, read from a database.
	 * Returns <tt>null</tt> if there is no matching value in the database
	 * @return the global scope entries as a map
	 * @throws ClassNotFoundException if a value was persisted with a class that is not
	 *    available or compatible on this VM
	 * @throws SQLException if there is a problem reading the persistent global scope entry
	 */
	public Serializable readEntry(final String key) throws ClassNotFoundException, SQLException {
		synchronized (this.connection) {
			try (PreparedStatement statement = this.connection.prepareStatement(SELECT_ENTRY)) {
				statement.setString(1, key);

				final Serializable result;
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						final byte[] valueData = resultSet.getBytes("value");
						result = Serializables.deserializeObjects(valueData, 0, valueData.length)[0];
					} else {
						result = null;
					}
				}

				this.connection.commit();
				return result;
			} catch (final Exception exception) {
				try { this.connection.rollback(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
				throw exception;
			}
		}
	}


	/**
	 * Writes a persistent global scope entry to a database. If the given value is <tt>null</tt>
	 * while the key is not, the underlying database entry is removed.
	 * @return the global scope entries as a map
	 * @throws NotSerializableException if a value is passed that cannot be serialized
	 * @throws SQLException if there is a problem writing the persistent global scope entry
	 */
	public void writeEntry(final String key, final Serializable value) throws SQLException, NotSerializableException {
		synchronized (this.connection) {
			try {
				try (PreparedStatement statement = this.connection.prepareStatement(DELETE_ENTRY)) {
					statement.setString(1, key);
					statement.execute();
				}

				if (value != null) {
					final byte[] valueData = Serializables.serializeObjects(value);
					try (PreparedStatement statement = this.connection.prepareStatement(INSERT_ENTRY)) {
						statement.setString(1, key);
						statement.setBytes(2, valueData);
						statement.execute();
					}
				}
				this.connection.commit();
			} catch (final Exception exception) {
				try { this.connection.rollback(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
				throw exception;
			}
		}
	}
}