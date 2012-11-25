package de.htw.ds.sort;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import de.htw.ds.TypeMetadata;


/**
 * <p>This class implements a single-threaded file sorter test case. It sorts all
 * non-empty trimmed lines of a an input file into an output file.</p>
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.1", authors="Sascha Baumeister")
public final class SortClient1 extends SortClient {

	/**
	 * Creates a new sort client.
	 * @param sourcePath the source file path
	 * @param targetPath the target file path
	 * @param streamSorter the stream sorter
	 */
	public SortClient1(final Path sourcePath, final Path targetPath, final StreamSorter<String> streamSorter) {
		super(sourcePath, targetPath, streamSorter);
	}

	/**
	 * Processes a stream sort test case. Arguments are the path to the input file, and
	 * the path of the sorted output file.
	 * @param args the given runtime arguments
	 * @throws IOException if there is an I/O related problem
	 */
	public static void main(final String[] args) throws IOException {
		final Path sourcePath = Paths.get(args[0]);
		final Path targetPath = Paths.get(args[1]);

		final StreamSorter<String> sorter = SortClient1.createSorter();
		final SortClient1 client = new SortClient1(sourcePath, targetPath, sorter);
		client.process();
	}


	/**
	 * Returns a new stream sorter.
	 * @return the stream sorter
	 */
	private static StreamSorter<String> createSorter() {
		return new StreamSingleThreadSorter<>();
	}
}