package de.htw.ds.share;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import javax.xml.ws.WebServiceException;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;


/**
 * <p>Local service interface for JAX-WS based P2P file-sharing access.
 * While some operations within interface are intended for communications
 * within the same operating system instance, others are intended to
 * cross operating system instance boundaries.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public interface FileShareService {

	// local OS operations //

	/**
	 * Returns the receivers local service address in canonical representation.
	 * outside their local network.
	 * @return the local service address
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	SocketAddress getLocalServiceAddress();


	/**
	 * Performs a depth search query for source file descriptors matching the
	 * given search criteria over all known peers. Matching is performed using
	 * the given optional name fragments, file types, and minimum and maximum
	 * file lengths. Any of these parameters may be null which represents
	 * omitted query criteria. Note that the algorithm ignores any name
	 * fragments or file types that are <tt>null</tt>. This implies that passing
	 * <tt>null</tt> for all criteria returns all file descriptors known to the
	 * receiver's peers.
	 * @param fragments the file name fragments or null
	 * @param types the allowed file types or null
	 * @param minSize the minimum file size
	 * @param maxSize the maximum file size
	 * @param timeToLive the maximum amount of time spent querying random peers,
	 *    in milliseconds
	 * @return the file descriptors matching the given criteria
	 * @throws IllegalArgumentException if the given minimum length exceeds the
	 *    given maximum length, or of the given timeout is negative
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	FileDescriptor[] queryFileDescriptors(String[] fragments, String[] types, Long minSize, Long maxSize, final long timeToLive);


	/**
	 * Initiates downloading the file identified by the given file-hash into the
	 * given target file location. Note that there must already be a file descriptor
	 * for the given content hash, usually resulting from a previous criteria
	 * query. This is mandatory because the file descriptor contains the file length,
	 * which is required to handle the file properly. Returns <tt>true</tt> if the
	 * download was accepted, false if not. Typical reasons for rejection are that
	 * the target file has already been fully downloaded, or that there is already
	 * an active download for the given file hash.
	 * @param contentHash the content-hash identifying a remote file
	 * @param filePath the path of the local file to be created
	 * @returns whether or not the download was accepted
	 * @throws WebServiceException if there's a JAX-WS related problem
	 * @throws NoSuchFileException if there is no known file descriptor for the given
	 *    file-hash, or if the given file path's parent doesn't exist
	 * @throws AccessDeniedException if a file or directory access check fails
	 * @throws FileSystemException if the file path points to a directory
	 * @throws IOException if there is an I/O related problem
	 */
	boolean startDownload(BigInteger contentHash, Path filePath) throws NoSuchFileException, AccessDeniedException, FileSystemException, IOException;


	/**
	 * Aborts downloading the file identified by the given file-hash.
	 * @param contentHash the file-hash identifying the target file
	 * @returns true if the download was aborted, or false if there is no matching download
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	boolean stopDownload(BigInteger contentHash);


	/**
	 * Returns the download descriptors for all active downloads tasks. Any closed
	 * download task is represented within the result; however such a task is then
	 * immediately removed from this service, which means it won't be occurring
	 * when this method is called again.
	 * @return the download descriptors
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	DownloadDescriptor[] getDownloadDescriptors();


	// remote OS operations //

	/**
	 * Adds the given sender address to this service's peer address
	 * collection if it is neither <tt>null</tt>, nor the service's
	 * local service address, nor unresolvable. Returns parts or all
	 * of this service's peer addresses.
	 * @param the given peer address, or <tt>null</tt>
	 * @return part or all of this service's peer addresses
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	SocketAddress[] exchangePeerAddresses(SocketAddress senderAddress);


	/**
	 * Registers the given file descriptor, or merges it with an existing
	 * one. The resulting file descriptor is returned, which allows the
	 * sender to update it's own descriptor information as well.
	 * @param fileDescriptor the file descriptor
	 * @return the modified file descriptor
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	FileDescriptor addFileDescriptor(FileDescriptor fileDescriptor);


	/**
	 * Returns the file descriptors that match the given query criteria. Matching
	 * is performed using the given optional name fragments, file types, and minimum
	 * and maximum file lengths. Any of these criteria may be <tt>null</tt> which
	 * represents omitted criteria. Note that the algorithm ignores any name fragments
	 * or file types that are <tt>null</tt>. This implies that passing <tt>null</tt>
	 * for all criteria will automatically all file descriptors known to this service!
	 * @param fragments the file name fragments or <tt>null</tt>
	 * @param types the allowed file types or <tt>null</tt>
	 * @param minSize the minimum file size
	 * @param maxSize the maximum file size
	 * @return the file descriptors matching the given criteria
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	FileDescriptor[] getFileDescriptors(String[] fragments, String[] types, Long minSize, Long maxSize);


	/**
	 * Returns a chunk of the file content of the file identified by the given
	 * content-hash. Throws an exception if no such file is hosted by the local
	 * machine.
	 * @param contentHash the content-hash identifying the file
	 * @param offset the position of the first byte to read inside the file
	 * @param length the number of bytes to be transferred
	 * @return the chunk
	 * @throws NullPointerException if the given content hash ist <tt>null</tt>
	 * @throws IllegalArgumentException if the given offset or length is
	 *    strictly negative
	 * @throws WebServiceException if there's a JAX-WS related problem
	 * @throws NoSuchFileException if a file with the given hash is not hosted
	 *    by this service's machine
	 * @throws IOException if there is another I/O related problem
	 */
	byte[] getFileContent(BigInteger contentHash, long offset, int length) throws NoSuchFileException, IOException;
}