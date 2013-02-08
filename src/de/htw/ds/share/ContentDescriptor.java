package de.htw.ds.share;

import java.math.BigInteger;
import de.sb.javase.TypeMetadata;


/**
 * <p>This interface enforces the ability of all content describing
 * classes to provide a content hash and a content length.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public interface ContentDescriptor {

	/**
	 * Returns the content hash-code.
	 * @return the content hash-code
	 */
	BigInteger getContentHash();


	/**
	 * Returns the content length.
	 * @return the content length
	 */
	long getContentLength();
}