package de.htw.ds.sync;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
	public static void main(final String[] args) throws IOException {
		
		class Transporter implements Runnable {	//final? static	

			InputStream inputStream;
			OutputStream outputStream;
			
			//constructor
			Transporter (InputStream input, OutputStream output){
				inputStream = input;
				outputStream = output;
			}
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				byte[] buffer = new byte[1024]; // Adjust if you want
			    int bytesRead;
			    try {
					while ((bytesRead = inputStream.read(buffer)) != -1)
					{
					    outputStream.write(buffer, 0, bytesRead);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}	
			}

		}
		
		InputStream input = new FileInputStream("quelle.txt"); 	//ev. google
		OutputStream output = new FileOutputStream("ziel.txt"); //ev. google
		
		Transporter runnable = new Transporter(input, output);
		runnable.run();
		
		
		
		/*final Path sourcePath = Paths.get(args[0]);
		if (!Files.isReadable(sourcePath)) throw new IllegalArgumentException(sourcePath.toString());

		final Path sinkPath = Paths.get(args[1]);
		if (sinkPath.getParent() != null && !Files.isDirectory(sinkPath.getParent())) throw new IllegalArgumentException(sinkPath.toString());

		Files.copy(sourcePath, sinkPath, StandardCopyOption.REPLACE_EXISTING);
*/
		System.out.println("done.");
		
	}
}