package de.htw.ds.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;


/**
 * <p>P2P chat peer based on UDP multicast messages, using a multicast acceptor thread.</p>
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class UdpChatPeer implements Runnable, AutoCloseable {
	private static final int MAX_PACKET_SIZE = 0xFFFF;
	private static final String PROCESS_NAME = ManagementFactory.getRuntimeMXBean().getName();
	private static final String TERMINATION_MESSAGE = "quit";

	private final MulticastSocket serviceSocket;
	private final InetAddress groupAddress;


	/**
	 * Creates a new instance based on a multicast socket opened on the
	 * given multicast port, joining the given multicast address's
	 * transmission group. Note that in opposition to TCP ports, UDP
	 * multicast ports are NOT process-exclusive resources and can be
	 * opened by multiple processes at once!
	 * @param multicastSocketAddress the multicast address and port
	 * @throws IOException if there is an I/O related problem
	 */
	public UdpChatPeer(final InetSocketAddress multicastSocketAddress) throws IOException {
		this.serviceSocket = new MulticastSocket(multicastSocketAddress.getPort());
		this.serviceSocket.joinGroup(multicastSocketAddress.getAddress());
		this.groupAddress = multicastSocketAddress.getAddress();
		new Thread(this, "udp-acceptor").start();
	}


	/**
	 * Leaves the multicast transmission group and closes the service socket.
	 */
	public void close() {
		try { this.serviceSocket.leaveGroup(this.groupAddress); } catch (final Exception exception) {}
		this.serviceSocket.close();
	}


	/**
	 * Periodically blocks until a UDP packet arrives, handles the latter subsequently.
	 */
	public void run() {
		final DatagramPacket packet = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);
		while(true) {
			try {
				this.serviceSocket.receive(packet);
				final String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
				System.out.println(message);
			} catch (final SocketException exception) {
				break;
			} catch (final Exception exception) {
				try { exception.printStackTrace(); } catch (final Exception nestedException) {}
			}
		}
	}


	/**
	 * Starts the P2P chat based on a UDP multicast address and port. Valid UDP multicast addresses are:<ul>
	 * <li>IPv4: 224.0.0.0 through 239.255.255.255</li>
	 * <li>IPv6: ff00:0000:0000:0000:0000:0000:0000:0000 through ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff</li>
	 * </ul>
	 * @param args the destination socket-address (address:port)
	 * @throws SocketException if there is a connectivity problem
	 * @throws IOException if there is an I/O related problem
	 */
	public static void main(final String[] args) throws SocketException, IOException {
		final InetSocketAddress multicastSocketAddress = new SocketAddress(args[0]).toInetSocketAddress();

		try (UdpChatPeer peer = new UdpChatPeer(multicastSocketAddress)) {
			System.out.println("UDP multicast chat peer running on one acceptor thread, enter \"quit\" to stop.");
			System.out.format("Multicast socket-address is %s.\n", multicastSocketAddress);
			System.out.format("Process name is %s, messages:\n", PROCESS_NAME);
			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));

			try (DatagramSocket socket = new DatagramSocket()) {
				byte[] messageData = String.format("%s joins the chat group!", PROCESS_NAME).getBytes("UTF-8");
				socket.send(new DatagramPacket(messageData, messageData.length, multicastSocketAddress));

				for (String line = charSource.readLine(); !TERMINATION_MESSAGE.equals(line); line = charSource.readLine()) {
					messageData = String.format("%s: %s", PROCESS_NAME, line).getBytes("UTF-8");
					socket.send(new DatagramPacket(messageData, messageData.length, multicastSocketAddress));
				}
	
				messageData = String.format("%s quits the chat group!", PROCESS_NAME).getBytes("UTF-8");
				socket.send(new DatagramPacket(messageData, messageData.length, multicastSocketAddress));
			}
		}
	}
}