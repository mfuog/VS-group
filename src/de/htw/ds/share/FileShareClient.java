package de.htw.ds.share;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.ws.Service;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;
import de.sb.javase.xml.Namespaces;


/**
 * <p>Simple command line based client for the file share service. Note that
 * it is possible to simulate a complex P2P network within one machine,
 * by starting multiple servers on separate ports, and multiple clients 
 * for some of the former.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class FileShareClient {
	private static enum Command {
		QUERY, STATUS, START_DOWNLOAD, STOP_DOWNLOAD
	}


	/**
	 * Application entry point. The runtime parameters must be a service URI and a
	 * command, followed by parameters that depend on the given command. Running
	 * without any parameters, or with illegal parameters, yields a syntax hint.
	 * @param args the given runtime arguments
	 * @throws WebServiceException if the local file share service isn't startet
	 * @throws NumberFormatException if a given numeric value is not valid
	 * @throws IOException if there is an I/O related problem
	 * @throws URISyntaxException 
	 */
	public static void main(final String[] args) throws IOException {
		final Command command;
		final int localServicePort;
		try {
			localServicePort = Integer.parseInt(args[0]);
			command = Command.valueOf(args[1].toUpperCase());
		} catch (final Exception exception) {
			System.out.println("Syntax: <localServicePort:int> <query | status | add_download | remove_download> {options}");
			return;
		}

		final URL wsdlLocator = FileShareResources.serviceWsdlLocator(new SocketAddress(localServicePort));
		final Service proxyFactory = Service.create(wsdlLocator, Namespaces.toQualifiedName(FileShareService.class));
		final FileShareService proxy = proxyFactory.getPort(FileShareService.class);

		switch (command) {
			case QUERY: {
				final String[] fragments, types;
				final Long minSize, maxSize, timeout;
				try {
					fragments = args[2].length() == 0 ? null : args[2].split(",");
					types = args[3].length() == 0 ? null : args[3].split(",");
					minSize = args[4].length() == 0 ? null : Long.parseLong(args[4]);
					maxSize = args[5].length() == 0 ? null : Long.parseLong(args[5]);
					timeout = Long.parseLong(args[6]);
				} catch (final Exception exception) {
					System.out.println("Query syntax: <localServicePort:int> query <fragments:string> <type:String> <minSize:long> <maxSize:long> <timeout:long>");
					break;
				}

				final FileDescriptor[] fileDescriptors = proxy.queryFileDescriptors(fragments, types, minSize, maxSize, timeout);
				for (final FileDescriptor fileDescriptor : fileDescriptors) {
					System.out.format("hash=%s\n", FileShareResources.toHexString(fileDescriptor.getContentHash()));
					System.out.format("length=%s\n", fileDescriptor.getContentLength());
					for (final String fileName : fileDescriptor.getFileNames()) {
						System.out.format("name=%s\n", fileName);
					}
					System.out.println();
				}
				break;
			}
			case STATUS: {
				final DownloadDescriptor[] statusEntries = proxy.getDownloadDescriptors();
				System.out.println("Status report for current downloads:");
				if (statusEntries.length == 0) {
					System.out.println("No active downloads!");
				} else {
					for (final DownloadDescriptor entry : statusEntries) {
						System.out.print("hash=0x");
						System.out.print(entry.getContentHash().toString(16).toUpperCase());
						System.out.print(", file=");
						System.out.print(entry.getFilePath());
						System.out.print(", fileSize=");
						System.out.print(entry.getContentLength());
						System.out.print(", acquirable=");
						System.out.print(Math.round(100 * entry.getAcquirableChunkRatio()));
						System.out.print("%, acquired=");
						System.out.print(Math.round(100 * entry.getAcquiredChunkRatio()));
						System.out.print("%, committed=");
						System.out.print(Math.round(100 * entry.getCommittedChunkRatio()));
						System.out.println("%");
					}
				}
				break;
			}
			case START_DOWNLOAD: {
				final BigInteger contentHash;
				final Path filePath;
				try {
					contentHash = args[2].startsWith("0x")
						? new BigInteger(args[2].substring(2), 16)
						: new BigInteger(args[2], 10);
					filePath = Paths.get(args[3]).normalize().toAbsolutePath();
				} catch (final Exception exception) {
					System.out.println("Download syntax: <localServicePort:int> start_download <content-hash:BigInteger> <filePath:Path>");
					break;
				}

				final boolean ok = proxy.startDownload(contentHash, filePath);
				System.out.println("Download " + (ok ? "registered." : "rejected."));
				break;
			}
			case STOP_DOWNLOAD: {
				final BigInteger contentHash;
				try {
					contentHash = args[2].startsWith("0x")
					? new BigInteger(args[2].substring(2), 16)
					: new BigInteger(args[2], 10);
				} catch (final Exception exception) {
					System.out.println("Download syntax: <localServicePort:int> stop_download <file-hash:BigInteger>");
					break;
				}

				final boolean ok = proxy.stopDownload(contentHash);
				System.out.print("Download ");
				System.out.println(ok ? "aborted." : "doesn't exist.");
				break;
			}
		}
	}
}