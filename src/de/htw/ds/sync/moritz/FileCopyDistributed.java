package de.htw.ds.sync.moritz;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import de.htw.ds.TypeMetadata;

/**
 * <p>
 * Demonstrates copying a file using a single thread.
 * </p>
 */
@TypeMetadata(copyright = "2008-2012 Sascha Baumeister, all rights reserved",
		version = "0.2.2", authors = "Sascha Baumeister")
public final class FileCopyDistributed
{
	
	private final static int	BUFFER_SIZE	= 0x100000;
	
	/**
	 * Copies a file. The first argument is expected to be a qualified source
	 * file name, the second a qualified target file name.
	 * 
	 * @param args
	 *            the VM arguments
	 * @throws IOException
	 *             if there's an I/O related problem
	 */
	public static void main(final String[] args) throws IOException
	{
		final Path sourcePath = Paths.get(args[0]);
		if (!Files.isReadable(sourcePath)) throw new IllegalArgumentException(
				sourcePath.toString());
		
		final Path sinkPath = Paths.get(args[1]);
		if (sinkPath.getParent() != null
				&& !Files.isDirectory(sinkPath.getParent())) throw new IllegalArgumentException(
				sinkPath.toString());
		
		try
		{
			final PipedInputStream inputStream = new PipedInputStream(
					BUFFER_SIZE);
			final PipedOutputStream outputStream = new PipedOutputStream(
					inputStream);
			
			new Thread(new Transporter(Files.newInputStream(sourcePath),
					outputStream)).start();
			new Thread(new Transporter(inputStream,
					Files.newOutputStream(sinkPath))).start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		System.out.println("done.");
	}
	
	private static final class Transporter implements Runnable
	{
		private final InputStream	in;
		private final OutputStream	out;
		
		public Transporter(InputStream in, OutputStream out)
		{
			super();
			this.in = in;
			this.out = out;
		}
		
		@Override
		public void run()
		{
			try
			{
				final byte[] readwriteBlock= new byte[BUFFER_SIZE];
				while (in.available() != 0
						&& in.read(readwriteBlock) != -1)
					out.write(BUFFER_SIZE);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}