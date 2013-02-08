package de.htw.ds.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;


/**
 * <p>Chat server wrapping a POJO chat service with everything needed for SOAP over HTTP
 * communication, implemented by the standard JAX-WS API. Note that every SOAP method
 * invocation causes an individual thread to be spawned.
 */
@WebService (endpointInterface="de.htw.ds.chat.RpcChatService", serviceName="RpcChatService")
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class RpcChatServer implements RpcChatService, AutoCloseable {

	private final URI serviceURI;
	private final Endpoint endpoint;
	private final ChatService delegate;


	/**
	 * Public constructor.
	 * @param servicePort the service port
	 * @param serviceName the service name
	 * @param maxEntries the maximum number of chat entries
	 * @throws NullPointerException if any of the given arguments is null
	 * @throws IllegalArgumentException if the given service port is outside
	 *    it's allowed range, if the service name is illegal, or if the given
	 *    maximum number of chat entries is negative
	 * @throws WebServiceException if the service URI's port is already in use
	 */
	public RpcChatServer(final int servicePort, final String serviceName, final int maxEntries) {
		this(servicePort, serviceName, new ChatService(maxEntries));
	}


	/**
	 * Creates a new instance, creates a SOAP endpoint for it, and publishes
	 * the server instance on said endpoint.
	 * @param servicePort the service port
	 * @param serviceName the service name
	 * @param delegate the protocol independent chat service delegate
	 * @throws NullPointerException if any of the given arguments is null
	 * @throws IllegalArgumentException if the given service port is outside
	 *    it's allowed range, or if the service name is illegal
	 * @throws WebServiceException if the service URI's port is already in use
	 * @see SOAPBinding
	 */
	public RpcChatServer(final int servicePort, final String serviceName, final ChatService chatService) {
		super();
		if (servicePort <= 0 | servicePort > 0xFFFF) throw new IllegalArgumentException();

		try {
			this.serviceURI = new URI("http", null, SocketAddress.getLocalAddress().getCanonicalHostName(), servicePort, "/" + serviceName, null, null);
		} catch (final URISyntaxException exception) {
			throw new IllegalArgumentException();
		}
		this.delegate = chatService;

		// non-standard SOAP1.2 binding: "http://java.sun.com/xml/ns/jaxws/2003/05/soap/bindings/HTTP/"
		this.endpoint = Endpoint.create(SOAPBinding.SOAP11HTTP_BINDING, this);
		this.endpoint.publish(this.serviceURI.toASCIIString());
	}


	/**
	 * Closes the receiver, thereby stopping it's SOAP endpoint.
	 */
	public void close() {
		this.endpoint.stop();
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
	 * Delegates to the service implementation.
	 * @throws NullPointerException {@inheritDoc} 
	 */
	public long addEntry(final String alias, final String content) {
		return this.delegate.addEntry(alias, content);
	}


	/**
	 * {@inheritDoc}
	 * Delegates to the service implementation.
	 */
	public boolean removeEntry(final ChatEntry entry) {
		return this.delegate.removeEntry(entry);
	}


	/**
	 * {@inheritDoc}
	 * Delegates to the service implementation.
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
	 * @throws WebServiceException if the given port is already in use
	 */
	public static void main(final String[] args) throws WebServiceException {
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final String serviceName = args[1];

		try (RpcChatServer server = new RpcChatServer(servicePort, serviceName, 50)) {
			System.out.format("Dynamic (bottom-up) JAX-WS chat server running, enter \"quit\" to stop.\n");
			System.out.format("Service URI is \"%s\".\n", server.getServiceURI().toASCIIString());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(charSource.readLine())); } catch (final IOException exception) {}
		}
	}
}