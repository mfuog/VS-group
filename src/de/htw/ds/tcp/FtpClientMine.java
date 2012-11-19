package de.htw.ds.tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
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
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import de.htw.ds.TypeMetadata;
import de.htw.ds.util.BinaryTransporter;
import de.htw.ds.util.SocketAddress;


/**
 * <p>This class implements a simple FTP client. It demonstrates the use of
 * TCP connections, and the Java Logging API.</p>
 */
@TypeMetadata(copyright="2011-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class FtpClientMine implements Closeable {
	// workaround for Java7 bug initializing global logger without parent!
	static { LogManager.getLogManager(); }

	//Puffergröße festlegen: Je größer der Puffer, um so weniger Festplattenzugriffe sind nötig.
	private static final int MAX_PACKET_SIZE = 0xFFFF;//0xFFFF = 65535 = 64kB. größte Wert("high value"), den man mit 2 Byte darstellen kann. 
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
	public FtpClientMine(final InetSocketAddress hostSocketAddress, final String alias, final String password, final boolean binaryMode) throws IOException {
		super();
		
		//setup connection socket (of host/client)
		this.controlConnection = new Socket(hostSocketAddress.getHostName(), hostSocketAddress.getPort());
		//setup reader & writer with inputStream & OutputStream (of host/client)
		this.controlConnectionSource = new BufferedReader(new InputStreamReader(this.controlConnection.getInputStream(), Charset.forName("US-ASCII")));
		this.controlConnectionSink = new BufferedWriter(new OutputStreamWriter(this.controlConnection.getOutputStream(), Charset.forName("US-ASCII")));
		
		//login on server & set mode
		this.initialize(alias, password, binaryMode);
	}


	/**
	 * Sends an FTP request and parses a single FTP command over the control connection of this.
	 * Returns a new FtpResponse Object, which contains the ftp response code and message
	 * 
	 * Note that some kinds of FTP requests will cause more than one FTP response, which need to be
	 * received separately.
	 * @param ftpRequest the FTP request
	 * @return an FTP response
	 * @throws IOException if there is an I/O related problem
	 */
	private FtpResponse processFtpRequest(final String ftpRequest) throws IOException {
		
		//log the ftp request to the console, but don't show the actual password
		Logger.getGlobal().log(Level.INFO, ftpRequest.startsWith("PASS") ? "PASS xxxxxxxx" : ftpRequest);	
		
		//write the FTP command to the control connection writer = send request!!
		this.controlConnectionSink.write(ftpRequest);
		this.controlConnectionSink.newLine();
		this.controlConnectionSink.flush();
		
		//create ftpResponse with new return code & message
		final FtpResponse ftpResponse = FtpResponse.parse(this.controlConnectionSource);
		
		//log the ftp response to the console
		Logger.getGlobal().log(Level.INFO, ftpResponse.getCode()+" "+ftpResponse.getMessage());	
		
		return ftpResponse;
	}


	/**
	 * Initializes the FTP control connection by processing the following ftp requests:
	 * USER, PASS and TYPE
	 * 
	 * FTP response codes:
	 * vgl. http://www.altools.com/image/support/alftp/alftp_4x/FTP_response_codes_rfc_959_messages.htm
	 * 
	 * @param alias the user-ID
	 * @param binaryMode true for binary transmission, false for ASCII
	 * @throws SecurityException if the given alias or password is invalid
	 * @throws IOException if there is an I/O related problem
	 */
	private synchronized void initialize(final String alias, final String password, final boolean binaryMode) throws IOException {
		System.out.println("initialzing FTP control connection...");
		//setup first ftpResponse-Object. 
		FtpResponse ftpResponse = FtpResponse.parse(this.controlConnectionSource);	//should return 220 
		
		//log the initial ftp response (220) to the console
		Logger.getGlobal().log(Level.INFO, ftpResponse.toString());
		
		//sending ftp requests & checking ftp response codes for logging in & setting mode:
		if (ftpResponse.getCode() != 220) throw new ProtocolException(ftpResponse.toString());	//220 = Service ready for new user.

		ftpResponse = this.processFtpRequest("USER " + (alias == null ? "guest" : alias));
		if (ftpResponse.getCode() == 331) {														//331 = User name okay, need password.
			ftpResponse = this.processFtpRequest("PASS " + (password == null ? "" : password));
		}
		if (ftpResponse.getCode() != 230) throw new SecurityException(ftpResponse.toString());	//230 = User logged in, proceed.

		ftpResponse = this.processFtpRequest("TYPE " + (binaryMode ? "I" : "A"));
		if (ftpResponse.getCode() != 200) throw new ProtocolException(ftpResponse.toString());	// 200 = Command okay.
		//-> ready to proceed!
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
		if (!Files.isDirectory(sinkDirectory)) throw new NotDirectoryException(sinkDirectory.toString());
				
		if(sourceFile.getParent()!=null){	//Warum?
			
			FtpResponse ftpResponse = processFtpRequest("CWD "+sourceFile.getParent());	//issue a CWD message to the FTP server, setting it's current working directory to the source file parent
			ftpResponse = processFtpRequest("PASV");	// Send a PASV message to query the socket-address to be used for the data transfer.
			
			InetSocketAddress serverSocketAddress = this.parseSocketAddress(ftpResponse.toString());				//parse the socket-address from the response.
			Socket dataConnection = new Socket(serverSocketAddress.getAddress(), serverSocketAddress.getPort());	// Open a data connection to the socket-address using "new Socket(host, port)".
			
			ftpResponse = processFtpRequest("RETR "+sourceFile.toString());	// Send a RETR message over the control connection (=get source file)
			
			// After receiving the first part of it's response (code 150),
			//TODO...?
			
			//copy filename: sourceFile.getFileName()
			try ( OutputStream sink = Files.newOutputStream(sinkDirectory.resolve("copiedFile.txt"), StandardOpenOption.CREATE))
			{
				// transport the content of the data connection's INPUT stream to the target file, using BinaryTransporter
				new BinaryTransporter(true, MAX_PACKET_SIZE, dataConnection.getInputStream(), sink).call();	//Transporter.call() transportiert daten von source to sink und schließt Ressource
				Logger.getGlobal().log(Level.INFO, ftpResponse.getCode()+" "+ftpResponse.getMessage());
				// closing it once there is no more data. - macht BinaryTransporter!
			}
			catch ( IOException e ){e.printStackTrace();}
	
			ftpResponse = FtpResponse.parse(this.controlConnectionSource);	//update ftpResponse
			Logger.getGlobal().log(Level.INFO, ftpResponse.getCode()+" "+ftpResponse.getMessage());
			
			Logger.getGlobal().log(Level.INFO, ftpResponse.getCode()+" "+ftpResponse.getMessage());
		}
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
		System.out.println("source: "+sourceFile);
		System.out.println("sink: "+sinkDirectory);
		
		if (!Files.isReadable(sourceFile)) throw new NoSuchFileException(sourceFile.toString());

		// TODO: If the target directory is not null, issue a CWD message to the FTP server,
		// setting it's current working directory to the target directory. Send a PASV
		// message to query the socket-address to be used for the data transfer.
		// You can use parseSocketAddress() to parse the socket-address from the response.
		// Open a data connection to the socket-address using "new Socket(host, port)".
		// Send a STOR message over the control connection. After receiving the first part of
		// it's response (code 150),
		// transport the source file content to the data connection's
		// OUTPUT stream, closing it once there is no more data. Then receive the second part
		// of the STOR response (code 226) on the control connection. Make sure the source file
		// and the data connection are closed in any case.
	}


	/**
	 * Returns a socket address (address & port) out of a PASV command's return message.
	 * = the address of the addressed server
	 * 
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
		//print all arguments:
		for(int i=0; i<args.length;i++){
			System.out.println("args["+i+"]: "+args[i]);
		}
		
		final InetSocketAddress hostAddress = new SocketAddress(args[0]).toInetSocketAddress();
		final String alias = args[1];
		final String password = args[2];
		final boolean binaryMode = Boolean.parseBoolean(args[3]);
		final Mode mode = Mode.valueOf(args[4]);
		final Path sourcePath = Paths.get(args[5]).normalize();
		final Path targetPath = Paths.get(args[6]).normalize();

		
		try (FtpClientMine client = new FtpClientMine(hostAddress, alias, password, binaryMode)) {
			switch (mode) {
				case RETRIEVE: {
					System.out.println("retrieving...");
					client.retrieve(sourcePath, targetPath);
					break;
				}
				case STORE: {
					System.out.println("storing...");
					client.store(sourcePath, targetPath);
					break;
				}
			}
		}
	}
}