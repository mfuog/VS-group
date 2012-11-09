package de.htw.ds.tcp;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import de.htw.ds.TypeMetadata;
import de.htw.ds.util.BinaryTransporter;
import de.htw.ds.util.SocketAddress;


/**
 * <p>This class models a TCP monitor, i.e. a TCP router that mirrors all
 * information between two ports, while logging it at the same time.</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
@SuppressWarnings("unused")	//TODO: remove this line
public final class TcpMonitorSkeleton implements Runnable, Closeable {
	// workaround for Java7 bug initializing global logger without parent!
	static { LogManager.getLogManager(); }

	private static final Random RANDOMIZER = new Random();
	private static final String CONNECTION_ID_PATTERN = "%1$tF-%1$tT.%tL-%06d";
	private static final String LOG_FILE_PATTERN = "%s-%s.log";
	private static final int MAX_PACKET_SIZE = 0xFFFF;

	private final ServerSocket serviceSocket;
	private final ExecutorService executorService;
	private final InetSocketAddress forwardAddress;
	private final Path contextPath;


	/**
	 * Public constructor.
	 * @param servicePort the service port
	 * @param forwardAddress the forward address
	 * @param contextPath the context directory path
	 * @throws NullPointerException if the given socket-address of context directory is null
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use, or cannot be bound,
	 *    or the given context path is not a directory
	 */
	public TcpMonitorSkeleton(final int servicePort, final InetSocketAddress forwardAddress, final Path contextPath) throws IOException {
		super();
		if (forwardAddress == null) throw new NullPointerException();
		if (!Files.isDirectory(contextPath)) throw new NotDirectoryException(contextPath.toString());

		this.serviceSocket = new ServerSocket(servicePort);
		this.executorService = Executors.newCachedThreadPool();
		this.forwardAddress = forwardAddress;
		this.contextPath = contextPath;

		final Thread thread = new Thread(this, "tcp-acceptor");
		thread.setDaemon(true);
		thread.start();
	}


	/**
	 * Closes this server.
	 * @throws IOException if there is an I/O related problem
	 */
	public void close() throws IOException {
		this.serviceSocket.close();
	}


	/**
	 * Periodically blocks until a request arrives, handles the latter subsequently.
	 */
	public void run() {
		while (true) {
			Socket clientConnection = null;
			try {
				clientConnection = this.serviceSocket.accept();

				final ConnectionHandler connectionHandler = new ConnectionHandler(clientConnection);
				this.executorService.execute(connectionHandler);
			} catch (final SocketException exception) {
				break;
			} catch (final Throwable exception) {
				try { clientConnection.close(); } catch (final Throwable nestedException) {}
				try { Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception); } catch (final Throwable nestedException) {}
			}
		}
	}


	/**
	 * Application entry point. The given runtime parameters must be a service port,
	 * a server context directory, and the forward socket-address as address:port combination.
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use, or cannot be bound,
	 *    or the given context path is not a directory
	 */
	public static void main(final String[] args) throws IOException {
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final Path contextPath = Paths.get(args[1]).normalize();
		final InetSocketAddress forwardAddress = new SocketAddress(args[2]).toInetSocketAddress();

		try (TcpMonitorSkeleton server = new TcpMonitorSkeleton(servicePort, forwardAddress, contextPath)) {
			// print welcome message
			System.out.println("TCP monitor running on one acceptor thread, type \"quit\" to stop.");
			System.out.format("Service port is %s.\n", server.serviceSocket.getLocalPort());
			System.out.format("Context directory is %s.\n", contextPath);
			System.out.format("Forward address is %s.\n", forwardAddress.toString());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			// wait for stop signal on System.in
			final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(reader.readLine())); } catch (final IOException exception) {}
		}
	}


	/**
	 * <p>Instances of this inner class handle TCP client connections
	 * accepted by a TCP monitor.</p> 
	 */
	private class ConnectionHandler implements Runnable {
		final Socket clientConnection;


		/**
		 * Creates a new instance from a given client connection.
		 * @param clientConnection the connection
		 * @throws NullPointerException if the given connection is <tt>null</tt>
		 */
		public ConnectionHandler(final Socket clientConnection) {
			super();
			if (clientConnection == null) throw new NullPointerException();

			this.clientConnection = clientConnection;
		}


		/**
		 * Handles the client connection by transporting all data to a new
		 * server connection, and vice versa. Closes all connections upon
		 * completion.
		 */
		public void run() {
			final String connectionID = String.format(CONNECTION_ID_PATTERN, System.currentTimeMillis(), RANDOMIZER.nextInt(1000000));
			final Path requestLogPath = TcpMonitorSkeleton.this.contextPath.resolve(String.format(LOG_FILE_PATTERN, connectionID, "request"));
			final Path responseLogPath = TcpMonitorSkeleton.this.contextPath.resolve(String.format(LOG_FILE_PATTERN, connectionID, "response"));

			// TODO: Transport all content from the client connection's input stream into
			// both the server connection's output stream and an output stream created for
			// the respective request/response log file, and vice versa. You'll need to
			// open output streams for the log files first, and exceptions should be logged
			// using the class's logger.
			// Note that you'll need 1-2 new transporter threads to complete this tasks, as
			// you cannot foresee if the client or the server closes the connection, or if
			// the protocol communicated involves handshakes. Either case implies you'd
			// end up reading "too much" if you try to transport both communication directions
			// within this thread, creating a deadlock scenario!
			// Especially make sure that all connections and files are properly closed in
			// any circumstances! Note that closing one socket stream closes the underlying
			// socket connection as well. Also note that a SocketInputStream's read() method
			// will throw a SocketException when interrupted while blocking, which is "normal"
			// behavior and should be handled as if the read() Method returned -1!

			try (
				Socket serverConnection = new Socket(TcpMonitorSkeleton.this.forwardAddress.getAddress(), TcpMonitorSkeleton.this.forwardAddress.getPort())
			) {

			} catch (final Throwable exception) {
				Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception);
			}
		}
	}
}