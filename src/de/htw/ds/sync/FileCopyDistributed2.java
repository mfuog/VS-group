package de.htw.ds.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import de.htw.ds.TypeMetadata;
import de.htw.ds.util.BinaryTransporter;


/**
 * <p>Demonstrates copying a file using two separate threads for file-read and file-write.
 * Note that this is only expected to be more efficient that a single-threaded implementation
 * when using multi-core systems with multiple hard drives!</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class FileCopyDistributed2 {
	private static final int BUFFER_LENGTH = 0x100000;

	/**
	 * Copies a file. The first argument is expected to be a qualified source file name,
	 * the second a qualified target file name. 
	 * @param args the VM arguments
	 * @throws IOException if there's an I/O related problem
	 */
	public static void main(final String[] args) throws IOException {
		final Path sourcePath = Paths.get(args[0]);
		if (!Files.isReadable(sourcePath)) throw new IllegalArgumentException(sourcePath.toString());

		final Path sinkPath = Paths.get(args[1]);
		if (sinkPath.getParent() != null && !Files.isDirectory(sinkPath.getParent())) throw new IllegalArgumentException(sinkPath.toString());

		InputStream fileSource = null;
		OutputStream fileSink = null;
		try {
			// open file streams
			fileSource = Files.newInputStream(sourcePath);
			fileSink = Files.newOutputStream(sinkPath);

			// create inter-thread pipe
			final PipedInputStream pipedSource = new PipedInputStream(BUFFER_LENGTH);
			final PipedOutputStream pipedSink = new PipedOutputStream(pipedSource);

			final Runnable fileSourceTransporter = new BinaryTransporter(true, BUFFER_LENGTH, fileSource, pipedSink);
			final Runnable fileSinkTransporter = new BinaryTransporter(true, BUFFER_LENGTH, pipedSource, fileSink);

			new Thread(fileSourceTransporter, "source-transporter").start();
			new Thread(fileSinkTransporter, "sink-transporter").start();
			System.out.println("two transporter threads started.");
		} catch (final Throwable exception) {
			try { fileSource.close(); } catch (final Throwable nestedException) {}
			try { fileSink.close(); } catch (final Throwable nestedException) {}
			throw exception;
		}
	}
}