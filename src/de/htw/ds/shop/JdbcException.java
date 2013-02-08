package de.htw.ds.shop;

import java.sql.SQLException;


/**
 * <p>Wrapper class for SQL Exception that can be marshaled without generating
 * JAX-B problems. See related description of JAX-B bug #814 for details. Note
 * that this bug is fixed in OpenJDK/IcedTea7 2.2.4u2+, which allows SQLException
 * to be declared in web-service interfaces again. For previous versions of
 * OpenJDK/IcedTea, declare this exception type instead in service interfaces,
 * and use it in service implementations to wrap SQLException-Instances occurring!</p>
 * @see http://java.net/jira/browse/JAXB-814
 */
public class JdbcException extends Exception {
	private static final long serialVersionUID = 3836363944063765934L;

	/**
	 * Constructs a new instance with <tt>null</tt> as its detail message
	 * and cause.
	 */
	public JdbcException() {
		super();
	}


	/**
	 * Constructs a new instance with the given message, and <tt>null</tt>
	 * as its cause.
	 * @param message the detail message
	 */
	public JdbcException(final String message) {
		super(message);
	}


	/**
	 * Constructs a new instance with the given cause, and the cause's
	 * message
	 * @param cause the cause
	 */
	public JdbcException(final SQLException cause) {
		super(cause);
	}


	/**
	 * Constructs a new instance with the given cause and message.
	 * @param message the message
	 * @param cause the cause
	 */
	public JdbcException(final String message, final SQLException cause) {
		super(message, cause);
	}
}