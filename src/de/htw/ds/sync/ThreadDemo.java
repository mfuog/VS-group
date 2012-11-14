package de.htw.ds.sync;

public class ThreadDemo {

	/**
	 * @param args
	 */
	
	//args final machen, damit man dies in dem runnable verweden kann
	public static void main(final String[] args) {
		// TODO Auto-generated method stub

		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				System.out.println(args[0]); //es kann nur auf args referenziert werden, da es final ist
				
			}
			
		};
		
		//start --> Ausführung erfolgt assynchron
		//bei run() wird kein neuer Thread gestartet!
		new Thread(runnable).start();
	}

}
