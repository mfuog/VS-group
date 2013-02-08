package de.htw.ds.grid;

import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import javax.jws.Oneway;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.ws.WebServiceException;
import de.sb.javase.TypeMetadata;
import de.sb.javase.xml.PathStringAdapter;


/**
 * <p>Service interface for a JAX-WS based P2P grid container.</p>
 */
@WebService
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.1.0", authors="Sascha Baumeister")
public interface GridService {

	/**
	 * Returns the receivers local service URI.
	 * @return the service URI
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	URI getLocalServiceURI();


	/**
	 * Returns the local output path.
	 * @return the local output path
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	@XmlJavaTypeAdapter(PathStringAdapter.class)
	Path getLocalOutputPath();


	/**
	 * Returns <tt>true</tt> if this grid service has an active task with
	 * the given task identity in it's task collection. Tasks are removed
	 * once their processing result has been stored, or if their processing
	 * times out after their last retry. Note that the existence of a task
	 * should be used to resynchronize clients.
	 * @param taskIdentity the task identity
	 * @return whether or not this grid service has an active task with
	 *    the given identity
	 * @see #createTask(String, Path, Path, Map, int, int)
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	boolean hasActiveTask(
		@WebParam(name="taskIdentity") long taskIdentity
	);


	/**
	 * Creates a task using the specified information, and returns it's identity.
	 * Additionally, the new task is added to the collection of active tasks, and
	 * scheduled for possibly repeated execution. If the given <tt>tryCount</tt>
	 * or </tt>timeToLive</tt> is negative, the method silently substitutes it 
	 * with the allowable minimum value of <tt>1</tt>.
	 * @param agentClassName the fully qualified class name of the agent
	 * @param jarPath the optional local path to the jar-file containing agent
	 *    code, or <tt>null</tt> for none
	 * @param dataPath the optional local path to the data file to be consumed by
	 *    the agent, or <tt>null</tt> for none
	 * @param properties the optional properties steering the processing, or
	 *    <tt>null</tt> for none
	 * @param tryCount the maximum number of times a grid container will try
	 *    to issue requests or responses for the new task
	 * @param timeToLive the maximum number of seconds a task computation may require
	 * @return the identity assigned to the new task
	 * @throws NullPointerException if the given agent class name is <tt>null</tt>
	 * @throws WebServiceException if there's a JAX-WS related problem
	 * @throws NoSuchFileException if any of the given paths is not <tt>null</tt>,
	 *     but doesn't point to a regular file
	 * @throws AccessDeniedException if any of the given paths is not <tt>null</tt>,
	 *    but the file cannot be read
	 * @see #hasActiveTask(long)
	 */
	long createTask(
		@WebParam(name="agentClassName") String agentClassName,
		@WebParam(name="jarPath") @XmlJavaTypeAdapter(PathStringAdapter.class) Path jarPath,
		@WebParam(name="dataPath") @XmlJavaTypeAdapter(PathStringAdapter.class) Path dataPath,
		@WebParam(name="properties") @XmlJavaTypeAdapter(StringMapAdapter.class) Map<String,String> properties,
		@WebParam(name="tryCount") int tryCount,
		@WebParam(name="timeToLive") int timeToLive
	) throws NoSuchFileException, AccessDeniedException;


	/**
	 * Processes the given task. If the given task's jarPath is <tt>null</tt>,
	 * the task is simply run synchronously. Otherwise, the task is executed
	 * asynchronously under a temporarily extended class path. This procedure
	 * allows tasks to be processed whose code base is not part of the
	 * container's normal class path. Note that this service method is designed
	 * to be one-way, therefore callers can not expect any processing results,
	 * including exceptions!
	 * @param task the grid task to be processed
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	@Oneway
	void processTask(
		@WebParam(name="task") GridTask task
	);


	/**
	 * Processes the given task response response by checking if the given task
	 * still exists, storing the response's data content in a file named after
	 * the gridResponse's output path within the grid container's output directory.
	 * Finally it removes the task from the container's task collection. Note that
	 * the method is one-way, therefore no processing results whatsoever can be
	 * returned, including exceptions!
	 * @param taskResponse the grid task response
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	@Oneway
	void processTaskResponse(
		@WebParam(name="taskResponse") GridTaskResponse taskResponse
	);
}