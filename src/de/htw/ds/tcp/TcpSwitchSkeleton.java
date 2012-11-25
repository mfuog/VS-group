package de.htw.ds.tcp;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import de.htw.ds.TypeMetadata;
import de.htw.ds.util.BinaryTransporter;
import de.htw.ds.util.SocketAddress;


/**
 * <p>This class models a TCP switch, i.e. a "spray router" for all kinds of
 * TCP oriented protocol connections. It routes incoming client requests
 * to it's given set of protocol servers, either randomly selected, or
 * determined by known session association. Note that while this implementation
 * routes all kinds of TCP protocols, a single instance is only able to route
 * one protocol type unless it's child servers support multi-protocol requests.</p>
 * <p>Session association is determined by receiving subsequent requests from
 * the same client, which may or may not be interpreted as being part of the
 * same session by the protocol server selected. However, two requests cannot
 * be part of the same session if they do not share the same request client
 * address! Note that this algorithm allows for protocol independence, but does
 * not work with clients that dynamically change their IP-address during a
 * session's lifetime.</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
@SuppressWarnings("unused")	// TODO: remove this line to see warnings!
public final class TcpSwitchSkeleton implements Runnable, Closeable {
	// workaround for Java7 bug initializing global logger without parent!
	static { LogManager.getLogManager(); }

	private static final Random RANDOMIZER = new Random();
	private static final int MAX_PACKET_SIZE = 0xFFFF;

	private final ServerSocket serviceSocket;
	private final ExecutorService executorService;
	private final InetSocketAddress[] nodeAddresses;
	private final Map<InetAddress,InetSocketAddress> sessions;


	/**
	 * Public constructor.
	 * @param servicePort the service port
	 * @param nodeAddresses the node addresses
	 * @param sessionAware true if the server is aware of sessions, false otherwise
	 * @throws NullPointerException if the given socket-addresses array is null
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF],
	 *    or the given socket-addresses array is empty
	 * @throws IOException if the given port is already in use, or cannot be bound
	 */
	public TcpSwitchSkeleton(final int servicePort, final boolean sessionAware, final InetSocketAddress... nodeAddresses) throws IOException {
		super();
		if (nodeAddresses.length == 0) throw new IllegalArgumentException();

		this.serviceSocket = new ServerSocket(servicePort);
		this.executorService = Executors.newCachedThreadPool();
		this.nodeAddresses = nodeAddresses;
		this.sessions = sessionAware ? Collections.synchronizedMap(new HashMap<InetAddress,InetSocketAddress>()) : null;

		// start acceptor thread
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


	public boolean isSessionAware() {
		return this.sessions != null;
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
				this.executorService.equals(connectionHandler);
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
	 * the session awareness, and the list of address:port combinations for the cluster nodes.
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF],
	 *    or there are no cluster nodes
	 * @throws IOException if the given port is already in use, or cannot be bound
	 */
	public static void main(final String[] args) throws IOException {
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final boolean sessionAware = Boolean.parseBoolean(args[1]);

		final Set<InetSocketAddress> nodeAddresses = new HashSet<>();
		for (int index = 2; index < args.length; ++index) {
			nodeAddresses.add(new SocketAddress(args[index]).toInetSocketAddress());
		}

		try (TcpSwitchSkeleton server = new TcpSwitchSkeleton(servicePort, sessionAware, nodeAddresses.toArray(new InetSocketAddress[nodeAddresses.size()]))) {
			// print welcome message
			System.out.println("TCP switch running on one acceptor thread, type \"quit\" to stop.");
			System.out.format("Service port is %s.\n",server.serviceSocket.getLocalPort());
			System.out.format("Session awareness is %s.\n", sessionAware);
			System.out.println("The following node addresses have been registered:");
			for (final InetSocketAddress nodeAddress : nodeAddresses) {
				System.out.println(nodeAddress);
			}
			System.out.format("Startup time is %sms.\n",System.currentTimeMillis() - timestamp);

			// wait for stop signal on System.in
			final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(reader.readLine())); } catch (final IOException exception) {}
		}
	}


	/**
	 * <p>Instances of this inner class handle TCP client connections
	 * accepted by a TCP switch.</p> 
	 */
	private class ConnectionHandler implements Runnable {
		final Socket clientConnection;


		/**
		 * Creates a new instance from a given client connection.
		 * @param clientConnection the connection
		 * @throws NullPointerException if the given connection is <tt>null</tt>
		 */
		public ConnectionHandler (final Socket clientConnection) {
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
			// TODO implement TCP routing here, and close the connections upon completion!
			// Note that you'll need 1-2 new transporter threads to complete this tasks, as
			// you cannot foresee if the client or the server closes the connection, or if
			// the protocol communicated involves handshakes. Either case implies you'd
			// end up reading "too much" if you try to transport both communication directions
			// within this thread, creating a deadlock scenario!
			// Especially make sure that all connections are properly closed in
			// any circumstances! Note that closing one socket stream closes the underlying
			// socket connection as well. Also note that a SocketInputStream's read() method
			// will throw a SocketException when interrupted while blocking, which is "normal"
			// behavior and should be handled as if the read() Method returned -1!
		}
	}
}