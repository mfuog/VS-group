package de.htw.ds.sync.myrtha;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import de.htw.ds.TypeMetadata;


/**
 * <p>Demonstrates copying a file using two separate threads for file-read and file-write.
 * Note that this is only expected to be more efficient that a single-threaded implementation
 * when using multi-core systems with multiple hard drives!</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class FileCopyDistributed1 {
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

		// create inter-thread pipe
		final PipedInputStream pipedSource = new PipedInputStream(BUFFER_LENGTH);
		final PipedOutputStream pipedSink = new PipedOutputStream(pipedSource);

		final Runnable fileSourceTransporter = new Runnable() {
			public void run() {
				try {
					Files.copy(sourcePath, pipedSink);	//verschiedene Methode als das andere copy()
				} catch (final Throwable exception) {
					exception.printStackTrace();
				} finally {
					try { pipedSink.close(); } catch (final Throwable exception) {}
				}
			}
		};

		// start threads
		final Runnable fileSinkTransporter = new Runnable() {
			public void run() {
				try {
					//verschiedene copy()-Methode als oben
					Files.copy(pipedSource, sinkPath, StandardCopyOption.REPLACE_EXISTING);
				} catch (final Throwable exception) {
					exception.printStackTrace();
				} finally {
					try { pipedSource.close(); } catch (final Throwable exception) {}
				}
			}
		};

		new Thread(fileSourceTransporter, "source-transporter").start();
		new Thread(fileSinkTransporter, "sink-transporter").start();
		System.out.println("two transporter threads started.");
		//non-deamon-Threads -> halten Programm am laufen bis Input zu ende
		//Automatisch beendet wenn Input fertig. Thread1 beendet thread2 indirekt auch
	}
}