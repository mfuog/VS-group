package de.htw.ds.sync.marian;

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

	/**
	 * Copies a file. The first argument is expected to be a qualified source file name,
	 * the second a qualified target file name. 
	 * @param args the VM arguments
	 * @throws IOException if there's an I/O related problem
	 */
	public static void main(final String[] args) {
		final Path sourcePath = Paths.get(args[0]);
		if (!Files.isReadable(sourcePath)) throw new IllegalArgumentException(sourcePath.toString());

		final Path sinkPath = Paths.get(args[1]);
		if (sinkPath.getParent() != null && !Files.isDirectory(sinkPath.getParent())) throw new IllegalArgumentException(sinkPath.toString());
		
		try {
			PipedOutputStream pos = new PipedOutputStream();
			
			(new Thread(new Transponder(new FileInputStream(sourcePath.toFile()),pos))).start();
			(new Thread(new Transponder(new PipedInputStream(pos), new FileOutputStream(sinkPath.toFile())))).start();
			
		} catch (IOException e) {
			System.out.println("Could not open InputStream to: "+sourcePath.toString());
			e.printStackTrace();
		}
		
		System.out.println("done.");
	}
		
	static public class Transponder implements Runnable {
		private static final int BUFFER_SIZE = 512;
		private InputStream inputStream;
		private OutputStream outputStream;

		public Transponder (InputStream inStream, OutputStream outStream){
			this.inputStream = inStream;
			this.outputStream = outStream;
		}
		
		@Override
		public void run(){
			if(inputStream !=null && outputStream !=null )
				transportData(inputStream, outputStream, new byte[BUFFER_SIZE]);
			else 
				throw new TransponderException("In- and OutputStream cannot be null or wrong assinged for FileCopy!");
		}
	
		private void transportData(InputStream inputStream,
				OutputStream outputStream, byte[] buffer) {
			try {
				while(inputStream.available()!=0 && inputStream.read(buffer)!=-1){
					outputStream.write(buffer);
				}
			} catch (IOException e) {
				throw new TransponderException("Problem during reding/writing",e);
			}
		}

		public class TransponderException extends RuntimeException{
			private static final long serialVersionUID = 7181560788254287169L;
			public TransponderException(String message, Exception e) {
				super(message,e);
			}
			public TransponderException(String message) {
				super(message);
			}
		}
		
	}
	
}