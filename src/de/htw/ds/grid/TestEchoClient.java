package de.htw.ds.grid;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import de.sb.javase.TypeMetadata;


/**
 * <p>Static test client for the EchoAgent class.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class TestEchoClient {

	/**
	 * Prevents instantiation.
	 */
	private TestEchoClient() {
		super();
	}


	/**
	 * Application entry point. The given runtime parameter must be the service URI of the
	 * local grid container.</p>
	 * @param args the given runtime arguments
	 * @throws IOException if there is a problem storing the data content
	 * @throws URISyntaxException if the given URI is malformed
	 */
	public static void main (final String[] args) throws IOException, URISyntaxException {
		final URI serviceURI = new URI(args[0]);
		final Path dataPath = Files.createTempFile("test-", "txt");
		try {
			Files.write(dataPath, new byte[] { 64, 65, 66, 67, 68, 69, 70 });

			final GridTask task = new GridTask(serviceURI, EchoAgent.class.getName(), null, dataPath, null, 1);
			task.run();
		} finally {
			Files.deleteIfExists(dataPath);
		}
	}
}