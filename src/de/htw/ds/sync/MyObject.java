package de.htw.ds.sync.moritz;

public class MyObject
{
	final Object object;

	public MyObject()
	{
		super();
		this.object = new Object();
	}
	
	private void changeObject()
	{
	 this.object = new Object();	
	}
	
}
