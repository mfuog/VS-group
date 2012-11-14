package de.htw.ds.http;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import de.htw.ds.TypeMetadata;


/**
 * <p>This class models a dynamic HTTP server. It does implement the acceptor/service threading
 * pattern, the HTTP 1.1 connection keep-alive feature, and request path mapping. Note that by
 * default the resource "/default.html" is used instead of "/", but can be re-mapped as well.</p>
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class HttpServer2 extends HttpServer {

	/**
	 * Public constructor.
	 * @param servicePort the service port
	 * @param contextPath the context directory path
	 * @param requestPathMappings the request path mappings
	 * @throws NullPointerException if one of the given arguments if <tt>null</tt>
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use, or cannot be bound, or if the
	 *    given context path is not a directory
	 */
	public HttpServer2(final int servicePort, final Path contextPath, final Map<String,String> requestPathMappings) throws IOException {
		super(new HttpStaticResourceFlavor(), servicePort, contextPath, requestPathMappings);

		final Thread thread = new Thread(this, "http-acceptor");
		thread.setDaemon(true);
		thread.start();
	}


	/**
	 * Periodically blocks until a connection is established, spawning a new thread to handle the former subsequently.
	 */
	public void run() {
		while (true) {
			Socket connection = null;
			try {
				connection = this.getServiceSocket().accept();

				final HttpConnectionHandler connectionHandler = new HttpConnectionHandler(connection, this.getResourceFlavor(), this.getContextPath(), this.getRequestPathMappings());
				this.getExecutorService().execute(connectionHandler);
			} catch (final SocketException exception) {
				break;
			} catch (final Throwable exception) {
				try { connection.close(); } catch (final Throwable nestedException) {}
				try { Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception); } catch (final Throwable nestedException) {}
			}
		}
	}


	/**
	 * Application entry point. The given runtime parameters must be a service port,
	 * and a server context directory. Optionally, additional path mappings (a->b)
	 * may be registered that cause resources to be accessible under multiple request paths.
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use, or cannot be bound,
	 *      or if the given context path is not a directory
	 */
	public static void main(final String[] args) throws IOException {
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final Path contextPath = Paths.get(args[1]).normalize();
		final Map<String,String> requestPathMappings = args.length >= 3 
			? HttpServer.parseRequestPathMappings(args, 2)
			: new HashMap<String,String>();

		try (HttpServer server = new HttpServer2(servicePort, contextPath, requestPathMappings)) {
			System.out.println("HTTP server running on one acceptor thread, type \"quit\" to stop.");
			server.printCommonWelcomeMessage(timestamp);
			server.waitForShutdownSignal();
		}
	}
}