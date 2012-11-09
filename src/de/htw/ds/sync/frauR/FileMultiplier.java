package de.htw.ds.sync.frau_r;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import de.htw.ds.TypeMetadata;
import de.htw.ds.util.MultiInputStream;


/**
 * <p>Demonstrates multiplying a file using a single thread.</p>
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class FileMultiplier {

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

		final int copyCount = Integer.parseInt(args[2]);
		if (copyCount < 0) throw new IllegalArgumentException(Integer.toString(copyCount));

		final InputStream[] sources = new InputStream[copyCount];
		for (int index = 0; index < sources.length; ++index) {
			sources[index] = Files.newInputStream(sourcePath);
		}

		try (
			InputStream source = new MultiInputStream(sources);
			OutputStream sink = Files.newOutputStream(sinkPath);
		) {
			final byte[] buffer = new byte[0x010000];
			for (int bytesRead = source.read(buffer); bytesRead != -1; bytesRead = source.read(buffer)) {
				sink.write(buffer, 0, bytesRead);
			}
		}

		System.out.println("done.");
	}
}