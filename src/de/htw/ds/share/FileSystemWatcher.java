package de.htw.ds.share;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.PathCollector;
import de.sb.javase.io.SocketAddress;


/**
 * <p>Watches a set of upload directories for changes. Addition, update and removal
 * of files and directories are handled by modifying the watcher's file descriptors
 * accordingly. Note that two file descriptors will be merged if a file is changed,
 * and it's new content equals another managed file's content. Likewise, file
 * descriptors are split if they represent multiple files sharing the same content,
 * and the content of one of them changes.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class FileSystemWatcher implements AutoCloseable {
	private static final MessageDigest SHA_MESSAGE_DIGEST;
	private static final String LOG_FILE_DESCRIPTOR_COUNT = "Current file descriptor count: {0}.";
	static {
		try {
			SHA_MESSAGE_DIGEST = MessageDigest.getInstance("SHA");
		} catch (final NoSuchAlgorithmException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}

	private final SocketAddress localServiceAddress;
	private final WatchService watchService;
	private final Map<Path,WatchKey> directories;
	private final Map<BigInteger,FileDescriptor> fileDescriptors;
	private volatile FileDescriptor[] fileDescriptorSnapshot;


	/**
	 * Public constructor.
	 * @param localServiceAddress the local service address
	 * @param uploadDirectoryPath the root directory to be watched
	 * @throws IOException if there is an I/O related problem
	 * @throws NullPointerException if any of the given arguments is <tt>null</tt>
	 */
	public FileSystemWatcher(final SocketAddress localServiceAddress, final Path uploadDirectoryPath) throws IOException {
		super();
		if (localServiceAddress == null | uploadDirectoryPath == null) throw new NullPointerException();

		this.localServiceAddress = localServiceAddress;
		this.watchService = FileSystems.getDefault().newWatchService();
		this.directories = new HashMap<Path,WatchKey>();
		this.fileDescriptors = Collections.synchronizedMap(new HashMap<BigInteger,FileDescriptor>());

		this.directoryAdded(uploadDirectoryPath);
		this.updateFileDescriptorSnapshot();
	}


	/**
	 * Closes this watcher.
	 * @throws IOException if there is an I/O related problem
	 */
	public void close() throws IOException {
		this.watchService.close();
	}


	/**
	 * Returns the file descriptors.
	 * @return the file descriptors
	 */
	public Map<BigInteger,FileDescriptor> getFileDescriptors() {
		return this.fileDescriptors;
	}


	/**
	 * Returns the latest snapshot of shuffled file descriptors.
	 * @return the latest file descriptor snapshot
	 */
	public FileDescriptor[] getFileDescriptorSnapshot() {
		return this.fileDescriptorSnapshot;
	}


	/**
	 * Checks for changes within the upload directories, and
	 * updates the file descriptors accordingly.
	 * @throws ClosedWatchServiceException if the underlying watch service is closed
	 */
	public void updateFileDescriptors() throws ClosedWatchServiceException {
		final WatchKey watchKey = this.watchService.poll();
		if (watchKey != null) {
			@SuppressWarnings("unchecked")
			final WatchEvent<Path>[] watchEvents = watchKey.pollEvents().toArray(new WatchEvent[0]);
			final Path contextDirectory = (Path) watchKey.watchable();
			try {
				for (final WatchEvent<Path> watchEvent : watchEvents) {
					final WatchEvent.Kind<?> watchEventType = watchEvent.kind();
					final Path eventPath = contextDirectory.resolve(watchEvent.context());
					try {
						if (watchEventType == StandardWatchEventKinds.ENTRY_DELETE) {
							final WatchKey contextChildKey = this.directories.get(eventPath);
							if (contextChildKey != null) contextChildKey.cancel(); // TODO: needs to be canceled?
							this.pathRemoved(eventPath);
						} else if (watchEventType == StandardWatchEventKinds.ENTRY_MODIFY) {
							if (Files.isRegularFile(eventPath, LinkOption.NOFOLLOW_LINKS)) {
								this.pathRemoved(eventPath);
								this.fileAdded(eventPath);
							}
						} else if (watchEventType == StandardWatchEventKinds.ENTRY_CREATE) {
							if (Files.isRegularFile(eventPath, LinkOption.NOFOLLOW_LINKS)) {
								this.fileAdded(eventPath);
							} else if (Files.isDirectory(eventPath, LinkOption.NOFOLLOW_LINKS)) {
								this.directoryAdded(eventPath);
							}
						}
					} catch (final Exception exception) {
						Logger.getGlobal().log(Level.WARNING, eventPath.toString(), exception);
					}
				}
			} finally {
				if (!watchKey.reset()) this.directories.remove(watchKey.watchable());
			}
		}

		this.updateFileDescriptorSnapshot();
		Logger.getGlobal().log(Level.INFO, LOG_FILE_DESCRIPTOR_COUNT, this.fileDescriptorSnapshot.length);
	}


	/**
	 * Creates a new file descriptor snapshot. Note that without centralized snapshot
	 * creation every server/watcher or monitor method working with file descriptors
	 * would have to constantly synchronize the file descriptor map in order to create
	 * a private snapshot, and then shuffle it to achieve statistical spread. As the
	 * file descriptor map can become quite huge, and those tasks often don't require
	 * the newest available data, this would soon become very wasteful in terms of
	 * memory and processor utilization.
	 */
	private void updateFileDescriptorSnapshot() {
		final FileDescriptor[] snapshot;
		synchronized (this.fileDescriptors) {
			snapshot = this.fileDescriptors.values().toArray(new FileDescriptor[0]);
		}

		Collections.shuffle(Arrays.asList(snapshot));
		this.fileDescriptorSnapshot = snapshot;
	}


	/**
	 * Takes necessary action for a directory that has been added
	 * somewhere within the upload directory tree.
	 * @param directoryPath the directory path
	 * @throws IOException if there is an I/O related problem walking the
	 *    directory, or registering it with the local file system watcher
	 */
	private void directoryAdded(final Path directoryPath) throws IOException {
		final PathCollector collector = new PathCollector(false);
		Files.walkFileTree(directoryPath, collector);

		for (final Path path : collector.getVisitedDirectoryPaths()) {
			final WatchKey watchKey = path.register(this.watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
			this.directories.put((Path) watchKey.watchable(), watchKey);
		}

		for (final Path path : collector.getVisitedFilePaths()) {
			this.fileAdded(path);
		}
	}


	/**
	 * Takes necessary action for a file that has been added
	 * somewhere within the upload directory tree.
	 * @param filePath the file path
	 * @throws IOException if there is an I/O related problem
	 *    determining the file hash
	 */
	private void fileAdded(final Path filePath) throws IOException {
		final BigInteger contentHash = contentHash(filePath, SocketAddress.BINARY_IP6_SOCKET_ADDRESS_LENGTH);

		FileDescriptor fileDescriptor;
		synchronized (this.fileDescriptors) {
			fileDescriptor = this.fileDescriptors.get(contentHash);
			if (fileDescriptor == null) {
				fileDescriptor = new FileDescriptor(contentHash, Files.size(filePath));
				this.fileDescriptors.put(contentHash, fileDescriptor);
			}
		}

		fileDescriptor.getSourceAddresses().add(this.localServiceAddress);
		fileDescriptor.getLocalPaths().add(filePath);
		fileDescriptor.getFileNames().add(filePath.getFileName().toString());
	}


	/**
	 * Takes necessary action for a path (directory or file) that has
	 * been removed somewhere within the upload directory tree.
	 * @param filePath the file path
	 */
	private void pathRemoved(final Path path) {
		synchronized (this.fileDescriptors) {
			for (final Iterator<FileDescriptor> iterator = this.fileDescriptors.values().iterator(); iterator.hasNext(); ) {
				final FileDescriptor descriptor = iterator.next();
				descriptor.cleanup(path, this.localServiceAddress);
				if (!descriptor.isLocal() && !descriptor.isRemote()) {
					iterator.remove();
				}
			}
		}
	}


	/**
	 * Returns an unsigned derivative of a (160 bit) SHA hash code for the
	 * given file's content, shifted to the given byte length.
	 * @param filePath the file path
	 * @param byte length the byte length of the resulting content hash
	 * @return the content hash
	 * @throws NullPointerException if the given file path is <tt>null</tt>
	 * @throws IllegalArgumentException if the given byte length is negative
	 */
	private static BigInteger contentHash(final Path filePath, final int byteLength) throws IOException {
		if (byteLength <= 0) throw new IllegalArgumentException();

		final BigInteger hashCode;
		try (InputStream byteSource = Files.newInputStream(filePath, StandardOpenOption.READ)) {
			final byte[] buffer = new byte[0x10000];

			synchronized (SHA_MESSAGE_DIGEST) {
				SHA_MESSAGE_DIGEST.reset();
				for (int bytesRead = byteSource.read(buffer); bytesRead != -1; bytesRead = byteSource.read(buffer)) {
					SHA_MESSAGE_DIGEST.update(buffer, 0, bytesRead);
				}
				final byte[] hashBytes = SHA_MESSAGE_DIGEST.digest();
				hashBytes[0] &= 0x7F; // mask sign bit!
				hashCode = new BigInteger(1, hashBytes);
			}
		}

		return hashCode.shiftRight(8 * (SHA_MESSAGE_DIGEST.getDigestLength() - byteLength));
	}
}