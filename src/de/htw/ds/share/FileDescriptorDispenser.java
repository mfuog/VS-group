package de.htw.ds.share;

import java.math.BigInteger;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;
import de.sb.javase.util.PivotFilter;
import de.sb.javase.xml.Namespaces;


/**
 * <p>Dispenses file descriptor information among peers.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class FileDescriptorDispenser {

	private final ExecutorService taskExecutor;
	private final PivotFilter<SocketAddress,BigInteger> hashPeerFilter;


	/**
	 * Public constructor.
	 * @param taskExecutor the task executor
	 * @param hashPeerFilter the file-hash based peer filter
	 * @throws NullPointerException if any of the given arguments is <tt>null</tt>
	 */
	public FileDescriptorDispenser(final ExecutorService taskExecutor, final PivotFilter<SocketAddress,BigInteger> hashPeerFilter) {
		super();
		if (taskExecutor == null | hashPeerFilter == null) throw new NullPointerException();

		this.taskExecutor = taskExecutor;
		this.hashPeerFilter = hashPeerFilter;
	}


	/**
	 * Dispense file information to peers.
	 */
	public void dispenseFileDescriptors(final FileDescriptor[] fileDescriptors, final SocketAddress[] peerAddresses) {
		for (final FileDescriptor fileDescriptor : fileDescriptors) {
			final SocketAddress[] filteredPeerAddresses = this.hashPeerFilter.filter(peerAddresses, fileDescriptor.getContentHash());

			for (final SocketAddress peerAddress : filteredPeerAddresses) {
				this.taskExecutor.execute(new DispenserTask(peerAddress, fileDescriptor));
			}
		}
	}



	/**
	 * A file dispenser task is responsible to dispense a single
	 * file descriptor to a single peer service, and update the
	 * the local file descriptor with the information answered. 
	 */
	private final class DispenserTask implements Runnable {
		private final SocketAddress peerAddress;
		private final FileDescriptor fileDescriptor;


		/**
		 * Creates a new file dispenser task.
		 * @param peerAddress the peer address to be contacted
		 * @param fileDescriptor the file descriptor to be dispensed
		 */
		public DispenserTask(final SocketAddress peerAddress, final FileDescriptor fileDescriptor) {
			super();
			if (peerAddress == null | fileDescriptor == null) throw new NullPointerException();

			this.fileDescriptor = fileDescriptor;
			this.peerAddress = peerAddress;
		}


		/**
		 * Sends this task's file descriptor to this task's peer address, and
		 * updates said file descriptor with the information returned.
		 */
		public void run() {
			try {
				final URL wsdlLocator = FileShareResources.serviceWsdlLocator(this.peerAddress);
				final Service proxyFactory = Service.create(wsdlLocator, Namespaces.toQualifiedName(FileShareService.class));
				final FileShareService proxy = proxyFactory.getPort(FileShareService.class);

				final FileDescriptor peerDescriptor = proxy.addFileDescriptor(this.fileDescriptor);
				this.fileDescriptor.getSourceAddresses().addAll(peerDescriptor.getSourceAddresses());
				this.fileDescriptor.getFileNames().addAll(peerDescriptor.getFileNames());
			} catch (final WebServiceException exception) {
				// do nothing
			}
		}
	}
}