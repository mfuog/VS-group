package de.htw.ds.grid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import de.htw.ds.grid.Agent;
import de.sb.javase.TypeMetadata;


/**
 * <p>Agent returning an echo of the given input data.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class EchoAgent implements Agent {

	/**
	 * {@inheritDoc}
	 * @throws IOException if there is an I/O related problem
	 */
	public String process(final InputStream byteSource, final OutputStream byteSink, Map<String,String> properties) throws IOException {
		final byte[] buffer = new byte[0x100000];

		for (int bytesRead = byteSource.read(buffer); bytesRead != -1; bytesRead = byteSource.read(buffer)) {
			byteSink.write(buffer, 0, bytesRead);
		}

		return "bin";
	}
}