package de.htw.ds.sync.frauR;

import java.io.FileInputStream;
import java.io.FileOutputStream;
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
 * <p>Demonstrates copying a file using a single thread.</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class FileCopyDistributed {
	
	final static int bufferSize = 10000;

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

		// Lineare : --> Files.copy(sourcePath, sinkPath, StandardCopyOption.REPLACE_EXISTING);

		InputStream inputStream = new FileInputStream(sourcePath.toFile());
		OutputStream outputStream = new FileOutputStream(sinkPath.toFile());
		
		final PipedInputStream pI = new PipedInputStream(bufferSize);
		final PipedOutputStream pO = new PipedOutputStream();
		pO.connect(pI);
				
		Thread t1 = new Thread(new Transporter(inputStream, pO));
		Thread t2 = new Thread(new Transporter(pI, outputStream));		
		
		t1.start();
		t2.start();
		
		System.out.println("done.");
	}
	
	static class Transporter implements Runnable{

		private final InputStream inputStream;
		private final OutputStream outputStream;
		
		public Transporter(InputStream inputStream, OutputStream outputStream) {
			super();
			this.inputStream = inputStream;
			this.outputStream = outputStream;
		}
	

		@Override
		public void run() {
			
			byte[] readwrite = new byte[bufferSize]; 
		    try {
				while ((inputStream.read(readwrite)) != -1){
				  outputStream.write(readwrite);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try{
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
}