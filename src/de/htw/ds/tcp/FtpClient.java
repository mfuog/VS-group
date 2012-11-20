package de.htw.ds.tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
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
import de.htw.ds.TypeMetadata;
import de.htw.ds.util.SocketAddress;


/**
 * <p>This class implements a simple FTP client. It demonstrates the use of
 * TCP connections, and the Java Logging API.</p>
 */
@TypeMetadata(copyright="2011-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class FtpClient implements Closeable {
	// workaround for Java7 bug initializing global logger without parent!
	static { LogManager.getLogManager(); }

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
		if (!this.controlConnection.isClosed()) {
			try {
				final FtpResponse ftpResponse = this.processFtpRequest("QUIT");
				if (ftpResponse.getCode() != 221) throw new ProtocolException(ftpResponse.toString());
			} finally {
				try { this.controlConnection.close(); } catch (final Throwable exception) {};
			}
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
 	 * @throws IOException if there is an I/O related problem
	 */
	public synchronized void retrieve(final Path sourceFile, final Path sinkDirectory) throws IOException {
		System.out.println("source: "+sourceFile);
		System.out.println("sink: "+sinkDirectory);
		System.out.println("sourceFile.getParent(): "+sourceFile.getParent());
		
		if (!Files.isDirectory(sinkDirectory)) throw new NotDirectoryException(sinkDirectory.toString());

		// TODO: If the source file parent is not null, issue a CWD message to the FTP server,
		if (sourceFile.getParent() != null){
			// setting it's current working directory to the source file parent.
			this.controlConnectionSink.write("CWD " + sourceFile.getParent() + "\r\n");
			// Send a PASV message to query the socket-address to be used for the data transfer.
			// You can use parseSocketAddress() to parse the socket-address from the response.

			final FtpResponse response1 = this.processFtpRequest("PASV");
			if (response1.getCode() != 227) throw new ProtocolException(response1.toString());
			InetSocketAddress dataSocketAdress = parseSocketAddress(response1.toString());

			// Open a data connection to the socket-address using "new Socket(host, port)".
			Socket dataSocket = new Socket(dataSocketAdress.getAddress(), dataSocketAdress.getPort());
			
			BufferedReader read = new BufferedReader(new InputStreamReader(dataSocket.getInputStream(), Charset.forName("US-ASCII")));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream(), Charset.forName("US-ASCII")));
	
			// Send a RETR message over the control connection. After receiving the first part
			final FtpResponse response2 = this.processFtpRequest("RETR " + sourceFile.getParent().toString());
			if (response2.getCode() != 150) throw new ProtocolException(response2.toString());
			
			
			
		}
		
		
		 
		
		
		// of it's response (code 150), transport the content of the data connection's INPUT
		// stream to the target file, closing it once there is no more data. Then receive the
		// second part of the RETR response (code 226). Make sure the source file and the data
		// connection are closed in any case.
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

		// TODO: If the target directory is not null, issue a CWD (change working directory) message to the FTP server,
		// setting it's current working directory to the target directory. Send a PASV
		// (PASV = This command requests the server "listen" on a data port and to wait for a connection rather than to initiate one 
		//		upon a transfer command thus making the Transfer Mode Passive. The response to this command 
		//		includes the host and port address the server is listening on in most cases these have defaults..)
		
		// message to query the socket-address to be used for the data transfer.
		// You can use parseSocketAddress() to parse the socket-address from the response.
		// Open a data connection to the socket-address using "new Socket(host, port)".
		// Send a STOR message over the control connection. After receiving the first part of
		// it's response (code 150), transport the source file content to the data connection's
		// OUTPUT stream, closing it once there is no more data. Then receive the second part
		// of the STOR response (code 226) on the control connection. Make sure the source file
		// and the data connection are closed in any case.
	}


	/**
	 * Parses a socket-address from a code 227 response to a PASV command.
	 * @param pasvResponseMessage the PASV response message
	 * @return a socket-address
	 * @throws NullPointerException if the given PASV message is null
	 * @throws IllegalArgumentException if the PASV response message is not properly formatted
	 */
	//@SuppressWarnings("unused")	// TODO: Remove this if you use this method
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
		// ariel.f4.htw-berlin.de:21 s0529509 abc false RETRIEVE /home/05/29509/Documents/test.rtf test
		final InetSocketAddress hostAddress = new SocketAddress(args[0]).toInetSocketAddress();
		final String alias = args[1];
		final String password = args[2];
		final boolean binaryMode = Boolean.parseBoolean(args[3]);
		final Mode mode = Mode.valueOf(args[4]);
		final Path sourcePath = Paths.get(args[5]).normalize();
		final Path targetPath = Paths.get(args[6]).normalize();

		try (FtpClient client = new FtpClient(hostAddress, alias, password, binaryMode)) {
			switch (mode) { // mode is an enum predifined in this class
				case RETRIEVE: {
					client.retrieve(sourcePath, targetPath);
					break;
				}
				case STORE: {
					client.store(sourcePath, targetPath);
					break;
				}
			}
		}
	}
}