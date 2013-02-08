package de.htw.ds.share;

import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;


/**
 * <p>File descriptor class describing local and/or remote files. Such
 * descriptors can match meta-information against various aspects, and
 * determine if there is a match or not.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class FileDescriptor implements ContentDescriptor {

	private final BigInteger contentHash;
	private final long contentLength;
	private final Set<SocketAddress> sourceAddresses;
	private final Set<String> fileNames;
	private transient final Set<Path> localPaths;


	/**
	 * Protected no-arg constructor is required for JAX-B marshaling.
	 */
	protected FileDescriptor() {
		super();

		this.contentHash = null;
		this.contentLength = 0;
		this.sourceAddresses = null;
		this.fileNames = null;
		this.localPaths = Collections.synchronizedSet(new HashSet<Path>());
	}


	/**
	 * Constructs an instance for the given content hash-code.
	 * @param contentHash the content hash-code
	 * @param contentLength the content length
	 * @throws NullPointerException if the given hash-code is <tt>null</tt>
	 * @throws IllegalArgumentException if the given length is strictly negative
	 */
	public FileDescriptor(final BigInteger contentHash, final long contentLength) {
		super();
		if (contentHash == null) throw new NullPointerException();
		if (contentLength < 0) throw new IllegalArgumentException();

		this.contentHash = contentHash;
		this.contentLength = contentLength;
		this.sourceAddresses = Collections.synchronizedSet(new HashSet<SocketAddress>());
		this.fileNames = Collections.synchronizedSet(new HashSet<String>());
		this.localPaths = Collections.synchronizedSet(new HashSet<Path>());
	}


	/**
	 * Returns the content hash.
	 * @return the content hash
	 */
	public BigInteger getContentHash() {
		return this.contentHash;
	}


	/**
	 * Returns the content length.
	 * @return the content length
	 */
	public long getContentLength() {
		return this.contentLength;
	}


	/**
	 * Returns the source addresses, i.e. the service addresses of file
	 * share servers which manage at least one copy of this content.
	 * @return the source addresses
	 */
	public Set<SocketAddress> getSourceAddresses() {
		return this.sourceAddresses;
	}


	/**
	 * Returns the simple file names of this content throughout the net.
	 * @return the file names
	 */
	public Set<String> getFileNames() {
		return this.fileNames;
	}


	/**
	 * Returns the local file paths. Note that specific content may be stored
	 * within multiple local files, which is why all occurrences must be
	 * managed.
	 * @return the local file paths
	 */
	public Set<Path> getLocalPaths() {
		return this.localPaths;
	}


	/**
	 * Returns <tt>true</tt> if this descriptor represents a local
	 * file, <tt>false</tt> otherwise.
	 * @return whether or not this descriptor represents a local file
	 */
	public boolean isLocal() {
		return !this.localPaths.isEmpty() && !this.sourceAddresses.isEmpty();
	}


	/**
	 * Returns <tt>true</tt> if this descriptor represents a remote
	 * file, <tt>false</tt> otherwise.
	 * @return whether or not this descriptor represents a local file
	 */
	public boolean isRemote() {
		final int sourceAddressCount = this.sourceAddresses.size();
		return sourceAddressCount == 1 ? this.localPaths.isEmpty() : sourceAddressCount != 0;
	}


	/**
	 * Matches the receiver against the given criteria. A match's file name contains
	 * all the given name fragments, and ends with any of the given file types - both
	 * comparisons are case insensitive. A match's file length also exceeds or equals
	 * the given minimum length, and undercuts or equals the given maximum length.
	 * Any parameter may be <tt>null</tt>, which means the given criterion is omitted
	 * during the query.
	 * @param fragments the name fragments or <tt>null</tt>
	 * @param types the file types or </tt>null</tt>
	 * @param minSize the minimum file size, or </tt>null</tt>
	 * @param maxSize the maximum file size, or </tt>null</tt>
	 * @return whether or not this descriptor matches the given criteria
	 */
	public boolean match(final String[] fragments, final String[] types, final Long minSize, final Long maxSize) {
		if (minSize != null && this.getContentLength() < minSize) return false;
		if (maxSize != null && this.getContentLength() > maxSize) return false;

		final Set<String> fragmentSet = new HashSet<>(), typeSet = new HashSet<>();
		for (final String fragment : fragments == null ? new String[0] : fragments) {
			if (fragment != null) fragmentSet.add(fragment.toLowerCase());
		}
		for (final String type : types == null ? new String[0] : types) {
			if (type != null) typeSet.add(type.toLowerCase());
		}

		synchronized (this.fileNames) {
			for (final String fileName : this.fileNames) {
				if (match(fileName.toLowerCase(), fragmentSet, typeSet)) return true;
			}
		}
		return false;
	}


	/**
	 * Cleans up the given local path, or any of it's child paths, from this descriptor.
	 * If this leaves the descriptor without any local path information, the given
	 * local service address is removed from this descriptor as well.
	 * @param localPath the local path
	 * @param localServiceAddress the local service address
	 */
	public void cleanup(final Path localPath, final SocketAddress localServiceAddress) {
		synchronized (this.localPaths) {
			for (final Iterator<Path> iterator = this.localPaths.iterator(); iterator.hasNext(); ) {
				if (iterator.next().startsWith(localPath)) iterator.remove();
			}
		}

		if (this.localPaths.isEmpty()) {
			this.sourceAddresses.remove(localServiceAddress);
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringWriter charSink = new StringWriter();
		charSink.write("{hash=");
		charSink.write(FileShareResources.toHexString(this.contentHash));
		charSink.write(", length=");
		charSink.write(Long.toString(this.contentLength));
		charSink.write(", local=");
		charSink.write(Boolean.toString(this.isLocal()));
		charSink.write(", remote=");
		charSink.write(Boolean.toString(this.isRemote()));
		charSink.write(", names=");
		charSink.write(this.getFileNames().toString());
		charSink.write("}");
		return charSink.toString();
	}


	/**
	 * Returns <tt>true> if the given file name contains all of the given file
	 * name fragments, and any of the given file types (if present). Note that
	 * the given file name, the fragments, and the types must either be all
	 * upper case or all lower case, otherwise there will be no match.
	 * @param fileName the file name
	 * @param fragments the file name fragments
	 * @param types the file types
	 * @return whether or not the given file name matches the criteria
	 * @throws NullPointerException if any of the given arguments is <tt>null</tt>
	 */
	private static boolean match (final String fileName, final Set<String> fragments, final Set<String> types) {
		final int dotOffset = fileName.lastIndexOf('.');
		final String name = dotOffset == -1 ? fileName : fileName.substring(0, dotOffset);
		final String type = dotOffset == -1 ? null : fileName.substring(dotOffset + 1);

		for (final String fragment : fragments) {
			if (!name.contains(fragment)) return false;
		}

		return types.isEmpty() || types.contains(type);
	}
}