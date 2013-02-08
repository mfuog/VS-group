package de.htw.ds.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.ws.WebServiceException;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;


/**
 * <p>Chat server wrapping a POJO chat service with everything needed for CCP, RMI and SOAP
 * communication. Note that every method invocation causes an individual thread to be spawned.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class MultiProtocolChatServer {
	private static final String SERVICE_MESSAGE = "%s chat service URI is %s.\n";


	/**
	 * Application entry point. The given runtime parameters must be the service port
	 * for RMI, the service port for SOAP, and the service port for CCP.
	 * @param args the given runtime arguments
	 * @throws URISyntaxException if the given service name cannot be used to construct
	 *    a valid service URI
	 * @throws IOException if the RMI or CCP service port is already in use
	 * @throws WebServiceException if the given SOAP port is already in use
	 */
	public static void main(final String[] args) throws  URISyntaxException, IOException, WebServiceException {
		final long timestamp = System.currentTimeMillis();
		final int servicePortWSDL = Integer.parseInt(args[0]);
		final int servicePortRMI = Integer.parseInt(args[1]);
		final int servicePortCCP = Integer.parseInt(args[2]);
		final String serviceName = args[3];

		final URI ccpServiceURI = new URI("ccp", null, SocketAddress.getLocalAddress().getCanonicalHostName(), servicePortCCP, "/", null, null);

		final ChatService chatService = new ChatService(50);
		try (
			RpcChatServer wsdlServer = new RpcChatServer(servicePortWSDL, serviceName, chatService);
			RmiChatServer rmiServer = new RmiChatServer(servicePortRMI, serviceName, chatService);
			CcpChatServer ccpServer = new CcpChatServer(ccpServiceURI.getPort(), chatService);
		) {
			System.out.format("Multi-protocol chat server running, type \"quit\" to stop.\n");
			System.out.format(SERVICE_MESSAGE, "JAX-WS", wsdlServer.getServiceURI());
			System.out.format(SERVICE_MESSAGE, "Java-RMI", rmiServer.getServiceURI());
			System.out.format(SERVICE_MESSAGE, "CCP", ccpServer.getServiceURI());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(charSource.readLine())); } catch (final IOException exception) {}
		}
	}
}