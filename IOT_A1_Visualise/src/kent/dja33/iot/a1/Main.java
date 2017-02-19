package kent.dja33.iot.a1;

import kent.dja33.iot.a1.util.Out;

/**
 * Starting point for the application to run, contains 
 * static entrance for the GUI component that can be accessed from 
 * anywhere.
 * @author Dante
 *
 */
public class Main {
	
	public static final SensorDisplay display = new SensorDisplay();
	
	public static void main(String[] args){
		
		//System.out.print("hello");
		
		Out.out.logln("Starting application...");	
		
		display.launch();
		
	}

}
