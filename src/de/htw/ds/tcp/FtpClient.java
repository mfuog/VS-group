package de.htw.ds.tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.BinaryTransporter;
import de.sb.javase.io.SocketAddress;


/**
 * <p>This class implements a simple FTP client. It demonstrates the use of
 * TCP connections, and the Java Logging API.</p>
 */
@TypeMetadata(copyright="2011-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class FtpClient implements AutoCloseable {
	// workaround for Java7 bug initializing global logger without parent!
	static { LogManager.getLogManager(); }

	private static final int MAX_PACKET_SIZE = 0xFFFF;
	private static enum Mode { STORE, RETRIEVE };

	private final Socket controlConnection;
	private final BufferedReader controlConnectionSource;
	private final BufferedWriter controlConnectionSink;


	/**
	 * Public constructor. Note that forcing the use if the loopback address may be necessary for certain
	 * local FTP servers that don't recognize the local address.
	 * @param hostSocketAddress the IP socket-address of the TFP server
	 * @param alias the user-ID
	 * @param password the password
	 * @param binaryMode true for binary transmission, false for ASCII
	 * @throws IOException if there is an I/O related problem
	 */
	public FtpClient(final InetSocketAddress hostSocketAddress, final String alias, final String password, final boolean binaryMode) throws IOException {
		super();

		this.controlConnection = new Socket(hostSocketAddress.getHostName(), hostSocketAddress.getPort());
		this.controlConnectionSource = new BufferedReader(new InputStreamReader(this.controlConnection.getInputStream(), Charset.forName("US-ASCII")));
		this.controlConnectionSink = new BufferedWriter(new OutputStreamWriter(this.controlConnection.getOutputStream(), Charset.forName("US-ASCII")));
		this.initialize(alias, password, binaryMode);
	}


	/**
	 * Sends an FTP request and parses a single FTP response over the control connection. Note
	 * that some kinds of FTP requests will cause more than one FTP response, which need to be
	 * received separately.
	 * @param ftpRequest the FTP request
	 * @return an FTP response
	 * @throws IOException if there is an I/O related problem
	 */
	private FtpResponse processFtpRequest(final String ftpRequest) throws IOException {
		Logger.getGlobal().log(Level.INFO, ftpRequest.startsWith("PASS") ? "PASS xxxxxxxx" : ftpRequest);
		this.controlConnectionSink.write(ftpRequest);
		this.controlConnectionSink.newLine();
		this.controlConnectionSink.flush();
		final FtpResponse ftpResponse = FtpResponse.parse(this.controlConnectionSource);
		Logger.getGlobal().log(Level.INFO, ftpResponse.toString());
		return ftpResponse;
	}


	/**
	 * Initializes the FTP control connection.
	 * @param alias the user-ID
	 * @param binaryMode true for binary transmission, false for ASCII
	 * @throws SecurityException if the given alias or password is invalid
	 * @throws IOException if there is an I/O related problem
	 */
	private synchronized void initialize(final String alias, final String password, final boolean binaryMode) throws IOException {
		FtpResponse ftpResponse = FtpResponse.parse(this.controlConnectionSource);
		Logger.getGlobal().log(Level.INFO, ftpResponse.toString());
		if (ftpResponse.getCode() != 220) throw new ProtocolException(ftpResponse.toString());

		ftpResponse = this.processFtpRequest("USER " + (alias == null ? "guest" : alias));
		if (ftpResponse.getCode() == 331) {
			ftpResponse = this.processFtpRequest("PASS " + (password == null ? "" : password));
		}
		if (ftpResponse.getCode() != 230) throw new SecurityException(ftpResponse.toString());

		ftpResponse = this.processFtpRequest("TYPE " + (binaryMode ? "I" : "A"));
		if (ftpResponse.getCode() != 200) throw new ProtocolException(ftpResponse.toString());
	}


	/**
	 * Closes the FTP control connection and the data socket.
	 * @throws IOException if there is an I/O related problem
	 */
	public synchronized void close() throws IOException {
		try {
			if (!this.controlConnection.isClosed()) {
				final FtpResponse ftpResponse = this.processFtpRequest("QUIT");
				if (ftpResponse.getCode() != 221) throw new ProtocolException(ftpResponse.toString());
			}
		} finally {
			try { this.controlConnection.close(); } catch (final Exception exception) {};
		}
	}


	/**
	 * Stores the given file on the FTP client side. Note that the source file resides on the
	 * server side and must therefore be a relative path (relative to the FTP server context
	 * directory), while the target directory resides on the client side and can be a global
	 * path.
	 * @param sourceFile the source file (server side)
	 * @param sinkDirectory the sink directory (client side)
	 * @throws NullPointerException if the target directory is null
	 * @throws NotDirectoryException if the source or target directory does not exist
	 * @throws NoSuchFileException if the source file does not exist
	 * @throws AccessDeniedException if the source file cannot be read, or
	 *    the sink directory cannot be written
	 * @throws IOException if there is an I/O related problem
	 */
	public synchronized void retrieve(final Path sourceFile, final Path sinkDirectory) throws IOException {
		if (!Files.isDirectory(sinkDirectory)) throw new NotDirectoryException(sinkDirectory.toString());

		FtpResponse ftpResponse;
		try (OutputStream fileSink = Files.newOutputStream(sinkDirectory.resolve(sourceFile.getFileName()))) {
			if (sourceFile.getParent() != null) {
				ftpResponse = this.processFtpRequest("CWD " + sourceFile.getParent().toString().replace('\\', '/'));
				if (ftpResponse.getCode() != 250) throw new NotDirectoryException(sourceFile.toString());
			}

			ftpResponse = this.processFtpRequest("PASV");
			if (ftpResponse.getCode() != 227) throw new ProtocolException(ftpResponse.toString());
			final InetSocketAddress socketAddress;
			try {
				socketAddress = parseSocketAddress(ftpResponse.getMessage());
			} catch (final IllegalArgumentException exception) {
				throw new ProtocolException(exception.getMessage());
			}

			try (Socket dataConnection = new Socket(socketAddress.getAddress(), socketAddress.getPort())) {
				ftpResponse = this.processFtpRequest("RETR " + sourceFile.getFileName());
				if (ftpResponse.getCode() == 550) throw new NoSuchFileException(ftpResponse.toString());
				if (ftpResponse.getCode() != 150) throw new ProtocolException(ftpResponse.toString());
				new BinaryTransporter(false, MAX_PACKET_SIZE, dataConnection.getInputStream(), fileSink).call();
			}
		}

		ftpResponse = FtpResponse.parse(this.controlConnectionSource);
		Logger.getGlobal().log(Level.INFO, ftpResponse.toString());
		if (ftpResponse.getCode() != 226) throw new ProtocolException(ftpResponse.toString());
	}


	/**
	 * Stores the given file on the FTP server side. Note that the source file resides on the client
	 * side and can therefore be a global path, while the target directory resides on the server side
	 * and must be a relative path (relative to the FTP server context directory), or null.
	 * @param sourceFile the source file (client side)
	 * @param sinkDirectory the sink directory (server side), may be empty
	 * @throws NullPointerException if the source file is null
	 * @throws NotDirectoryException if the sink directory does not exist
	 * @throws AccessDeniedException if the source file cannot be read, or
	 *    the sink directory cannot be written
 	 * @throws IOException if there is an I/O related problem
	 */
	public synchronized void store(final Path sourceFile, final Path sinkDirectory) throws IOException {
		if (!Files.isReadable(sourceFile)) throw new NoSuchFileException(sourceFile.toString());

		FtpResponse ftpResponse;
		try (InputStream fileSource = Files.newInputStream(sourceFile)) {
			if (!sinkDirectory.toString().isEmpty()) {
				ftpResponse = this.processFtpRequest("CWD " + sinkDirectory.toString().replace('\\', '/'));
				if (ftpResponse.getCode() != 250) throw new NotDirectoryException(sinkDirectory.toString());
			}

			ftpResponse = this.processFtpRequest("PASV");
			if (ftpResponse.getCode() != 227) throw new ProtocolException(ftpResponse.toString());
			final InetSocketAddress socketAddress;
			try {
				socketAddress = parseSocketAddress(ftpResponse.getMessage());
			} catch (final IllegalArgumentException exception) {
				throw new ProtocolException(exception.getMessage());
			}

			try (Socket dataConnection = new Socket(socketAddress.getAddress(), socketAddress.getPort())) {
				ftpResponse = this.processFtpRequest("STOR " + sourceFile.getFileName());
				if (ftpResponse.getCode() == 550) throw new AccessDeniedException(ftpResponse.toString());
				if (ftpResponse.getCode() != 150) throw new ProtocolException(ftpResponse.toString());
				new BinaryTransporter(false, MAX_PACKET_SIZE, fileSource, dataConnection.getOutputStream()).call();
			}
		}

		ftpResponse = FtpResponse.parse(this.controlConnectionSource);
		Logger.getGlobal().log(Level.INFO, ftpResponse.toString());
		if (ftpResponse.getCode() != 226) throw new ProtocolException(ftpResponse.toString());
	}


	/**
	 * Parses a socket-address from a code 227 response to a PASV command.
	 * @param pasvResponseMessage the PASV response message
	 * @return a socket-address
	 * @throws NullPointerException if the given PASV message is null
	 * @throws IllegalArgumentException if the PASV response message is not properly formatted
	 */
	private static InetSocketAddress parseSocketAddress(final String pasvResponseMessage) {
		final int beginIndex = pasvResponseMessage.lastIndexOf('(');
		final int endIndex = pasvResponseMessage.lastIndexOf(')');
		if (beginIndex == -1 || endIndex == -1 || beginIndex > endIndex) throw new IllegalArgumentException(pasvResponseMessage);

		final String[] binarySocketAddressElements = pasvResponseMessage.substring(beginIndex + 1, endIndex).split(",");
		if (binarySocketAddressElements.length != 6) throw new IllegalArgumentException(pasvResponseMessage);
		final byte binarySocketAddress[] = new byte[binarySocketAddressElements.length];

		for (int index = 0; index < binarySocketAddress.length; ++index) {
			binarySocketAddress[index] = (byte) Short.parseShort(binarySocketAddressElements[index]);
		}

		final byte[] binaryAddress = { binarySocketAddress[0], binarySocketAddress[1], binarySocketAddress[2], binarySocketAddress[3] };
		final int port = ((binarySocketAddress[4] & 0xFF) << 8) | ((binarySocketAddress[5] & 0xFF) << 0);
		try {
			final InetAddress address = InetAddress.getByAddress(binaryAddress);
			return new InetSocketAddress(address, port);
		} catch (final UnknownHostException exception) {
			throw new AssertionError(); // cannot happen because the binary address array is guaranteed to have the correct length 
		}
	}


	/**
	 * Application entry point. The given runtime parameters must be a host address,
	 * an alias, a password, a boolean indicating binary or ASCII transfer mode,
	 * STORE or RETRIEVE transfer direction, a source file path, and a target file path.
	 * @param args the given runtime arguments
	 * @throws IOException if the given port is already in use
	 */
	public static void main(final String[] args) throws IOException {
		final InetSocketAddress hostAddress = new SocketAddress(args[0]).toInetSocketAddress();
		final String alias = args[1];
		final String password = args[2];
		final boolean binaryMode = Boolean.parseBoolean(args[3]);
		final Mode mode = Mode.valueOf(args[4]);
		final Path sourcePath = Paths.get(args[5]).normalize();
		final Path targetPath = Paths.get(args[6]).normalize();

		try (FtpClient client = new FtpClient(hostAddress, alias, password, binaryMode)) {
			switch (mode) {
				case RETRIEVE:
					client.retrieve(sourcePath, targetPath);
					break;
				case STORE:
					client.store(sourcePath, targetPath);
					break;
				default:
					throw new AssertionError();
			}
		}
	}
}