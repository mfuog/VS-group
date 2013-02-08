package de.htw.ds.edge;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import de.htw.ds.http.Context;
import de.htw.ds.http.HttpRequestHandler;
import de.htw.ds.http.HttpRequestHeader;
import de.htw.ds.http.HttpResponseHeader;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;


/**
 * <p>HTTP request handler generating a redirect response towards a replica server.
 * The replica server is selected using time zone information.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class EdgeHandler implements HttpRequestHandler {
	private static final InetSocketAddress[] NODE_ADDRESSES = new InetSocketAddress[48];

	static {
		final Properties properties = new Properties();
		try (InputStream byteSource = EdgeHandler.class.getResourceAsStream("node-addresses.properties")) {
			properties.load(byteSource);

			for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
				final float timezoneOffset = Float.parseFloat(entry.getKey().toString());
				final InetSocketAddress socketAddress = new SocketAddress(entry.getValue().toString()).toInetSocketAddress();
				final int index = offsetToIndex(timezoneOffset);
				NODE_ADDRESSES[index] = socketAddress;
			}
		} catch (final IOException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}


	/**
	 * Handles an HTTP request and generates an HTTP response.
	 * @param context the optional server context, or <tt>null</tt>
	 * @param requestHeader the HTTP request header 
	 * @param responseHeader the HTTP response header
	 * @throws NullPointerException if one of the given headers is <tt>null</tt>
	 * @throws IOException if there's an I/O related problem
	 */
	public void service(final Context context, final HttpRequestHeader requestHeader, final HttpResponseHeader responseHeader) throws IOException {
		String resourcePath = requestHeader.getParameters().containsKey("resourcePath")
			? requestHeader.getParameters().get("resourcePath")
			: "/";
		if (!resourcePath.startsWith("/")) resourcePath =  "/" + resourcePath;

		float timezoneOffset;
		try {
			timezoneOffset = Float.parseFloat(requestHeader.getParameters().get("timezoneOffset"));
		} catch (final Exception exception) {
			timezoneOffset = 0;
		}

		final InetSocketAddress nodeAddress = NODE_ADDRESSES[offsetToIndex(timezoneOffset)];
		final URI redirectURI;
		try {
			redirectURI = new URI("http", null, nodeAddress.getHostName(), nodeAddress.getPort(), resourcePath, null, null);
		} catch (URISyntaxException exception) {
			responseHeader.setType(HttpResponseHeader.Type.BAD_REQUEST);
			return;
		}

		responseHeader.setType(HttpResponseHeader.Type.REDIRECT);
		responseHeader.getProperties().put("Location", redirectURI.toASCIIString());
	}


	/**
	 * Returns an index for the given timezone offset.
	 * @param timezoneOffset a timezone offset in hours
	 * @return an index within range [0, 47]
	 */
	private static int offsetToIndex(float timezoneOffset) {
		while (timezoneOffset <  -12.0f) timezoneOffset += 24.0f;
		while (timezoneOffset >=  12.0f) timezoneOffset -= 24.0f;
		return Math.round(2.0f * (timezoneOffset + 12.0f));
	}
}