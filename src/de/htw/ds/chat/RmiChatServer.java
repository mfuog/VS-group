package de.htw.ds.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;


/**
 * <p>Chat server wrapping a POJO chat service with everything needed for RMI
 * communication, implemented by the standard Java-RMI API. Note that every RMI
 * method invocation causes an individual thread to be spawned.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class RmiChatServer implements RmiChatService, AutoCloseable {

	private final URI serviceURI;
	private final ChatService delegate;


	/**
	 * Creates a new instance and exports it to the given service port.
	 * @param servicePort the service port
	 * @param serviceName the service name
	 * @param maxEntries the maximum number of chat entries
	 * @throws IllegalArgumentException if the given service port is outside
	 *    it's allowed range, if the service name is illegal, or if the given
	 *    maximum number of chat entries is negative
	 * @throws RemoteException if export fails
	 */
	public RmiChatServer(final int servicePort, final String serviceName, final int maxEntries) throws RemoteException {
		this(servicePort, serviceName, new ChatService(maxEntries));
	}


	/**
	 * Creates a new instance and exports it to the given service port.
	 * @param delegate the protocol independent chat service delegate
	 * @throws NullPointerException if the given delegate is null
	 * @throws IllegalArgumentException if the given service port is outside
	 *    it's allowed range, or if the service name is illegal
	 * @throws RemoteException if export fails
	 */
	public RmiChatServer(final int servicePort, final String serviceName, final ChatService delegate) throws RemoteException {
		super();
		if (delegate == null) throw new NullPointerException();
		if (servicePort <= 0 | servicePort > 0xFFFF) throw new IllegalArgumentException();

		try {
			this.serviceURI = new URI("rmi", null, SocketAddress.getLocalAddress().getCanonicalHostName(), servicePort, "/" + serviceName, null, null);
		} catch (final URISyntaxException exception) {
			throw new IllegalArgumentException();
		}

		this.delegate = delegate;

		// opens service socket, starts RMI acceptor-thread,
		// creates and associates anonymous proxy class+instance
		UnicastRemoteObject.exportObject(this, servicePort);

		// allows remote distribution of proxy clones by naming lookup
		final Registry registry = LocateRegistry.createRegistry(servicePort);
		registry.rebind(this.serviceURI.getPath().substring(1), this);
	}


	/**
	 * Closes this server.
	 */
	public void close() {
		try {
			// prevents remote distribution of more proxy clones by naming lookup
			final Registry registry = LocateRegistry.getRegistry(this.serviceURI.getPort());
			registry.unbind(this.serviceURI.getPath().substring(1));
		} catch (final Exception exception) {
			// do nothing;
		}

		try {
			// removes association with proxy instance,
			// stops RMI acceptor-thread, closes service socket
			UnicastRemoteObject.unexportObject(this, true);
		} catch (final NoSuchObjectException exception) {
			// do nothing
		}
	}


	/**
	 * Returns the service URI.
	 * @return the service URI
	 */
	public URI getServiceURI() {
		return this.serviceURI;
	}


	/**
	 * {@inheritDoc}
	 * Delegates the addEntry method to the service implementation.
	 */
	public long addEntry(final String alias, final String content) {
		return this.delegate.addEntry(alias, content);
	}


	/**
	 * {@inheritDoc}
	 * Delegates the removeEntry method to the service implementation.
	 */
	public boolean removeEntry(final ChatEntry entry) {
		return this.delegate.removeEntry(entry);
	}


	/**
	 * {@inheritDoc}
	 * Delegates the getEntries method to the service implementation.
	 */
	public ChatEntry[] getEntries() {
		return this.delegate.getEntries();
	}


	/**
	 * Application entry point. The given runtime parameters must be a service port,
	 * and a service name.
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if the given service port is outside
	 *    it's allowed range, if the service name is illegal, or if the given
	 *    maximum number of chat entries is negative
	 * @throws RemoteException if the service port is already in use, or the
	 *    server class does not implement a valid remote interfaces
	 */
	public static void main(final String[] args) throws RemoteException {
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final String serviceName = args[1];

		try (RmiChatServer server = new RmiChatServer(servicePort, serviceName, 50)) {
			System.out.format("Java-RMI chat server running, type \"quit\" to stop.\n");
			System.out.format("Service URI is \"%s\".\n", server.getServiceURI());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(charSource.readLine())); } catch (final IOException exception) {}
		}
	}
}