package de.htw.ds.http;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <p>This class models HTTP connection handlers that are spawned whenever
 * a new connection is established. Note that all instances implement the
 * HTTP 1.1 connection keep-alive feature, and request path mapping. Further
 * features depend on the given resource flavor.</p>
 */
public final class HttpConnectionHandler implements Runnable, Closeable {
	private static final int TCP_PACKET_SIZE = 0x500;
	private static final long KEEP_ALIVE_MILLIES = 5000;
	private static final AtomicLong LAST_HANDLER_ID = new AtomicLong();

	private final long identity;
	private final Socket connection;
	private final HttpServer.ResourceFlavor resourceFlavor;
	private final Path contextPath;
	private final Map<String,String> requestPathMappings;


	/**
	 * Creates a new instance.
	 * @param connection the socket connection
	 * @param resourceFlavor the resource flavor
	 * @param contextPath the context directory path
	 * @param requestPathMappings the request path mappings
	 * @throws NullPointerException if one of the given arguments if <tt>null</tt>
	 */
	public HttpConnectionHandler(final Socket connection, final HttpServer.ResourceFlavor resourceFlavor, final Path contextPath, final Map<String,String> requestPathMappings) {
		super();
		if (connection == null | resourceFlavor == null | contextPath == null | requestPathMappings == null) throw new NullPointerException();

		this.identity = LAST_HANDLER_ID.incrementAndGet();
		this.connection = connection;
		this.resourceFlavor = resourceFlavor;
		this.contextPath = contextPath;
		this.requestPathMappings = requestPathMappings;
	}


	/**
	 * Closes this connection handler.
	 * @throws IOException if there is an I/O related problem
	 */
	public void close() throws IOException {
		this.connection.close();
	}


	/**
	 * Handles an HTTP connection, allowing for multiple requests per connection.
	 * Closes the connection before returning.
	 */
	public void run() {
		try {
			HttpRequestHeader requestHeader = null;
			HttpResponseHeader responseHeader = null;
			for (boolean hasNextRequest = true; hasNextRequest; hasNextRequest = this.hasNextRequest(requestHeader, responseHeader)) {
				try {
					try {
						final OutputStream outputStream = new BufferedOutputStream(this.connection.getOutputStream(), TCP_PACKET_SIZE);
						requestHeader = new HttpRequestHeader(this.connection.getInputStream(), null);
						responseHeader = new HttpResponseHeader(requestHeader.getVersion(), requestHeader.getRevision(), null, outputStream);
					} catch (final ProtocolException exception) {
						responseHeader = new HttpResponseHeader((byte) 1, (byte) 0, null, this.connection.getOutputStream());
						responseHeader.setType(HttpResponseHeader.Type.BAD_REQUEST);
						break;
					}

					final HttpRequestHeader.Type requestType = requestHeader.getType();
					final Path resourcePath = this.getResourcePath(requestHeader.getPath());
					final HttpRequestHandler requestHandler = this.resourceFlavor.createRequestHandler(this.identity, resourcePath);
					final Context context = this.resourceFlavor.createContext(requestHeader, responseHeader);
					Logger.getGlobal().log(Level.INFO, "Connection handler {0} answering {1} request for resource {2}.", new Object[] { this.identity, requestType, resourcePath });

					requestHandler.service(context, requestHeader, responseHeader);
				} finally {
					// strip any remaining request body content from the connection, if necessary
					if (requestHeader.getProperties().containsKey("Content-Length")) {
						try { requestHeader.getBodyInputStream().skip(Long.MAX_VALUE); } catch (final Exception exception) {}
					}
					// flush response body stream to make sure it is written to the connection 
					try { responseHeader.getBodyOutputStream().flush(); } catch (final Exception exception) {}
				}
			}
		} catch (final SocketException exception) {
			// connection has been reset by client, do nothing
		} catch (final Throwable exception) {
			Logger.getGlobal().log(Level.WARNING,  exception.getMessage(), exception);
		} finally {
			try { this.close(); } catch (final Throwable exception) {}
			Logger.getGlobal().log(Level.INFO, "Connection handler {0} closed.", this.identity);
		}
	}


	/**
	 * If the given path is <tt>null</tt>, empty, or "/", then it is
	 * replaced by "/default.html". Then returns a mapped path for the given
	 * path, or the given path is no mapping is available, both resolved
	 * within the receiver's context directory. 
	 * @param requestPath the request path, may be <tt>null</tt>
	 * @return the resolved and potentially mapped resource path
	 */
	private Path getResourcePath(String requestPath) {
		if (requestPath == null || requestPath.isEmpty() || "/".equals(requestPath)) {
			requestPath = "/default.html";
		}

		for (final Map.Entry<String,String> entry : this.requestPathMappings.entrySet()) {
			if (requestPath.startsWith(entry.getKey())) {
				requestPath = entry.getValue();
				break;
			}
		}

		return this.contextPath.resolve(requestPath.substring(1));
	}


	/**
	 * Returns <tt>true</tt> if an HTTP connection can be reused, <tt>false</tt> otherwise. Connections can
	 * never be reused if the last response body sent is of undetermined length, because in this case a client
	 * cannot decide when one response ends and another one begins. Also, connections must not be reused if
	 * the protocol is HTTP 1.0, because it's experimental definitions break in conjunction with proxy servers.
	 * Finally, connections cannot be reused if a client explicitly requests the connection to be closed,
	 * or if a new request doesn't arrive in time.
	 * @return <tt>true</tt> if an HTTP connection can be reused, <tt>false</tt> otherwise
	 * @throws NullPointerException if any of the given headers is <tt>null</tt>
	 * @throws IOException if there is an I/O related problem
	 */
	private boolean hasNextRequest(final HttpRequestHeader requestHeader, final HttpResponseHeader responseHeader) throws IOException {
		if (!responseHeader.getProperties().containsKey("Content-Length")) return false;
		if (requestHeader.isHttp10()) return false;
		if ("close".equalsIgnoreCase(requestHeader.getProperties().get("Connection"))) return false;

		for (long keepAliveMillies = KEEP_ALIVE_MILLIES; keepAliveMillies >= 0 && this.connection.getInputStream().available() == 0; --keepAliveMillies) {
			try { Thread.sleep(1); } catch (final InterruptedException exception) {}
		}
		return this.connection.getInputStream().available() > 0;
	}
}