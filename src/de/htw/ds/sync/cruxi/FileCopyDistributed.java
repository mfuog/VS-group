package de.htw.ds.sync;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;


import de.htw.ds.TypeMetadata;


/**
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class FileCopyDistributed {

	/**
	 * Copies a file. The first argument is expected to be a qualified source file name,
	 * the second a qualified target file name. 
	 * @param args the VM arguments
	 * @throws IOException if there's an I/O related problem
	 */
	public static void main(final String[] args) throws IOException {

		
		
		class Transporter implements Runnable{


			//static ging nicht
			InputStream inputStream;
			OutputStream outputStream;

			Transporter(InputStream source, OutputStream target){
				inputStream = source;
				outputStream = target;
			}

			@Override
			public void run() {

				byte[] buffer = new byte[ 0xFFFF ]; 
				try {
					int read;
					//InputStream method read: Reads some number of bytes from the input stream and stores them into the buffer array b
					//returns: the total number of bytes read into the buffer, or -1 is there is no more data because the end of the stream has been reached
					while ((read = inputStream.read(buffer)) != -1) {
						//OutputStream method write: Writes read bytes from the specified byte array starting at offset 0 to this output stream
						outputStream.write(buffer, 0, read);  
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				finally
		        {
		            try
		            {
		                outputStream.close();
		            }
		            catch (Exception e)
		            {
		                e.printStackTrace();
		            }
		        }
			}
		}

//		InputStream myInput = new FileInputStream("test.txt");
//		OutputStream myOutput = new FileOutputStream("testcopy.txt");

		//AUFGABE 1
//		Transporter t = new Transporter(myInput, myOutput);
//		t.run();
		
		InputStream myInput = new FileInputStream(args[0]);
		OutputStream myOutput = new FileOutputStream(args[1]);
		
		PipedOutputStream pipeOutputStream = new PipedOutputStream(); 
		PipedInputStream  pipeInputStream = new PipedInputStream();
		
		//realizes connection between input and output stream
		pipeOutputStream.connect(pipeInputStream); 
		
		//alternativ
		//PipedOutputStream pipeOutputStream = new PipedOutputStream(pipeInputStream);

		
		Transporter t1 = new Transporter(myInput, pipeOutputStream);
		new Thread(t1).start();

		Transporter t2= new Transporter(pipeInputStream, myOutput);
		new Thread(t2).start();
		
	}
}