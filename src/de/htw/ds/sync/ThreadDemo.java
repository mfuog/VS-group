<<<<<<< HEAD
package de.htw.ds.sync;
=======
package de.htw.ds.sync.myrtha;
>>>>>>> e8571f9152b9780ec464505e18d7fb7142f2743a

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class ThreadDemo {
	private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
	private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_COUNT);

	public static void main(final String[] args) {
		final Runnable runnable = new Runnable() {
			public void run() {
				System.out.println(args[0]);		
			}
		};
		new Thread(runnable).start(); // NICHT run()!!!
		THREAD_POOL.submit(runnable);
	}
}