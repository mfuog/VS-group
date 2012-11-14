package de.htw.ds.sync.myrtha;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import de.htw.ds.TypeMetadata;


/**
 * <p>Demonstrates copying a file by using a two threads.</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class FileCopyDistributedMine {

	/**
	 * Transporter erweitert Runnable
	 */
	static class Transporter implements Runnable {	// static, damit Transporter angelegt werden kann ohne FileCopyDistributed zu instanziieren.	

		InputStream inputStream;
		OutputStream outputStream;
		
		//constructor
		Transporter (InputStream input, OutputStream output){
			inputStream = input;
			outputStream = output;
		}
		
		@Override
		/*
		 * Transferiert den Inhalt des Quell-Streams byteweise in den Ziel-Stream.(non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			
			byte[] buffer = new byte[ 1024 ]; // Adjust if you want
		    int bytesRead;
		    try {
		    	//byteweise vom Inputstream lesen und in den Outputstream schreiben
		    	//immer bei io darauf achten, dass nicht alle bufferinhalte gelesen werden sondern nur die die gefüllt waren
				while ((bytesRead = inputStream.read(buffer)) != -1){
				    outputStream.write(buffer, 0, bytesRead);	//daher immer methode mit 3 parameter verwenden 
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try { outputStream.close();}	//muss wieder geschlossen werden.. siehe Transporter in util
				catch (IOException e) { e.printStackTrace();} 
			}//finally END
		}//run() END
		
	}//Transporter class END
	
	/**
	 * Copies a file. The first argument is expected to be a qualified source file name,
	 * the second a qualified target file name.
	 * @param args the arguments (!=VM arguments! http://www.avajava.com/tutorials/lessons/whats-the-difference-between-program-arguments-and-vm-arguments.html)
	 * @throws IOException if there's an I/O related problem
	 */
	public static void main(final String[] args) throws IOException {

		//Streams der Text-Dateien
		InputStream input = new FileInputStream(args[0]); 	//ev. google
		OutputStream output = new FileOutputStream(args[1]); //ev. google
		
		//--Variante 1: mit Runnable
		//Transporter runnable = new Transporter(input, output);
		//runnable.run();
		
		//--Variante 2: mit 2 Threads
		//  vgl. http://openbook.galileocomputing.de/javainsel9/javainsel_17_007.htm
		//PipeStreams verbinden => eine Pipe
		PipedOutputStream pipedOutput = new PipedOutputStream();
		PipedInputStream  pipedInput = new PipedInputStream();
		pipedOutput.connect( pipedInput );
		
		//Threads verbinden (siehe Grafik pdf)
		Thread thread1  = new Thread(new Transporter(input, pipedOutput));
		Thread thread2  = new Thread(new Transporter(pipedInput, output));
		
		thread1.start();
		thread2.start();
	}
}