package de.htw.ds.share;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.ws.Service;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;
import de.sb.javase.util.PivotFilter;
import de.sb.javase.xml.Namespaces;


/**
 * <p>Allows periodic collection of peer address information from known peers.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class PeerWatcher {
	private static final String LOG_PEER_COUNT = "Current peer count: {0}.";
	private static final String LOG_COLLECT_BEGIN = "Collecting peer addresses from peer {0}.";
	private static final String LOG_COLLECT_FAILED = "Failed to contact peer {0}, peer removed.";

	private final SocketAddress localServiceAddress;
	private final PivotFilter<SocketAddress,Long> timePeerFilter;
	private final Set<SocketAddress> peerAddresses;
	private volatile SocketAddress[] peerAddressSnapshot;


	/**
	 * Public constructor.
	 * @param localServiceAddress the local service address
	 * @param timePeerFilter the timestamp based peer filter
	 * @param peerAddresses a var-arg list of bootstrap peer addresses
	 * @throws NullPointerException if any of the given arguments is <tt>null</tt>
	 */
	public PeerWatcher(final SocketAddress localServiceAddress, final PivotFilter<SocketAddress,Long> timePeerFilter, final SocketAddress... peerAddresses) {
		super();
		if (localServiceAddress == null | timePeerFilter == null) throw new NullPointerException();

		this.localServiceAddress = localServiceAddress;
		this.peerAddresses = Collections.synchronizedSet(new HashSet<SocketAddress>());
		this.timePeerFilter = timePeerFilter;

		for (final SocketAddress peerAddress : peerAddresses) {
			if (peerAddress != null && !peerAddress.equals(localServiceAddress) && (peerAddress.isResolved() || peerAddress.resolve())) {
				this.peerAddresses.add(peerAddress);
			}
		}
		this.updatePeerAddressSnapshot();
	}


	/**
	 * Returns the peer addresses.
	 * @return the peer addresses
	 */
	public Set<SocketAddress> getPeerAddresses() {
		return this.peerAddresses;
	}


	/**
	 * Returns the latest snapshot of shuffled peer address.
	 * @return the latest peer addresses snapshot
	 */
	public SocketAddress[] getPeerAddressSnapshot() {
		return this.peerAddressSnapshot;
	}


	/**
	 * Filters the peer address snapshot by selecting those peers whose address
	 * is "nearest" to current system time, in order to contact these peers for
	 * additional peer addresses. Adds those peer addresses returned to the
	 * server's live peer address collection that are neither <tt>null</tt>, nor
	 * the server's local service address, nor unresolvable.
	 * As all peer services will behave this way, some file share servers with
	 * addresses that happen to be "near" current system time will become the
	 * center of network attention when it comes to providing peer addresses.
	 * As a side effect, they will collect the requestor's service addresses,
	 * and therefore quickly know most active peers. As time progresses, other
	 * file servers will become the center of attention, preventing the network
	 * from becoming vulnerable to fake file share servers at specific addresses.
	 */
	public void updatePeerAddresses() {
		final SocketAddress[] contactAddresses = this.timePeerFilter.filter(this.peerAddressSnapshot, System.currentTimeMillis());

		for (final SocketAddress contactAddress : contactAddresses) {
			Logger.getGlobal().log(Level.FINER, LOG_COLLECT_BEGIN, contactAddress);
			try {
				final URL wsdlLocator = FileShareResources.serviceWsdlLocator(contactAddress);
				final Service proxyFactory = Service.create(wsdlLocator, Namespaces.toQualifiedName(FileShareService.class));
				final FileShareService proxy = proxyFactory.getPort(FileShareService.class);

				final SocketAddress[] socketAddresses = proxy.exchangePeerAddresses(this.localServiceAddress);
				for (final SocketAddress socketAddress : socketAddresses) {
					if (!socketAddress.equals(this.localServiceAddress) && (socketAddress.isResolved() || socketAddress.resolve())) {
						this.peerAddresses.add(socketAddress);
					}
				}
			} catch (final Exception exception) {
				this.peerAddresses.remove(contactAddress);
				Logger.getGlobal().log(Level.FINER, LOG_COLLECT_FAILED, contactAddress);
			}
		}

		this.updatePeerAddressSnapshot();
		Logger.getGlobal().log(Level.INFO, LOG_PEER_COUNT, this.peerAddressSnapshot.length);
	}


	/**
	 * Creates a new peer address snapshot. Note that without centralized snapshot
	 * shuffling every server/watcher or monitor method working with peer addresses
	 * would have to constantly synchronize the peer address set in order to create
	 * a private snapshot, and then shuffle it to achieve statistical spread. As the
	 * peer address set can become quite huge, and those tasks typically don't require
	 * the newest available data, this would soon become very wasteful in terms of
	 * memory and processor utilization.
	 */
	private void updatePeerAddressSnapshot() {
		final SocketAddress[] snapshot = this.peerAddresses.toArray(new SocketAddress[0]);
		Collections.shuffle(Arrays.asList(snapshot));
		this.peerAddressSnapshot = snapshot;
	}
}