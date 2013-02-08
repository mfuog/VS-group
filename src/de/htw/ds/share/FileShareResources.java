package de.htw.ds.share;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;


/**
 * <p>Helper methods for web service URI/URL creation.</p>s
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class FileShareResources {

	/**
	 * Private constructor prevents instantiation.
	 */
	private FileShareResources() {
		super();
	}

	/**
	 * Returns a file share service URI for the given socket address.
	 * @param serviceAddress the socket address
	 * @return the file share service URI
	 */
	public static URI serviceURI(final SocketAddress serviceAddress) {
		try {
			return new URI("http", null, serviceAddress.getHostName(), serviceAddress.getPort(), "/" + FileShareService.class.getSimpleName(), null, null);
		} catch (final URISyntaxException exception) {
			throw new AssertionError();
		}
	}


	/**
	 * Returns a file share service WSDL locator for the given socket address.
	 * @param serviceAddress the socket address
	 * @return the file share service WSDL locator
	 */
	public static URL serviceWsdlLocator(final SocketAddress serviceAddress) {
		try {
			return new URL(serviceURI(serviceAddress).toASCIIString() + "?wsdl");
		} catch (final MalformedURLException exception) {
			throw new AssertionError();
		}
	}


	/**
	 * Returns the standardized hex string representation of the given value.
	 * @param value the value
	 * @return the hex string representation
	 */
	public static String toHexString(final BigInteger value) {
		return "0x" + value.toString(16).toUpperCase();
	}
}