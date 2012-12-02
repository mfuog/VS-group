package de.htw.ds.sync;

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
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister Myrtha Fuog")
public final class FileCopyDistributedMine {

	/**
	 * Transporter erweitert Runnable
	 */
	static class Transporter implements Runnable {	// static, damit Transporter angelegt werden kann ohne FileCopyDistributedMine zu instanziieren.	

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
			
			byte[] buffer = new byte[ 1024 ]; //buffer in dem die vom InputStream gelesene Information zwischengespeichert und vom OutputStream abgeholt wird
		    int bytesRead;
		    try {
		    	//byteweise vom Inputstream lesen und in den Outputstream schreiben
		    	//vgl. http://docs.oracle.com/javase/1.4.2/docs/api/java/io/OutputStream.html
				while ((bytesRead = inputStream.read(buffer)) != -1){	//-1 = Stream am Ende
			    	//immer bei io darauf achten, dass nicht ALLE Bufferinhalte gelesen werden sondern nur die die auch gefüllt waren:
				    outputStream.write(buffer, 0, bytesRead);	//->daher immer write-Methode mit 3 parameter verwenden 
				    //vgl .http://docs.oracle.com/javase/1.4.2/docs/api/java/io/InputStream.html
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try 					{ outputStream.close();}	//muss wieder geschlossen werden.. siehe Transporter in util
				catch (IOException e) 	{ e.printStackTrace();} 
			}//finally END
		    System.out.println("run()");
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
		PipedOutputStream pipedSink = new PipedOutputStream();	//schreibt in die Pipe
		PipedInputStream  pipedSource = new PipedInputStream();	//liest aus der Pipe
		pipedSink.connect( pipedSource );
		
		//Threads verbinden (siehe Grafik pdf)
		Thread thread1  = new Thread(new Transporter(input, pipedSink));//das an Thread übergebene Runnable-Objekt bestimmt die run()-MEthode die bei Thread.start() verwendet wird!
		Thread thread2  = new Thread(new Transporter(pipedSource, output));
		
		thread1.start();
		thread2.start();
	}
}