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
import de.sb.javase.TypeMetadata;


/**
 * <p>This class models a static HTTP server. It does implement the  HTTP 1.1 connection keep-alive
 * feature, and request path mapping. Note that by default the resource "/default.html" is used
 * instead of "/", but can be re-mapped as well.</p>
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class HttpServer1 extends HttpServer {

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
	public HttpServer1(final int servicePort, final Path contextPath, final Map<String,String> requestPathMappings) throws IOException {
		super(new HttpStaticResourceFlavor(), servicePort, contextPath, requestPathMappings);
	}


	/**
	 * Periodically blocks until a connection is established, spawning a new thread to handle the former subsequently.
	 */
	public void run() {
		while (true) {
			try (Socket connection = this.getServiceSocket().accept()) {
				final HttpConnectionHandler connectionHandler = new HttpConnectionHandler(connection, this.getResourceFlavor(), this.getContextPath(), this.getRequestPathMappings());
				connectionHandler.run();
			} catch (final SocketException exception) {
				break;
			} catch (final Exception exception) {
				try { Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception); } catch (final Exception nestedException) {}
			}
		}
	}


	/**
	 * Application entry point. The given runtime parameters must be a service port,
	 * a service thread count, and a server context directory. Optionally, additional path
	 * mappings (a->b) may be registered that cause resources to be accessible under
	 * multiple request paths.
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use, or cannot be bound,
	 *      or if the given context path is not a directory
	 */
	public static void main(final String[] args) throws IOException {
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final int serviceThreadCount = Integer.parseInt(args[1]);
		final Path contextPath = Paths.get(args[2]).normalize();
		final Map<String,String> requestPathMappings = args.length >= 4 
			? HttpServer.parseRequestPathMappings(args, 3)
			: new HashMap<String,String>();

		try (HttpServer server = new HttpServer1(servicePort, contextPath, requestPathMappings)) {
			for (int threadIndex = 0; threadIndex < serviceThreadCount; ++threadIndex) {
				final Thread thread = new Thread(server, "http-service-" + threadIndex);
				thread.setDaemon(true);
				thread.start();
			}

			System.out.format("HTTP server running on %s service threads, type \"quit\" to stop.\n", serviceThreadCount);
			server.printCommonWelcomeMessage(timestamp);
			server.waitForShutdownSignal();
		}
	}
}