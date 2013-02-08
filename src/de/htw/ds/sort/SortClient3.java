package de.htw.ds.sort;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Queue;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;


/**
 * <p>This class implements a multi-programmed file sorter test case. It sorts all
 * non-empty trimmed lines of a an input file into an output file</p>.
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.2.1", authors="Sascha Baumeister")
public final class SortClient3 extends SortClient {

	/**
	 * Creates a new sort client.
	 * @param sourcePath the source file path
	 * @param targetPath the target file path
	 * @param streamSorter the stream sorter
	 */
	public SortClient3(final Path sourcePath, final Path targetPath, final StreamSorter<String> streamSorter) {
		super(sourcePath, targetPath, streamSorter);
	}


	/**
	 * Processes a stream sort test case. Arguments are the path to the input file, and
	 * the path of the sorted output file. Additionally, some socket-addresses (at least
	 * one) must be specified that point to available sort servers.
	 * @param args the given runtime arguments
	 * @throws IOException if there is an I/O related problem
	 */
	public static void main(final String[] args) throws IOException {
		final Path sourcePath = Paths.get(args[0]);
		final Path targetPath = Paths.get(args[1]);

		final InetSocketAddress[] socketAddresses = new InetSocketAddress[args.length - 2];
		for (int index = 0; index < socketAddresses.length; ++index) {
			socketAddresses[index] = new SocketAddress(args[index + 2]).toInetSocketAddress();
		}

		final StreamSorter<String> sorter = SortClient3.createSorter(socketAddresses);
		final SortClient3 client = new SortClient3(sourcePath, targetPath, sorter);
		client.process();
	}


	/**
	 * Returns a new stream sorter.
	 * @return the stream sorter
	 */
	private static StreamSorter<String> createSorter(final InetSocketAddress[] socketAddresses) {
		final Queue<StreamSorter<String>> sorterQueue = new ArrayDeque<>();
		for (final InetSocketAddress socketAddress : socketAddresses) {
			sorterQueue.add(new StreamMultiProgramSorter(socketAddress));
		}

		while (sorterQueue.size() > 1) {
			sorterQueue.add(new StreamMultiThreadSorter<>(sorterQueue.poll(), sorterQueue.poll()));
		}

		return sorterQueue.poll();
	}
}