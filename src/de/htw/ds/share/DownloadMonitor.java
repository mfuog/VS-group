package de.htw.ds.share;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import de.sb.javase.Classes;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;
import de.sb.javase.xml.Namespaces;


/**
 * <p>File descriptor class describing download tasks.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class DownloadMonitor implements Runnable, Channel {
	private static enum ChunkStatus { ACQUIRABLE, ACQUIRED, COMMITTED }
	private static final int CONTENT_HASH_LENGTH = SocketAddress.BINARY_IP6_SOCKET_ADDRESS_LENGTH;
	private static final int LONG_LENGTH = Long.SIZE >> 3;
	private static final int CHUNK_LENGTH = 0x400;
	private static final String LOG_TASK_OK = "Download task {0} succeeded, file \"{1}\" available.";
	private static final String LOG_TASK_FAIL = "Download task {0} failed, reason: \"{1}\"";
	private static final String LOG_TASK_TRANSPORT_BEGIN = "Download task {0}: Beginning transport from peer {1}.";
	private static final String LOG_TASK_TRANSPORT_ABORT = "Download task {0}: Aborted transport from peer {1}, reason: {2}";
	private static final String LOG_TASK_TRANSPORT_END = "Download task {0}: Transport from peer {1} ended naturally.";

	private final DownloadDescriptor descriptor;
	private final FileChannel fileChannel;
	private final MappedByteBuffer chunkStatusMap;
	private final Set<SocketAddress> sources;
	private final Set<SocketAddress> acquiredSources;


	/**
	 * Returns an instance for a new file. Creates the file with the given
	 * length, and appends a download header with the structure
	 * <tt>{chunkStatus} contentHash contentLength</tt> to it's end.
	 * @param filePath the path of the file to be created
	 * @param contentHash the content hash
	 * @param contentLength the content length
	 * @param sources the (concurrently managed) set of file sources
	 * @return the download task created
	 * @throws NullPointerException if any of the given arguments is <tt>null</tt>
	 * @throws IllegalArgumentException if the given content length is negative or
	 * 	 exceeds 2 Petabyte, or if the given content hash exceeds 144bit length
	 * @throws FileAlreadyExistsException if the given file already exists
	 * @throws IOException if there is an I/O related problem accessing the file path
	 */
	public static DownloadMonitor create(final Path filePath, final BigInteger contentHash, final long contentLength, final Set<SocketAddress> sources) throws IOException {
		final byte[] contentHashBytes = contentHash.toByteArray();
		if (contentHashBytes.length > CONTENT_HASH_LENGTH) throw new IllegalArgumentException();

		final FileChannel fileChannel = (FileChannel) Files.newByteChannel(filePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE);
		try {
			fileChannel.position(contentLength);
			
			// write download header: {chunkStatus} contentHash contentLength
			final ByteBuffer buffer = ByteBuffer.allocate(CHUNK_LENGTH);
			for (int bytesToWrite = chunkCount(contentLength); bytesToWrite > 0; bytesToWrite -= CHUNK_LENGTH) {
				buffer.position(bytesToWrite > CHUNK_LENGTH ? 0 : CHUNK_LENGTH - bytesToWrite);
				fileChannel.write(buffer);
			}

			buffer.limit(CONTENT_HASH_LENGTH + LONG_LENGTH);
			buffer.position(CONTENT_HASH_LENGTH - contentHashBytes.length);
			buffer.put(contentHashBytes);
			buffer.putLong(contentLength);
			buffer.rewind();
			fileChannel.write(buffer);
			fileChannel.force(true);

			return new DownloadMonitor(contentHash, contentLength, filePath, fileChannel, sources);
		} catch (final Throwable exception) {
			try { fileChannel.close(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
			throw exception;
		}
	}


	/**
	 * Returns an instance for an existing file. Note that the descriptor's persistent
	 * chunk status map is updated so that any previously ACQUIRED chunks become
	 * ACQUIRABLE again.
	 * @param filePath the path of an existing file to be opened
	 * @param sources the (concurrently managed) set of file sources
	 * @return the download task created
	 * @throws NullPointerException if any of the given arguments is <tt>null</tt>
	 * @throws IllegalArgumentException if the given file's size exceeds 2 Petabyte,
	 *    or if it does not contain a valid download header, or if the given content
	 *    hash exceeds 144bit length
	 * @throws SecurityException if this process doesn't have permission to access
	 *    the given file path in READ and WRITE mode
	 * @throws StreamCorruptedException if the given file does not conform to the
	 *    internal layout for actively downloading files
	 * @throws IOException if there is an I/O related problem accessing the file
	 */
	public static DownloadMonitor create(final Path filePath, final Set<SocketAddress> sources) throws IOException {
		final byte[] contentHashBytes = new byte[CONTENT_HASH_LENGTH];
		final FileChannel fileChannel = (FileChannel) Files.newByteChannel(filePath, StandardOpenOption.READ, StandardOpenOption.WRITE);
		try {
			final ByteBuffer buffer = ByteBuffer.allocate(CONTENT_HASH_LENGTH + LONG_LENGTH);
			fileChannel.position(fileChannel.size() - CONTENT_HASH_LENGTH - LONG_LENGTH);
			fileChannel.read(buffer);
			buffer.position(0);

			buffer.get(contentHashBytes);
			final BigInteger contentHash = new BigInteger(1, contentHashBytes);
			final long contentLength = buffer.getLong();

			final int chunkCount = chunkCount(contentLength);
			final long headerLength = chunkCount + CONTENT_HASH_LENGTH + LONG_LENGTH;
			if (fileChannel.size() != contentLength + headerLength) throw new IllegalArgumentException();

			return new DownloadMonitor(contentHash, contentLength, filePath, fileChannel, sources);
		} catch (final Throwable exception) {
			try { fileChannel.close(); } catch (final Exception nestedException) { exception.addSuppressed(nestedException); }
			throw exception;
		}
	}


	/**
	 * Return the number of chunks for the given content length.
	 * @param contentLength the content length
	 * @return the chunk count
	 * @throws IllegalArgumentException if the given content length is negative or
	 * 	 exceeds 2 Petabyte
	 */
	private static int chunkCount(final long contentLength) {
		final long chunkCount = 1 + (contentLength - 1) / CHUNK_LENGTH;
		if (chunkCount > Integer.MAX_VALUE) throw new IllegalArgumentException();
		return (int) chunkCount;
	}


	/**
	 * Creates a new instance and resets any AQUIRED chunks to ACQUIRABLE.
	 * @param contentHash the content hash
	 * @param contentLength the content length
	 * @param filePath the path of the file to be written
	 * @param fileChannel the channel of the file to be written
	 * @param sources the (concurrently managed) set of file sources
	 * @throws IOException if there is an I/O related problem mapping the file's
	 *    chunk status map into memory
	 * @throws NullPointerException if any of the given arguments is <tt>null</tt>
	 * @throws IllegalArgumentException if the given hash or length is strictly negative
	 * @throws IllegalStateException if the chunk status map within the given file contains
	 *    an illegal status
	 */
	protected DownloadMonitor(final BigInteger contentHash, final long contentLength, final Path filePath, final FileChannel fileChannel, final Set<SocketAddress> sources) throws IOException {
		super();
		if (filePath == null | fileChannel == null | sources == null) throw new NullPointerException();
		if (contentHash.compareTo(BigInteger.ZERO) == -1 | contentLength < 0) throw new IllegalArgumentException();

		this.descriptor = new DownloadDescriptor(contentHash, contentLength, filePath);
		this.fileChannel = fileChannel;
		this.chunkStatusMap = this.fileChannel.map(MapMode.READ_WRITE, this.descriptor.getContentLength(), chunkCount(this.descriptor.getContentLength()));
		this.sources = sources;
		this.acquiredSources = Collections.synchronizedSet(new HashSet<SocketAddress>());

		for (int chunkCount = this.getChunkCount(), chunkIndex = 0; chunkIndex < chunkCount; ++chunkIndex) {
			switch (this.getChunkStatus(chunkIndex)) {
				case ACQUIRED: this.chunkStatusMap.put(chunkIndex, (byte) ChunkStatus.ACQUIRABLE.ordinal());
				case ACQUIRABLE: this.descriptor.setAcquirableChunkCount(this.descriptor.getAcquirableChunkCount() + 1); break;
				case COMMITTED: this.descriptor.setCommittedChunkCount(this.descriptor.getCommittedChunkCount() + 1); break;
			}
		}
	}


	/**
	 * Truncates this descriptor's file to it's content length, and closes it.
	 * @throws IOException if there is an I/O related problem 
	 */
	public void close() throws IOException {
		this.descriptor.setClosed(true);

		synchronized(this.fileChannel) {
			if (this.fileChannel.isOpen()) {
				try {
					this.fileChannel.truncate(this.descriptor.getContentLength());
				} finally {
					try { this.fileChannel.force(true); } catch (final Exception exception) {}
					try { this.fileChannel.close(); } catch (final Exception exception) {}
				}
			}
		}
	}


	/**
	 * Returns <tt>true</tt> if this descriptor is open, <tt>false</tt> otherwise.
	 * @return whether or not this descriptor is open
	 */
	public boolean isOpen() {
		return !this.descriptor.isClosed();
	}


	/**
	 * Returns the task descriptor.
	 * @return the descriptor
	 */
	public DownloadDescriptor getDescriptor() {
		return this.descriptor;
	}


	/**
	 * Returns the number of chunks, i.e. the chunk map's capacity.
	 * @return the number of chunks
	 */
	public int getChunkCount() {
		return this.chunkStatusMap.capacity();
	}


	/**
	 * Returns the status of the given chunk.
	 * @param chunkIndex the chunk index
	 * @return the chunk status
	 * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds
	 * @throws IllegalStateException if an illegal chunk status is encountered
	 */
	protected ChunkStatus getChunkStatus(final int chunkIndex) {
		final byte statusCode = this.chunkStatusMap.get(chunkIndex);
		try {
			return ChunkStatus.values()[statusCode];
		} catch (final ArrayIndexOutOfBoundsException exception) {
			throw new IllegalStateException();
		}
	}


	/**
	 * Checks if the current chunk status is the expected status, and if so returns
	 * <tt>true</tt> after atomically changing it to the new status; otherwise returns
	 * <tt>false</tt> because the chunk status remains at it's unexpected value. Note
	 * that this provides concurrent download threads a thread-safe way to alter a
	 * chunk's status, as the file channel is solely used to update chunk content.
	 * @param chunkIndex the chunk index
	 * @param expectedStatus the expected chunk status
	 * @param newStatus the new chunk status
	 * @return whether or not the change was successful
	 * @throws IllegalStateException if an illegal chunk status is encountered
	 */
	protected boolean putChunkStatus(final int chunkIndex, final ChunkStatus expectedStatus, final ChunkStatus newStatus) {
		synchronized(this.chunkStatusMap) {
			if (this.getChunkStatus(chunkIndex) != expectedStatus) return false;

			if (expectedStatus != newStatus) {
				this.chunkStatusMap.put(chunkIndex, (byte) newStatus.ordinal());
				switch (expectedStatus) {
					case ACQUIRABLE: this.descriptor.setAcquirableChunkCount(this.descriptor.getAcquirableChunkCount() - 1); break;
					case ACQUIRED:   this.descriptor.setAcquiredChunkCount(this.descriptor.getAcquiredChunkCount() - 1); break;
					case COMMITTED:  this.descriptor.setCommittedChunkCount(this.descriptor.getCommittedChunkCount() - 1); break;
				}
				switch (newStatus) {
					case ACQUIRABLE: this.descriptor.setAcquirableChunkCount(this.descriptor.getAcquirableChunkCount() + 1); break;
					case ACQUIRED:   this.descriptor.setAcquiredChunkCount(this.descriptor.getAcquiredChunkCount() + 1); break;
					case COMMITTED:  this.descriptor.setCommittedChunkCount(this.descriptor.getCommittedChunkCount() + 1); break;
				}
			}
			return true;
		}
	}


	/**
	 * Returns the length of the given chunk.
	 * @param chunkIndex the chunk index
	 * @return the chunk length
	 * @throws IndexOutOfBoundsException if the given index is out of bounds
	 */
	protected int getChunkLength(final int chunkIndex) {
		if (chunkIndex < 0 | chunkIndex >= this.getChunkCount()) throw new IndexOutOfBoundsException();

		return (int) Math.min(CHUNK_LENGTH, this.descriptor.getContentLength() - chunkIndex * CHUNK_LENGTH);
	}


	/**
	 * Reads the chunk with the given index from this descriptor's file channel.
	 * Note that this operation allows for concurrency.
	 * @param chunkIndex the chunk index
	 * @return the chunk
	 * @throws IndexOutOfBoundsException if the given index is out of bounds
	 * @throws IOException if there is an I/O related problem
	 */
	protected byte[] getChunk(final int chunkIndex) throws IOException {
		final byte[] chunk = new byte[this.getChunkLength(chunkIndex)];
		this.fileChannel.read(ByteBuffer.wrap(chunk), chunkIndex * CHUNK_LENGTH);
		return chunk;
	}


	/**
	 * Writes the given chunk into it's proper position within this descriptor's
	 * file channel. Note that this operation allows for concurrency.
	 * @param chunkIndex the chunk index
	 * @param chunk the chunk
	 * @throws IndexOutOfBoundsException if the given index is out of bounds
	 * @throws IOException if there is an I/O related problem
	 */
	protected void putChunk(final int chunkIndex, final byte[] chunk) throws IOException {
		if (chunk.length != this.getChunkLength(chunkIndex)) throw new IllegalArgumentException();

		this.fileChannel.write(ByteBuffer.wrap(chunk), chunkIndex * CHUNK_LENGTH);
	}


	private void transportChunks(final SocketAddress source, final FileShareService proxy) {
		Logger.getGlobal().log(Level.FINE, LOG_TASK_TRANSPORT_BEGIN, new Object[] {this, source});

		try {
			for (int chunkCount = this.getChunkCount(), chunkIndex = 0; chunkIndex < chunkCount & DownloadMonitor.this.isOpen(); ++chunkIndex) {
				// if (this.getChunkStatus(chunkIndex) == ChunkStatus.ACQUIRABLE) {
					if (this.putChunkStatus(chunkIndex, ChunkStatus.ACQUIRABLE, ChunkStatus.ACQUIRED)) {
						try {
							final byte[] chunk = proxy.getFileContent(DownloadMonitor.this.descriptor.getContentHash(), chunkIndex * CHUNK_LENGTH, DownloadMonitor.this.getChunkLength(chunkIndex));
							this.putChunk(chunkIndex, chunk);
							this.putChunkStatus(chunkIndex, ChunkStatus.ACQUIRED, ChunkStatus.COMMITTED);
						} catch (final Throwable exception) {
							this.putChunkStatus(chunkIndex, ChunkStatus.ACQUIRED, ChunkStatus.ACQUIRABLE);
							throw exception;
						}
					}
				// }
			}
			Logger.getGlobal().log(Level.FINE, LOG_TASK_TRANSPORT_END, new Object[] {this, source, "all chunks visited."});
		} catch (final IllegalStateException | ArrayIndexOutOfBoundsException exception) {
			try { DownloadMonitor.this.close(); } catch (final Exception nestedException) {}
			try { Files.delete(DownloadMonitor.this.descriptor.getFilePath()); } catch (final Exception nestedException) {}
			Logger.getGlobal().log(Level.WARNING, LOG_TASK_FAIL, new Object[] {this, "chunk table corrupted."});
		} catch (final NoSuchFileException exception) {
			this.sources.remove(source);
			Logger.getGlobal().log(Level.FINE, LOG_TASK_TRANSPORT_END, new Object[] {this, source, exception.getMessage()});
		} catch (final Throwable exception) {
			Logger.getGlobal().log(Level.WARNING, LOG_TASK_TRANSPORT_ABORT, new Object[] {this, source, exception.getMessage()});
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return this.getClass().getName() + this.descriptor.toString();
	}


	/**
	 * Periodically spawns a new download transporter whenever a file source
	 * becomes available. Assembles the task's target file once a download task
	 * is finished, and cleans up temporary chunks
	 */
	public void run() {
		if (this.descriptor.isClosed() | this.getChunkCount() == this.descriptor.getCommittedChunkCount()) {
			try {
				this.close();
				Logger.getGlobal().log(Level.INFO, LOG_TASK_OK, new Object[] {this, this.descriptor.getFilePath()});
			} catch (final Exception exception) {
				Logger.getGlobal().log(Level.INFO, LOG_TASK_FAIL, new Object[] {this, this.descriptor.getFilePath(), "cannot truncate file."});
			}
			throw new ThreadDeath();
		}

		// TODO: Aufgabe 4

		for (final SocketAddress source : DownloadMonitor.this.sources.toArray(new SocketAddress[0])) {
			if (this.acquiredSources.add(source)) {
				try {
					final FileShareService proxy;
					try {
						final URL wsdlLocator = FileShareResources.serviceWsdlLocator(source);
						final Service proxyFactory = Service.create(wsdlLocator, Namespaces.toQualifiedName(FileShareService.class));
						proxy = proxyFactory.getPort(FileShareService.class);
					} catch (final WebServiceException exception) {
						if (!(Classes.rootCause(exception) instanceof ConnectException)) {
							this.sources.remove(source); // incompatible source
						}
						this.acquiredSources.remove(source);
						continue;
					}

					final Runnable transporter = new Runnable() {
						public void run() {
							try {
								DownloadMonitor.this.transportChunks(source, proxy);
							} finally {
								DownloadMonitor.this.acquiredSources.remove(source);
							}
						}
					};

					final Thread thread = new Thread(transporter, "download-transporter-" + FileShareResources.toHexString(this.descriptor.getContentHash()) + "-" + source);
					thread.setDaemon(true);
					thread.start();
				} catch (final Throwable exception) {
					this.acquiredSources.remove(source);
					throw exception;
				}
			}
		}
	}
}