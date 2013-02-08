package de.htw.ds.sync;

import java.io.IOException;
import java.io.PrintStream;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.BinaryTransporter;


/**
 * <p>Demonstrates child process initiation and resynchronization inside the parent
 * process, using the blocking method. Additionally demonstrates process I/O redirection,
 * and a situationally correct way of handling {@link InterruptedException}. Note that
 * there is no "golden" way to handle thread interruption which is always correct, i.e.
 * the handling must always take circumstances into account! However, an interruption
 * MUST always be handled immediately, in one or more of the following ways:</p><ul>
 * <li>ignoring or just logging it:<ul>
 *     <li>if the interrupted thread or it's process will end soon anyways</li>
 *     <li>OR if the interrupted thread is a service/monitor thread that other threads
 *         depend on, and which must therefore not be stopped before it's natural
 *         end</li></ul></li>
 * <li>re-interruption of the interrupted thread (or it's parent thread) at a later
 *     time using {@link Thread#interrupt()}:<ul>
 *     <li>if the interruption doesn't reappear immediately (interruptible loops)</li>
 *     <li>AND if interruptible operations follow later (otherwise the thread will not
 *         be interrupted again before it's natural end)</li></ul></li>
 * <li>terminating the interrupted process using {@link System#exit(int)}:<ul>
 *     <li>if process termination is safe with regard to other processes</li>
 *     <li>AND such behavior would be acceptable to process users</li></ul></li>
 * <li>terminating the interrupted thread by throwing {@link ThreadDeath}:<ul>
 *     <li>if it is safe that a method caller may catch <tt>ThreadDeath</tt> without
 *         rethrowing it (not recommended but can happen), thereby preventing thread
 *         termination</li>
 *     <li>AND if thread termination is safe with regard to open resources, which the use of
 *         {@link AutoCloseable#close()} in finally-blocks or try-with-resource blocks should
 *         always ensure anyways, because other errors like {@link OutOfMemoryError} can
 *         occur at any time</li>
 *     <li>AND if thread termination is safe with regard to other threads, i.e. other threads
 *         do not depend on the interrupted thread running until it's natural end</li>
 *         </ul></li>
 * <li>simply declaring {@link InterruptedException} in the methods throws clause:<ul>
 *     <li>ONLY allowed if the method provides system level functionality, or is the
 *         <tt>main()</tt> method of a process whose other threads don't depend on the
 *         main thread to end naturally!</li>
 *     <li>REASON: Higher call stacks have less and less chance to determine a correct way
 *         of handling the exception, which in effect would cause the exception declaration
 *         to spread like cancer throughout an API without any benefit! If you want to
 *         terminate the thread or process, throw {@link ThreadDeath} or use
 *         {@link System#exit(int)} instead. The main reason <tt>InterruptedException</tt>
 *         was introduced into Java was to force the immediate (!) caller of a blocking method
 *         to CHOOSE one of the possibilities in handling the interruption described above.
 *         Before that, system level methods threw <tt>ThreadDeath</tt>, which did not force
 *         programmers to THINK first about the implications of premature thread termination,
 *         and which subsequently caused a lot of problems when circumstances where not safe
 *         for this (see above).</li></ul></li></ul>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class ResyncProcessByBlocking {

	/**
	 * Application entry point. The single parameters must be a command line
	 * suitable to start a program/process.
	 * @param args the arguments
	 * @throws IndexOutOfBoundsException if no argument is passed
	 * @throws IOException if there's an I/O related problem
	 */
	public static void main(final String[] args) throws IOException {
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
		try {
			final int exitCode = process.waitFor();
			System.out.format("Process ended with exit code %s after running %sms.\n", exitCode, System.currentTimeMillis() - timestamp);
		} catch (final InterruptedException interrupt) {
			// CHOSING interrupt logging because this thread (and it's process) ends soon anyways (see class comment!)
			System.out.format("Process resynchronization was interrupted, exiting parent process");
		}
	}
}