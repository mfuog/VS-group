package de.htw.ds.sync.frau_r;

import java.io.IOException;
import java.io.PrintStream;
import de.htw.ds.TypeMetadata;
import de.htw.ds.util.BinaryTransporter;


/**
 * <p>Demonstrates child process initiation and resynchronization inside the parent
 * process, using the polling method. Additionally demonstrates process I/O redirection.</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class ResyncProcessByPolling {

	/**
	 * Application entry point. The single parameters must be a command line suitable to
	 * start a program/process.
	 * @param args the arguments
	 * @throws IOException if there's an I/O related problem
	 * @throws InterruptedException if the main thread waiting for the process to end is interrupted
	 */
	public static void main(final String[] args) throws InterruptedException, IOException {
		System.out.println("Starting process... ");
		final Process process = Runtime.getRuntime().exec(args[0]);

		System.out.println("Connecting process I/O streams with current Java process... ");
		// System.in transporter must be started as a daemon thread, otherwise read-block prevents termination!
		final Thread sysinThread = new Thread(new BinaryTransporter(false, 0x10, System.in, new PrintStream(process.getOutputStream())));
		sysinThread.setDaemon(true);
		sysinThread.start();
		new Thread(new BinaryTransporter(false, 0x10, process.getErrorStream(), System.err)).start();
		new Thread(new BinaryTransporter(false, 0x10, process.getInputStream(), System.out)).start();

		System.out.println("Resynchronising process... ");
		final long timestamp = System.currentTimeMillis();

		int exitCode;
		while (true) {
			try {
				exitCode = process.exitValue();
				break;
			} catch (final IllegalThreadStateException exception) {
				// sleep a millisecond to prevent CPU from running at 100% needlessly!
				Thread.sleep(1);
			}
		}

		System.out.format("Process ended with exit code %s after running %sms.\n", exitCode, System.currentTimeMillis() - timestamp);
	}
}