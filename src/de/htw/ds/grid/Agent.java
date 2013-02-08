package de.htw.ds.grid;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import de.sb.javase.TypeMetadata;


/**
 * <p>Common service interface for agents.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public interface Agent {
	/**
	 * Service callback processing the input data and generating output
	 * data. It returns a file type suitable for the output data generated.
	 * @param byteSource the byte source
	 * @param byteSink the byte sink
	 * @param properties the optional properties steering the processing
	 * @return a file extension suitable for the generated data
	 * @throws NullPointerException if any of the arguments is <tt>null</tt>
	 * @throws Exception if a problem occurs
	 */
	String process(InputStream byteSource, OutputStream byteSink, Map<String,String> properties) throws Exception;
}