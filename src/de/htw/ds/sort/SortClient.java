package de.htw.ds.sort;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import de.sb.javase.TypeMetadata;


/**
 * <p>This class defines a file sorter test case. It sorts all non-empty
 * trimmed lines of a an input file into an output file.</p>
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.2.1", authors="Sascha Baumeister")
public abstract class SortClient {

	final Path sourcePath;
	final Path targetPath;
	final StreamSorter<String> streamSorter;


	/**
	 * Creates a new sort client.
	 * @param sourcePath the source file path
	 * @param targetPath the target file path
	 * @param streamSorter the stream sorter
	 */
	public SortClient(final Path sourcePath, final Path targetPath, final StreamSorter<String> streamSorter) {
		super();
		if (sourcePath == null | targetPath == null | streamSorter == null) throw new NullPointerException();

		this.sourcePath = sourcePath;
		this.targetPath = targetPath;
		this.streamSorter = streamSorter;
	}


	/**
	 * Processes the elements to be sorted from from the given source file and
	 * writes the sorted elements to the given target file.
	 * @param sourcePath the source file path
	 * @param targetPath the target file path
	 * @param streamSorter the sorter to be used for sorting
	 * @throws IOException if an I/O related problem occurs
	 */
	public final void process() throws IOException {
		try (BufferedReader charSource = Files.newBufferedReader(this.sourcePath, Charset.forName("ISO-8859-2"))) {
			try (BufferedWriter charSink = Files.newBufferedWriter(this.targetPath, Charset.forName("ISO-8859-2"))) {
				final long timestamp1 = System.currentTimeMillis();
	
				long elementCount = 0;
				for (String line = charSource.readLine(); line != null; line = charSource.readLine()) {
					for (final String element : line.split("\\s")) {
						if (!element.isEmpty()) {
							this.streamSorter.write(element);
							elementCount += 1;
						}
					}
				}
	
				final long timestamp2 = System.currentTimeMillis();
				this.streamSorter.sort();
				final long timestamp3 = System.currentTimeMillis();
	
				for (long todo = elementCount; todo > 0; --todo) {
					final String element = this.streamSorter.read();
					charSink.write(element);
					charSink.newLine();
				}
				charSink.flush();
	
				final long timestamp4 = System.currentTimeMillis();
				System.out.format("Sort ok, %s elements sorted.\n", elementCount);
				System.out.format("Read time: %sms.\n", timestamp2 - timestamp1);
				System.out.format("Sort time: %sms.\n", timestamp3 - timestamp2);
				System.out.format("Write time: %sms.\n", timestamp4 - timestamp3);
			}
		} finally {
			try { this.streamSorter.reset(); } catch (final Exception exception) {}
		}
	}
}