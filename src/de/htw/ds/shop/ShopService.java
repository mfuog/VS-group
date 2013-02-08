package de.htw.ds.shop;

import java.sql.SQLException;
import java.util.Collection;
import java.util.SortedSet;

import javax.jws.Oneway;
import javax.jws.WebParam;
import javax.jws.WebService;
import de.sb.javase.TypeMetadata;


/**
 * <p>Shop SOAP service interface.</p>
 */
@WebService
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public interface ShopService {

	/**
	 * Returns all article data.
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	SortedSet<Article> queryArticles() throws SQLException;


	/**
	 * Returns the article data for the given identity.
	 * @param articleIdentity the article identity
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	Article queryArticle(
		@WebParam(name="articleIdentity") long articleIdentity
	) throws SQLException;


	/**
	 * Registers a new customer and returns it's identity. The given customer's identity
	 * is ignored during processing.
	 * @param customer the customer
	 * @return the customer identity
	 * @throws NullPointerException if the customer is <tt>null</tt>, or contains a <tt>null</tt>
	 *     alias or null password
	 * @throws IllegalArgumentException if the customer's alias or password are shorter than four digits
	 * @throws IllegalStateException if the insert is unsuccessful
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	long registerCustomer(
		@WebParam(name="customer") Customer customer
	) throws NullPointerException, IllegalArgumentException, IllegalStateException, SQLException;


	/**
	 * Unregisters a customer that has no orders.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @return the customer identity
	 * @throws NullPointerException if one of the given values is null
	 * @throws IllegalStateException if the login data is invalid, or the customer has any orders
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	long unregisterCustomer(
		@WebParam(name="alias") String alias,
		@WebParam(name="password") String password
	) throws NullPointerException, IllegalStateException, SQLException;


	/**
	 * Returns the customer data.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @return the customer matching the given alias and password
	 * @throws NullPointerException if one of the given values is null
	 * @throws IllegalStateException if the login data is invalid
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	Customer queryCustomer(
		@WebParam(name="alias") String alias,
		@WebParam(name="password") String password
	) throws NullPointerException, IllegalStateException, SQLException;


	/**
	 * Creates an order from the given items. Note that the suggested price for
	 * each item must be equal to or exceed the current article price. Also,
	 * note that orders which exhaust the available article capacity are
	 * rejected. Finally, note that the given items collection must contain at
	 * least one element.
	 * @param alias
	 *            the customer alias
	 * @param password
	 *            the customer password
	 * @param items
	 *            the order items
	 * @return the order identity
	 * @throws NullPointerException
	 *             if one of the given values is null
	 * @throws IllegalArgumentException
	 *             if one of the given items is priced too low
	 * @throws IllegalStateException
	 *             if the login data is invalid, or the order creation fails
	 * @throws SQLException
	 *             if there is a problem with the underlying JDBC connection
	 */
	long createOrder (
		@WebParam(name="alias") String alias,
		@WebParam(name="password") String password,
		@WebParam(name="items") Collection<OrderItem> items
	) throws NullPointerException, IllegalArgumentException, IllegalStateException, SQLException;


	/**
	 * Cancels an order if the given orderIdentity identifies an existing
	 * order of the user with the given alias, the password is correct for
	 * said user, and the order is no older than one hour.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @param orderIdentity the order identity
	 */
	@Oneway
	void cancelOrder (
		@WebParam(name="alias") String alias,
		@WebParam(name="password") String password,
		@WebParam(name="orderIdentity") long orderIdentity
	);


	/**
	 * Queries the given order.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @param orderIdentity the order identity
	 * @return the customer's order
	 * @throws NullPointerException if one of the given values is null
	 * @throws IllegalStateException if the login data is invalid, or the purchase doesn't target the customer
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	Order queryOrder (
		@WebParam(name="alias") String alias,
		@WebParam(name="password") String password,
		@WebParam(name="orderIdentity") long orderIdentity
	) throws NullPointerException, IllegalStateException, SQLException;


	/**
	 * Queries the given customer's orders.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @return the customer's orders
	 * @throws NullPointerException if one of the given values is null
	 * @throws IllegalStateException if the login data is invalid
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	SortedSet<Order> queryOrders (
		@WebParam(name="alias") String alias,
		@WebParam(name="password") String password
	) throws NullPointerException, IllegalStateException, SQLException;
}