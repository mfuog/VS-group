package de.htw.ds.sync.myrtha;

public class ThreadDemoMine {

	/**
	 * neuen Thread erstellen und darin Text ausgeben - Anfängerfehler: Thread class extenden->nicht so gedacht
	 * @param args
	 */
	public static void main(final String[] args) {//final damit in run() verwendbar -> immer auf dasselbe Objekt verwei��t

		//anonyme/unbenannte innere Klasse:
		final Runnable runnable = new Runnable() {	//Instanz der anonymen Klasse wird in einem Zug erstellt

			@Override
			public void run() {
				System.out.println(args[0]);
			}
			
		};
		new Thread(runnable).start();	//nicht run() damit Ausf��hrung asynchron erfolgt
		
	}

}
