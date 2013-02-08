package de.htw.ds.http;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;
import de.sb.javase.util.CallbackMap;


/**
 * <p>This class models a dynamic, simplified HTTP application container.
 * It does implement the acceptor/service threading pattern, HTTP 1.1 connection keep-alive,
 * extended variable scopes (including cluster-wide global scope), plug-in HTTP handler
 * execution, plug-in handler compilation, and request path mapping.</p>
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class HttpContainer2 extends HttpServer {

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
	public HttpContainer2(final int servicePort, final Path contextPath, final Map<String,String> requestPathMappings) throws IOException {
		super(new HttpDynamicResourceFlavor(), servicePort, contextPath, requestPathMappings);

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
			} catch (final Exception exception) {
				try { connection.close(); } catch (final Exception nestedException) {}
				try { Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception); } catch (final Exception nestedException) {}
			}
		}
	}


	/**
	 * Application entry point. The given runtime parameters must be a service port,
	 * a server context directory, and a multicast socket address. Optionally, additional
	 * path mappings (a->b) may be registered that cause resources to be accessible under
	 * multiple request paths.
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF],
	 *    or if the given directory is not a directory
	 * @throws Exception if the given port is already in use, or cannot be bound, or if there's
	 *    a database related problem
	 */
	public static void main(final String[] args) throws Exception {
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final Path contextPath = Paths.get(args[1]).normalize();
		final InetSocketAddress multicastSocketAddress = new SocketAddress(args[2]).toInetSocketAddress();
		final Map<String,String> requestPathMappings = args.length >= 4 
			? HttpServer.parseRequestPathMappings(args, 3)
			: new HashMap<String,String>();

		final CallbackMap<String,Serializable> globalMap = HttpDynamicResourceFlavor.globalMap();
		try (UdpMulticaster2 listener = new UdpMulticaster2(multicastSocketAddress, globalMap.getDelegateMap())) {
			try (HttpServer server = new HttpContainer2(servicePort, contextPath, requestPathMappings)) {
				globalMap.getListeners().add(listener);

				System.out.println("HTTP container running on one acceptor thread, type \"quit\" to stop.");
				System.out.format("UDP multicast socket address is %s.\n", multicastSocketAddress);
				server.printCommonWelcomeMessage(timestamp);
				server.waitForShutdownSignal();
			} finally {
				try { globalMap.getListeners().remove(listener); } catch (final Exception exception) {}
			}
		}
	}
}