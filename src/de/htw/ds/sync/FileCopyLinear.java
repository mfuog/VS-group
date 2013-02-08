package de.htw.ds.sync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import de.sb.javase.TypeMetadata;


/**
 * <p>Demonstrates copying a file using a single thread.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class FileCopyLinear {

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

		Files.copy(sourcePath, sinkPath, StandardCopyOption.REPLACE_EXISTING);

		System.out.println("done.");
	}
}