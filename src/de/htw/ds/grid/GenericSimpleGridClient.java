package de.htw.ds.grid;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import de.sb.javase.TypeMetadata;
import de.sb.javase.xml.Namespaces;


/**
 * <p>Static grid client for generic file processing. It causes a
 * single file to be processed by a single grid peer.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class GenericSimpleGridClient {
	private static final String LOG_BEGIN = "Processing file \"{0}\" using agent class {1}.";
	private static final String LOG_CONNECTED = "Service {0} is available.";
	private static final String LOG_CREATED = "Grid task %d created successfully.";
	private static final String LOG_END = "Grid task %d no longer active, check server output directory \"{0}\" for processing result.";

	/**
	 * Prevents instantiation.
	 */
	private GenericSimpleGridClient() {
		super();
	}


	/**
	 * Application entry point. The given runtime parameters must be the service port and name
	 * of the local grid container, that agent class name, the path of the jar-file containing
	 * the agent's code, the path of the input file to be processed, the timeToLive of each
	 * agent task before it is reissued, and optionally properties as multiple name=value
	 * pairs. For example, a valid set of invocation parameters might be:
	 * <pre>
	 * http://tabernakel:8002/GridService
	 * de.htw.ds.grid.agent.ImageResizeAgent
	 * "C:/Users/sascha/Kurskomponenten/Verteilte Systeme/distributed-systems-plugins.jar"
	 * "C:/temp/input.jpg" 5 60 width=50 height=50
	 * </pre>
	 * @param args the given runtime arguments
	 * @throws WebServiceException if there is a web-service related problem
	 * @throws URISyntaxException if the given service URI is invalid
	 * @throws MalformedURLException if the given service URI cannot be converted into
	 *    a valid URL
	 * @throws NoSuchFileException if any of the given paths is not <tt>null</tt>,
	 *     but doesn't point to a regular file
	 * @throws AccessDeniedException if any of the given paths is not <tt>null</tt>,
	 *    but the file cannot be read
	 */
	public static void main (final String[] args) throws URISyntaxException, MalformedURLException, URISyntaxException, NoSuchFileException, AccessDeniedException {
		final URI localServiceURI	= new URI(args[0]);
		final String agentClassName = args[1];
		final Path jarPath			= Paths.get(args[2]).normalize().toAbsolutePath();
		final Path dataPath			= Paths.get(args[3]).normalize().toAbsolutePath();
		final int tryCount			= Integer.parseInt(args[4]);
		final int timeToLive		= Integer.parseInt(args[5]);

		final Map<String,String> properties = new HashMap<>();
		for (int index = 6; index < args.length; ++index) {
			final String[] texts = args[index].split("=");
			properties.put(texts[0], texts[1]);
		}

		Logger.getGlobal().log(Level.INFO, LOG_BEGIN, new Object[] { dataPath.toAbsolutePath(), agentClassName });

		final URL wsdlLocator = new URL(localServiceURI.toASCIIString() + "?wsdl");
		final Service proxyFactory = Service.create(wsdlLocator, Namespaces.toQualifiedName(GridService.class));
		final GridService proxy = proxyFactory.getPort(GridService.class);
		Logger.getGlobal().log(Level.INFO, LOG_CONNECTED, localServiceURI);

		final Path outputPath = proxy.getLocalOutputPath();
		final long taskIdentity = proxy.createTask(agentClassName, jarPath, dataPath, properties, tryCount, timeToLive);
		Logger.getGlobal().log(Level.INFO, String.format(LOG_CREATED, taskIdentity));

		while (proxy.hasActiveTask(taskIdentity)) {
			try {
				Thread.sleep(100);
			} catch (final InterruptedException interrupt) {
				throw new ThreadDeath();
			}
		}
		Logger.getGlobal().log(Level.INFO, String.format(LOG_END, taskIdentity), outputPath);
	}
}