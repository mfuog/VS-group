package de.htw.ds.http;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import de.htw.ds.TypeMetadata;


/**
 * <p>This class demonstrates a minimal HTTP echo client. It requires a server name/address,
 * a server port, and an HTTP request path (leading slash must be present).</p>
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class HttpClient {

	/**
	 * Application entry point. The given runtime parameters must be a valid service
	 * URI pointing to an HTTP server.
	 * @param args the given runtime arguments
	 * @throws URISyntaxException if the given URI is malformed
	 * @throws IOException if there is an I/O related problem
	 */
	public static void main(final String[] args) throws URISyntaxException, IOException {
		final URI uri = new URI(args[0]);
		final String host = uri.getHost() == null ? "localhost" : uri.getHost();

		try (Socket connection = new Socket(host, uri.getPort() == -1 ? 80 : uri.getPort())) {
			final BufferedOutputStream outputStream = new BufferedOutputStream(connection.getOutputStream(), 1280);
			final HttpRequestHeader requestHeader = new HttpRequestHeader((byte) 1, (byte) 1, null, outputStream);
			requestHeader.setPath(uri.getPath() == null ? "/" : uri.getPath());
			requestHeader.getProperties().put("Host", host);
			requestHeader.getBodyOutputStream().flush();

			HttpResponseHeader responseHeader = new HttpResponseHeader(connection.getInputStream(), System.out);
			responseHeader.getBodyOutputStream().flush();
		}
	}
}