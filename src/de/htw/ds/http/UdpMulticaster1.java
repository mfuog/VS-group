package de.htw.ds.http;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Map;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.Serializables;


/**
 * <p>A map multicaster registers against a callback map and sends out every change within
 * said map to a group of network listeners, using UDP multicast. The multicaster itself is
 * one such listener and modifies it's target map from every valid multicast packet that has
 * not originated from the same process. Note that the target map must be the map underlying
 * a callback map, NOT the callback map itself - otherwise the network will get flooded by
 * an exponentially growing number of endlessly recursive UDP multicasts!</p>
 * <p>This implementation realizes a global scope push design where new values are
 * distributed to all cluster servers using UDP multicast messages.</p>
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class UdpMulticaster1 implements Runnable, AutoCloseable, VetoableChangeListener {
	private static final String PROCESS_IDENTITY = ManagementFactory.getRuntimeMXBean().getName();
	private static final int MAX_PACKET_SIZE = 0xFFFF;

	private final MulticastSocket serviceSocket;
	private final InetSocketAddress multicastSocketAddress;
	private final Map<String,Serializable> targetMap;


	/**
	 * Creates a new map multi-caster. Note that the given target map must be the map underlying
	 * a callback map, NOT the callback map itself - otherwise the network will get flooded by
	 * an exponentially growing number of endlessly recursive UDP multicasts! The following ranges of
	 * IP addresses are valid multicast addresses:<ul>
	 * <li>IPv4: 224.0.0.0 through 239.255.255.255</li>
	 * <li>IPv6: ff00:0000:0000:0000:0000:0000:0000:0000 through ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff</li>
	 * </ul>
	 * @param multicastSocketAddress the multicast socket-address
	 * @param targetMap the target map
	 * @throws IOException if there is an I/O related problem
	 * @throws NullPointerException if one of the given values is null
	 */
	public UdpMulticaster1(final InetSocketAddress multicastSocketAddress, final Map<String,Serializable> targetMap) throws IOException {
		super();
		if (targetMap == null) throw new NullPointerException();

		this.serviceSocket = new MulticastSocket(multicastSocketAddress.getPort());
		this.serviceSocket.joinGroup(multicastSocketAddress.getAddress());
		this.multicastSocketAddress = multicastSocketAddress;
		this.targetMap = targetMap;

		final Thread thread = new Thread(this, "multicast-listener");
		thread.setDaemon(true);
		thread.start();
	}


	/**
	 * Closes the receiver and implicitly ends the multicast socket's listener thread. 
	 */
	public void close() {
		this.serviceSocket.close();
	}


	/**
	 * Blocks periodically until a new UDP multicast message is received.
	 * Deserializes the message data received, and updates the target map
	 * accordingly.
	 */
	public void run() {
		final DatagramPacket packet = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);

		while(true) {
			try {
				this.serviceSocket.receive(packet);

				final Serializable[] objects = Serializables.deserializeObjects(packet.getData(), 0, packet.getLength()); 
				final String senderProcessIdentity = (String) objects[0];
				final String key = (String) objects[1];
				final Serializable value = (Serializable) objects[2];

				if (!PROCESS_IDENTITY.equals(senderProcessIdentity)) {
					if (value == null) {
						this.targetMap.remove(key);
					} else {
						this.targetMap.put(key, value);
					}
				}
			} catch (final SocketException exception) {
				break;
			} catch (final Exception exception) {
				exception.printStackTrace();
			}
		}
	}


	/**
	 * The relevant event data is sent to all active multicast listeners, so their
	 * associated target maps can be adjusted accordingly. The message data sent contains
	 * a serialized process identity, a serialized key, and a serialized value (or null
	 * if the key shall be removed)
	 * @throws PropertyVetoException if the message cannot be created
	 */
	public void vetoableChange(final PropertyChangeEvent event) throws PropertyVetoException {
		if (event.getOldValue() == null && event.getNewValue() == null) return;

		try {
			final byte[] messageData = Serializables.serializeObjects(PROCESS_IDENTITY, event.getPropertyName(), (Serializable) event.getNewValue());
			if (messageData.length > MAX_PACKET_SIZE) throw new InvalidObjectException("object too large");

			try (DatagramSocket socket = new DatagramSocket()) {
				socket.send(new DatagramPacket(messageData, messageData.length, this.multicastSocketAddress));
			}
		} catch (final Exception exception) {
			throw new PropertyVetoException(exception.getMessage(), event);
		}
	}
}