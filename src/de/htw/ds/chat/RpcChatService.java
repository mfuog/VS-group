package de.htw.ds.chat;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.WebServiceException;
import de.sb.javase.TypeMetadata;


/**
 * <p>Chat service interface for dynamic JAX-WS.</p>
 */
@WebService
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public interface RpcChatService {

	/**
	 * Adds a chat entry.
	 * @param alias the user alias
	 * @param content the content
	 * @return the creation timestamp
	 * @throws NullPointerException if any of the given arguments is null
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	long addEntry(
		@WebParam(name="alias") String alias,
		@WebParam(name="content") String content
	) throws NullPointerException;


	/**
	 * Removes a chat entry.
	 * @param entry the chat entry
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	boolean removeEntry(
		@WebParam(name="entry") ChatEntry entry
	);


	/**
	 * Returns the chat entries.
	 * @return the chat entries
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	ChatEntry[] getEntries();
}