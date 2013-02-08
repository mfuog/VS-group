package de.htw.ds.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.BinaryTransporter;
import de.sb.javase.io.SocketAddress;


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
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class TcpSwitch implements Runnable, AutoCloseable {
	// workaround for Java7 bug initializing global logger without parent!
	static { LogManager.getLogManager(); }

	private static final Random RANDOMIZER = new Random();
	private static final int MAX_PACKET_SIZE = 0xFFFF;

	private final ServerSocket serviceSocket;
	private final ExecutorService executorService;
	private final InetSocketAddress[] nodeAddresses;
	private final boolean sessionAware;


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
	public TcpSwitch(final int servicePort, final boolean sessionAware, final InetSocketAddress... nodeAddresses) throws IOException {
		super();
		if (nodeAddresses.length == 0) throw new IllegalArgumentException();

		this.serviceSocket = new ServerSocket(servicePort);
		this.executorService = Executors.newCachedThreadPool();
		this.nodeAddresses = nodeAddresses;
		this.sessionAware = sessionAware;

		// start acceptor thread
		final Thread thread = new Thread(this, "tcp-acceptor");
		thread.setDaemon(true);
		thread.start();
	}


	/**
	 * Closes this server.
	 */
	public void close() {
		try { this.serviceSocket.close(); } catch (final Exception exception) {}
		this.executorService.shutdown();
	}


	/**
	 * Returns the service port.
	 * @return the service port
	 */
	public int getServicePort() {
		return this.serviceSocket.getLocalPort();
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
			} catch (final Exception exception) {
				try { clientConnection.close(); } catch (final Exception nestedException) {}
				Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception);
			}
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
			Socket serverConnection;
			try {
				serverConnection = this.createServerConnection();
			} catch (final IOException exception) {
				Logger.getGlobal().log(Level.WARNING, "Cannot connect to any peers, request discarded!");
				try { this.clientConnection.close(); } catch (final Exception nestedException) {}
				return;
			}

			try {
				final InetSocketAddress clientSocketAddress = new InetSocketAddress(this.clientConnection.getInetAddress(), this.clientConnection.getPort());
				final InetSocketAddress serverSocketAddress = new InetSocketAddress(serverConnection.getInetAddress(), serverConnection.getPort());
				Logger.getGlobal().log(Level.INFO, "Routing between {0} and {1}.", new Object[] { clientSocketAddress, serverSocketAddress });

				final Runnable requestTransporter = new BinaryTransporter(true, MAX_PACKET_SIZE, this.clientConnection.getInputStream(), serverConnection.getOutputStream());
				TcpSwitch.this.executorService.execute(requestTransporter);

				final Runnable responseTransporter = new BinaryTransporter(true, MAX_PACKET_SIZE, serverConnection.getInputStream(), this.clientConnection.getOutputStream());
				TcpSwitch.this.executorService.execute(responseTransporter);
			} catch (final Exception exception) {
				try { serverConnection.close(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
				try { this.clientConnection.close(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
				Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception);
			}
		}


		/**
		 * Creates a TCP/IP connection with a selected service node.
		 * @return a TCP/IP server connection
		 * @throws IOException if there is a TCP/IP related problem
		 */
		private Socket createServerConnection() throws IOException {
			if (TcpSwitch.this.sessionAware) {
				final InetAddress clientAddress = this.clientConnection.getInetAddress();
				final Random scrambler = new Random(clientAddress.hashCode());
				final int selectionIndex = scrambler.nextInt(TcpSwitch.this.nodeAddresses.length);
				final InetSocketAddress nodeAddress = TcpSwitch.this.nodeAddresses[selectionIndex];
				return new Socket(nodeAddress.getHostName(), nodeAddress.getPort());
			} else {
				final List<InetSocketAddress> nodeAddresses = new ArrayList<>(Arrays.asList(TcpSwitch.this.nodeAddresses));
				while (!nodeAddresses.isEmpty()) {
					final int selectionIndex = RANDOMIZER.nextInt(nodeAddresses.size());
					try {
						final InetSocketAddress nodeAddress = nodeAddresses.get(selectionIndex);
						return new Socket(nodeAddress.getHostName(), nodeAddress.getPort());
					} catch (final IOException exception) {
						nodeAddresses.remove(selectionIndex);
					}
				}
				throw new UnknownHostException();
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

		try (TcpSwitch server = new TcpSwitch(servicePort, sessionAware, nodeAddresses.toArray(new InetSocketAddress[0]))) {
			// print welcome message
			System.out.println("TCP switch running on one acceptor thread, enter \"quit\" to stop.");
			System.out.format("Service port is %s.\n", server.getServicePort());
			System.out.format("Session awareness is %s.\n", sessionAware);
			System.out.println("The following node addresses have been registered:");
			for (final InetSocketAddress nodeAddress : nodeAddresses) {
				System.out.println(nodeAddress);
			}
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			// wait for stop signal on System.in
			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(charSource.readLine())); } catch (final IOException exception) {}
		}
	}
}