package de.htw.ds.shop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.SortedSet;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;


/**
 * <p>Shop server class implementing a JAX-WS based web-service.
 */
@WebService (endpointInterface="de.htw.ds.shop.ShopService", serviceName="ShopService", portName="ShopPort")
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public class ShopServer implements ShopService, AutoCloseable {

	private final URI serviceURI;
	private final Endpoint endpoint;
	private final ShopConnector jdbcConnector;
	private final double taxRate;



	/**
	 * Creates a new shop server that is exported it to the given
	 * SOAP service URI.
	 * @param servicePort the service port
	 * @param serviceName the service name
	 * @param the tax rate
	 * @throws NullPointerException if any of the given arguments is null
	 * @throws IllegalArgumentException if the given service port is outside
	 *    it's allowed range, if the service name is illegal, or if the given
	 *    tax rate is strictly negative
	 * @throws WebServiceException if the service URI's port is already in use
	 * @throws SQLException if there is an underlying JDBC problem
	 */
	public ShopServer(final int servicePort, final String serviceName, final double taxRate) throws SQLException {
		super();
		if (servicePort <= 0 | servicePort > 0xFFFF | taxRate < 0) throw new IllegalArgumentException();

		try {
			this.serviceURI = new URI("http", null, SocketAddress.getLocalAddress().getCanonicalHostName(), servicePort, "/" + serviceName, null, null);
		} catch (final URISyntaxException exception) {
			throw new IllegalArgumentException();
		}

		this.jdbcConnector = new ShopConnector();
		this.jdbcConnector.getConnection().setAutoCommit(false);
		this.taxRate = taxRate;

		this.endpoint = Endpoint.create(SOAPBinding.SOAP11HTTP_BINDING, this);
		this.endpoint.publish(this.serviceURI.toASCIIString());
 	}


	/**
	 * Closes the receiver, thereby stopping it's JDBC connector and SOAP endpoint.
	 * @throws SQLException if there is an SQL related problem
	 */
	public void close() throws SQLException {
		try { this.endpoint.stop(); } catch (final Exception exception) {};
		this.jdbcConnector.close();
	}


	/**
	 * Returns the service URI.
	 * @return the service URI
	 */
	public URI getServiceURI() {
		return this.serviceURI;
	}


	/**
	 * Returns the service tax rate.
	 * @return the service tax rate
	 */
	public double getTaxRate() {
		return taxRate;
	}


	/**
	 * {@inheritDoc}
	 */
	public SortedSet<Article> queryArticles() throws SQLException {
		synchronized (this.jdbcConnector.getConnection()) {
			try {
				final SortedSet<Article> articles = this.jdbcConnector.queryArticles();
				this.jdbcConnector.getConnection().commit();
				return articles;
			} catch (final Exception exception) {
				try { this.jdbcConnector.getConnection().rollback(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
				throw exception;
			}
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public Article queryArticle(final long articleIdentity) throws SQLException {
		synchronized (this.jdbcConnector.getConnection()) {
			try {
				final Article article = this.jdbcConnector.queryArticle(articleIdentity);
				this.jdbcConnector.getConnection().commit();
				return article;
			} catch (final Exception exception) {
				try { this.jdbcConnector.getConnection().rollback(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
				throw exception;
			}
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public long registerCustomer(final Customer customer) throws SQLException {
		synchronized (this.jdbcConnector.getConnection()) {
			try {
				final long customerIdentity = this.jdbcConnector.insertCustomer(customer.getAlias(), customer.getPassword(), customer.getFirstName(), customer.getLastName(), customer.getStreet(), customer.getPostcode(), customer.getCity(), customer.getEmail(), customer.getPhone());
				this.jdbcConnector.getConnection().commit();
				return customerIdentity;
			} catch (final Exception exception){
				try { this.jdbcConnector.getConnection().rollback(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
				throw exception;
			}
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public long unregisterCustomer(final String alias, final String password) throws SQLException {
		synchronized (this.jdbcConnector.getConnection()) {
			try {
				final long customerIdentity = this.jdbcConnector.deleteCustomer(alias, password);
				this.jdbcConnector.getConnection().commit();
				return customerIdentity;
			} catch (final Exception exception){
				try { this.jdbcConnector.getConnection().rollback(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
				throw exception;
			}
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public Customer queryCustomer(final String alias, final String password) throws SQLException {
		synchronized (this.jdbcConnector.getConnection()) {
			try {
				final Customer customer = this.jdbcConnector.queryCustomer(alias, password);
				this.jdbcConnector.getConnection().commit();
				return customer;
			} catch (final Exception exception){
				try { this.jdbcConnector.getConnection().rollback(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
				throw exception;
			}
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public long createOrder(final String alias, final String password, final Collection<OrderItem> items) throws IllegalArgumentException, SQLException {
		synchronized (this.jdbcConnector.getConnection()) {
			try {
				final long orderIdentity = this.jdbcConnector.insertOrder(alias, password, this.taxRate, items);
				this.jdbcConnector.getConnection().commit();
				return orderIdentity;
			} catch (final Exception exception){
				try { this.jdbcConnector.getConnection().rollback(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
				throw exception;
			}
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public void cancelOrder(final String alias, final String password, final long orderIdentity) {
		synchronized (this.jdbcConnector.getConnection()) {
			try {
				this.jdbcConnector.deleteOrder(alias, password, orderIdentity);
				this.jdbcConnector.getConnection().commit();
			} catch (final Exception exception){
				try { this.jdbcConnector.getConnection().rollback(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
			}
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public Order queryOrder(String alias, String password, long orderIdentity) throws SQLException {
		synchronized (this.jdbcConnector.getConnection()) {
			try {
				final Order order = this.jdbcConnector.queryOrder(alias, password, orderIdentity);
				this.jdbcConnector.getConnection().commit();
				return order;
			} catch (final Exception exception){
				try { this.jdbcConnector.getConnection().rollback(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
				throw exception;
			}
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public SortedSet<Order> queryOrders(String alias, String password) throws SQLException {
		synchronized (this.jdbcConnector.getConnection()) {
			try {
				final SortedSet<Order> orders = this.jdbcConnector.queryOrders(alias, password);
				this.jdbcConnector.getConnection().commit();
				return orders;
			} catch (final Exception exception){
				try { this.jdbcConnector.getConnection().rollback(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
				throw exception;
			}
		}
	}


	/**
	 * Application entry point. The given runtime parameters must be a SOAP service port,a
	 * SOAP service name, and a tax rate.
	 * @param args the given runtime arguments
	 * @throws WebServiceException if the given service port is already in use
	 * @throws URISyntaxException if one of the given service URIs is malformed
	 * @throws SQLException if none of the supported JDBC drivers is installed
	 */
	public static void main(final String[] args) throws URISyntaxException, SQLException {
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final String serviceName = args[1];
		final double taxRate = Double.parseDouble(args[2]);

		try (ShopServer server = new ShopServer(servicePort, serviceName, taxRate)) {
			System.out.println("Dynamic (bottom-up) JAX-WS shop server running, enter \"quit\" to stop.");
			System.out.format("Service URI is \"%s\".\n", server.getServiceURI());
			System.out.format("Tax-rate is %.2f%%.\n", 100 * server.getTaxRate());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(charSource.readLine())); } catch (final IOException exception) {}
		}
	}
}