package de.htw.ds.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.ProtocolException;
import de.htw.ds.TypeMetadata;


/**
 * <p>This class models FTP responses.</p>
 */
@TypeMetadata(copyright="2011-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class FtpResponse {

	private final short code;
	private final String message;


	/**
	 * Public constructor.
	 * @param code the FTP response code
	 * @param message the FTP response message
	 * @throws NullPointerException if the given message is null
	 */
	public FtpResponse(final short code, final String message) {
		super();
		if (message == null) throw new NullPointerException();

		this.code = code;
		this.message = message;
	}


	/**
	 * Returns the FTP response code.
	 * @return the code
	 */
	public short getCode() {
		return this.code;
	}


	/**
	 * Returns the FTP response message.
	 * @return the message
	 */
	public String getMessage() {
		return this.message;
	}


	/**
	 * ({@inheritDoc}
	 */
	public String toString() {
		return Short.toString(this.code) + " " + this.message;
 	}


	/**
	 * Returns a new FtpResponse Object which contains the ftp return code and message,
	 * read from the given buffered reader.
	 * 
	 * Parses an FTP response from the given reader, and returns an instance of FtpResponse.
	 * @param reader the reader
	 * @return an instance of FtpResponse
	 * @throws NullPointerException if the given reader is null
	 * @throws IOException if there is an I/O related problem
	 */
	public static final FtpResponse parse(final BufferedReader reader) throws IOException {
		final StringWriter writer = new StringWriter();
		short code = -1;

		while (true) {
			final String line = reader.readLine();
			if (line.length() < 4) throw new ProtocolException();

			writer.write(line.substring(4));
			if (line.charAt(3) == ' ') {
				code = Short.parseShort(line.substring(0, 3));
				break;
			} else {
				writer.write("\n");
			}
		}

		return new FtpResponse(code, writer.toString());
	}
}