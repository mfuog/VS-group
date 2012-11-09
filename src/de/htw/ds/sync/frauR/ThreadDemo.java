package de.htw.ds.sync.frau_r;

public class ThreadDemo {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		
		//private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors()..
		
		
		
		// annonyme innere Methode
		// Interface hat keinen Konstruktor
		// Subclassing -> implement runnable -> zuweisung
		// alles final was geht, nur dann kann ich auf args referenzieren 
		// -> bekommt der varable $args
		final Runnable runnable = new Runnable(){

			@Override
			public void run() {
				System.out.println(args[0]);
				
			}
			
		};
		new Thread(runnable).start(); // NICHT run() -> sonst wird es parallel statt asyncron ausgefŸhrt
		

	}

}
