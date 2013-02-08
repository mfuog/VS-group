package de.htw.ds.grid;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import de.sb.javase.Reference;
import de.sb.javase.TypeMetadata;
import de.sb.javase.xml.Namespaces;


/**
 * <p>Static audio grid client that partitions an audio file into many chunks,
 * alters these using an agent, and reassembles an audio file from the results.
 * Note that Java Audio only supports standard wave-table files unless a matching
 * audio library is plugged into the JVM.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class GenericAudioGridClient {
	private static final String LOG_BEGIN = "Processing audio file \"{0}\".";
	private static final String LOG_DECOMPOSE = "File contains {0} audio frames, decomposing into chunks.";
	private static final String LOG_PROCESS = "Created {0} chunks, sending to {1} for processing.";
	private static final String LOG_FORK = "Started grid task %d for chunk #{0}.";
	private static final String LOG_JOIN = "Waiting for {0} grid tasks to end.";
	private static final String LOG_RECOMPOSE = "Recomposing {0} chunks into \"{1}\".";
	private static final String LOG_SUCCESS = "Audio file successfully created, execution time was {0}ms.";

	/**
	 * Prevents instantiation.
	 */
	private GenericAudioGridClient() {
		super();
	}


	/**
	 * Decomposes the given audio file into chunks. Note that the given audio
	 * source is closed after processing!
	 * @param audioSource the audio source
	 * @param chunkSize the chunk size in audio frames
	 * @return the paths to the chunks created
	 * @throws IOException if there is an I/O related problem
	 */
	private static Path[] decompose(final AudioInputStream audioSource, final int chunkSize) throws IOException {
		final List<Path> chunkPaths = new ArrayList<Path>();
		final AudioFormat audioFormat = audioSource.getFormat();
		final byte[] buffer = new byte[audioFormat.getFrameSize() * chunkSize];

		try (InputStream byteSource = new BufferedInputStream(audioSource)) {
			for (int bytesRead = byteSource.read(buffer); bytesRead != -1; bytesRead = byteSource.read(buffer)) {
				final InputStream chunkSource = new ByteArrayInputStream(buffer, 0, bytesRead);
				final Path chunkPath = Files.createTempFile("audio", ".cnk");
				Files.copy(chunkSource, chunkPath, StandardCopyOption.REPLACE_EXISTING);
				chunkPaths.add(chunkPath);
			}
		} catch (final IOException exception) {
			for (final Path inputChunk : chunkPaths) {
				Files.deleteIfExists(inputChunk);
			}
			throw exception;
		}
		return chunkPaths.toArray(new Path[0]);
	}


	/**
	 * Starts a grid task for each input chunk given, polls until all the tasks
	 * have become inactive, and returns the expected paths of the output chunks.
	 * @param localServiceURI the local service URI
	 * @param className the agent class name
	 * @param jarFile the jar-file
	 * @param inputChunks the paths of the input chunks
	 * @param properties the optional properties steering the processing
	 * @param tryCount the maximum number of times the task will be scheduled
	 * @param timeToLive the maximum number of seconds a task computation may require
	 * @return the paths of the output chunks
	 * @throws IllegalArgumentException if the service URI is not a valid URL
	 * @throws WebServiceException if there is a web-service related problem
	 * @throws IOException if there is an I/O related problem
	 */
	private static Path[] executeTasks(final URI localServiceURI, final String className, final Path jarPath, final Path[] inputChunks, final Map<String,String> properties, final int tryCount, final int timeToLive) throws IOException {
		final URL wsdlLocator = new URL(localServiceURI.toASCIIString() + "?wsdl");
		final Service proxyFactory = Service.create(wsdlLocator, Namespaces.toQualifiedName(GridService.class));
		final GridService proxy = proxyFactory.getPort(GridService.class);

		final long[] taskIdentities = new long[inputChunks.length];
		for (int index = 0; index < inputChunks.length; ++index) {
			final Path inputChunk = inputChunks[index];
			final long taskIdentity = proxy.createTask(className, jarPath, inputChunk, properties, tryCount, timeToLive);
			Logger.getGlobal().log(Level.INFO, String.format(LOG_FORK, taskIdentity), index);
			taskIdentities[index] = taskIdentity;
		}
		final Path outputDirectory = proxy.getLocalOutputPath();

		Logger.getGlobal().log(Level.INFO, LOG_JOIN, taskIdentities.length);
		for (final Long taskIdentity : taskIdentities) {
			while (proxy.hasActiveTask(taskIdentity)) {
				try {
					Thread.sleep(100);
				} catch (final InterruptedException interrupt) {
					throw new ThreadDeath();
				}
			}
		}

		final Path[] outputChunks = new Path[inputChunks.length];
		for (int index = 0; index < outputChunks.length; ++index) {
			final long taskIdentity = taskIdentities[index];
			outputChunks[index] = outputDirectory.resolve(Long.toString(taskIdentity) + ".cnk");
		}
		return outputChunks;
	}


	/**
	 * Recomposes the given audio output file from the given chunks.
	 * @param audioSinkPath the audio sink path
	 * @param audioFormat the audio output format
	 * @param chunks the chunks to be recomposed
	 * @throws IOException if there is an I/O related problem
	 */
	private static void recompose(final Path audioSinkPath, final AudioFormat audioFormat, final Path[] chunks) throws IOException {
		final Reference<Throwable> exceptionReference = new Reference<Throwable>();

		try (PipedInputStream pipedSource = new PipedInputStream()) {
			final Runnable runnable = new Runnable() {
				public void run() {
					try (PipedOutputStream pipedSink = new PipedOutputStream(pipedSource)) {
						for (final Path chunk : chunks) {
							final byte[] chunkContent = Files.readAllBytes(chunk);
							pipedSink.write(chunkContent);
						}
					} catch (final Throwable exception) {
						exceptionReference.put(exception);
					}
				}
			};
			new Thread(runnable).start();

			try (AudioInputStream audioSink = new AudioInputStream(pipedSource, audioFormat, AudioSystem.NOT_SPECIFIED)) {
				AudioSystem.write(audioSink, AudioFileFormat.Type.WAVE, audioSinkPath.toFile());
			}
		}

		final Throwable exception = exceptionReference.get();
		if (exception instanceof Error) throw (Error) exception;
		if (exception instanceof RuntimeException) throw (RuntimeException) exception;
		if (exception instanceof IOException) throw (IOException) exception;
		assert exception == null;
	}


	/**
	 * Application entry point. The given runtime parameters must be the service port and name
	 * of the local grid container, that agent class name, the path of the jar-file containing the
	 * agent's code, the wave-table input file, the path of the wave-table generated as a result,
	 * the maximum size of each individual chunk to be processed by an agent in frames, the
	 * timeToLive of each agent task before it is reissued, and optionally properties as
	 * multiple name=value pairs. For example, a set of valid invocation parameters might be
	 * <pre>
	 * http://tabernakel:8002/GridService
	 * de.htw.ds.grid.agent.AudioDoomAgent
	 * "C:/Users/sascha/Kurskomponenten/Verteilte Systeme/distributed-systems-plugins.jar"
	 * "C:/temp/input.wav C:/temp/output.wav 10 600 2000000 compressionRatio=3.0
	 * <pre>
	 * @param args the given runtime arguments
	 * @throws WebServiceException if there is a web-service related problem
	 * @throws URISyntaxException if the service URI is not a valid URL
	 * @throws UnsupportedAudioFileException if the file format is not supported, i.e. not
	 *    a standard stereo, 16bit, and 44100hz sample rate wave-table
	 * @throws IOException if there is an I/O related problem
	 */
	public static void main (final String[] args) throws URISyntaxException, UnsupportedAudioFileException, IOException  {
		final long timestamp = System.currentTimeMillis();
		final URI localServiceURI	= new URI(args[0]);
		final String agentClassName	= args[1];
		final Path jarPath			= Paths.get(args[2]).normalize().toAbsolutePath();
		final Path audioInputPath	= Paths.get(args[3]).normalize();
		final Path audioOutputPath	= Paths.get(args[4]).normalize();
		final int tryCount			= Integer.parseInt(args[5]);
		final int timeToLive		= Integer.parseInt(args[6]);
		final int chunkSize			= Integer.parseInt(args[7]);

		final Map<String,String> properties = new HashMap<>();
		for (int index = 8; index < args.length; ++index) {
			final String[] texts = args[index].split("=");
			properties.put(texts[0], texts[1]);
		}

		Logger.getGlobal().log(Level.INFO, LOG_BEGIN, new Object[] { audioInputPath.toAbsolutePath().toString() });
		final AudioInputStream audioSource = AudioSystem.getAudioInputStream(audioInputPath.toFile());
		final AudioFormat audioFormat = audioSource.getFormat();

		Logger.getGlobal().log(Level.INFO, LOG_DECOMPOSE, audioSource.getFrameLength());
		final Path[] inputChunks = decompose(audioSource, chunkSize);

		try {
			Logger.getGlobal().log(Level.INFO, LOG_PROCESS, new Object[] { inputChunks.length, localServiceURI });
			final Path[] outputChunks = executeTasks(localServiceURI, agentClassName, jarPath, inputChunks, properties, tryCount, timeToLive);

			try {
				Logger.getGlobal().log(Level.INFO, LOG_RECOMPOSE, new Object[] { inputChunks.length, audioOutputPath });
				recompose(audioOutputPath, audioFormat, outputChunks);
			} finally {
				for (final Path outputChunk : outputChunks) {
					if (outputChunk != null) Files.deleteIfExists(outputChunk);
				}
			}
		} finally {
			for (final Path inputChunk : inputChunks) {
				if (inputChunk != null) Files.deleteIfExists(inputChunk);
			}
		}

		Logger.getGlobal().log(Level.INFO, LOG_SUCCESS, System.currentTimeMillis() - timestamp);
	}
}