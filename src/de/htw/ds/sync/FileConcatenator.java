package de.htw.ds.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import de.htw.ds.TypeMetadata;


/**
 * <p>Demonstrates concatenating multiple files into one output file.</p>
 */
@TypeMetadata(copyright="2012-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class FileConcatenator {

	/**
	 * Concatenates files. The first argument is expected to be a qualified
	 * target file name, while the others must be a qualified source file names. 
	 * @param args the VM arguments
	 * @throws IllegalArgumentException if one of the given paths is not a valid file path 
	 * @throws IOException if there's an I/O related problem
	 */
	public static void main(final String[] args) throws IOException {
		final Path[] sourcePaths = new Path[args.length - 1];
		for (int index = 0; index < sourcePaths.length; ++index) {
			final Path sourcePath = Paths.get(args[index + 1]);
			if (!Files.isReadable(sourcePath)) throw new IllegalArgumentException(sourcePath.toString());
			sourcePaths[index] = sourcePath;
		}

		final Path sinkPath = Paths.get(args[0]);
		if (sinkPath.getParent() != null && !Files.isDirectory(sinkPath.getParent())) throw new IllegalArgumentException(sinkPath.toString());

		final InputStream[] sources = new InputStream[sourcePaths.length];
		for (int index = 0; index < sources.length; ++index) {
			sources[index] = Files.newInputStream(sourcePaths[index]);
		}

		try (OutputStream fileSink = Files.newOutputStream(sinkPath)) {
			for (final Path sourcePath : sourcePaths) {
				Files.copy(sourcePath, fileSink);
			}
		}

		System.out.println("done.");
	}
}